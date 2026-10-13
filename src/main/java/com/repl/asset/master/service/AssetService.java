package com.repl.asset.master.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.login.model.AmsLoginAccess;
import com.repl.asset.login.repository.AmsLoginAccessRepository;
import com.repl.asset.master.bo.AssetBO;
import com.repl.asset.master.bo.AssetChangeLogBO;
import com.repl.asset.master.model.AssetChangeLog;
import com.repl.asset.master.model.AssetMaster;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.DeviceNameMaster;
import com.repl.asset.master.repository.AssetChangeLogRepository;
import com.repl.asset.master.repository.AssetMasterRepository;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * The asset register — every machine, of every device type, over ams_add_device_master.
 *
 * ONE SERVICE FOR EVERY DEVICE. There is no switch here and no list of supported types,
 * because there is nothing to switch on: a machine carries a device_id, and any id the device
 * master holds is valid. A device the admin adds in Device Master can be recorded from the
 * user side straight away, with nothing to change here first.
 *
 * That is the fix for what the per-device-table version got wrong. There, each type needed
 * its own table, entity and service before the user side would even offer it, so a device
 * added in the master silently never appeared.
 */
@Service
public class AssetService {

	@Autowired
	private AssetMasterRepository assetMasterRepository;

	/** Read-only here: looked up to check the device exists and to name it on the way out. */
	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	/** Open / Assigned on Device Name Master, from who actually holds each machine. */
	@Autowired
	private AllocationService allocationService;

	/** The reference number — the same one Device Name Master gave the name. */
	@Autowired
	private ReferenceNumberService referenceNumberService;

	/** A USER's Reporting Person row — the device types and plant they may save. */
	@Autowired
	private AmsLoginAccessRepository amsLoginAccessRepository;

	/** What each edit changed — see logChanges. */
	@Autowired
	private AssetChangeLogRepository changeLogRepository;

	/** Added through ADD DEVICE. */
	private static final String SOURCE_FORM = "FORM";

	/**
	 * The machines of one device type, active AND inactive, newest first.
	 *
	 * ONLY THE ONES ADDED THROUGH ADD DEVICE by default, which is what LIST DEVICE wants. The
	 * 656 rows carried over from the workbook are names and almost nothing else, so listing
	 * them alongside real entries buries what was actually filled in.
	 *
	 * includeImported brings the workbook rows back, and is what ADD DEVICE asks for — its
	 * Model and Device Name dropdowns are built from the whole catalogue, and its duplicate
	 * check has to see every name, not just the ones added here.
	 *
	 * Newest first rather than alphabetically, unlike the masters: this is a register being
	 * added to, so the row just saved belongs at the top where it can be seen.
	 */
	@Transactional(readOnly = true)
	public List<AssetBO> list(Long deviceId, boolean includeImported) throws ApplicationException {
		DeviceMaster device = requireDevice(deviceId);

		return assetMasterRepository.findByDeviceId(device.getId()).stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.filter(row -> includeImported || SOURCE_FORM.equalsIgnoreCase(row.getSource()))
				.sorted(Comparator.comparing(AssetMaster::getId).reversed())
				.map(row -> {
					AssetBO bo = toBO(row);
					bo.setDevice_name(device.getDeviceName());
					return bo;
				})
				.toList();
	}

	@Transactional
	public AssetBO save(AssetBO in) throws ApplicationException {
		DeviceMaster device = requireDevice(in == null ? null : in.getDevice_id());
		String name = requireName(in);
		BigDecimal years = requireYears(in);
		checkScope(in, device, null);

		AssetMaster existing = findIncludingDeleted(device.getId(), name);

		if (existing != null) {
			/*
			 * Three ways a name can already be taken, and only one of them is an error.
			 *
			 * DELETED — uk_device_asset is UNIQUE on (device_id, name), so a soft-deleted row
			 * still occupies that pair and a second insert is impossible. Re-adding revives
			 * it; refusing would be wrong, since the screen shows nothing there.
			 *
			 * IMPORTED — the row came from the workbook and is a bare name. Adding it here is
			 * how it gets filled in for the first time, so the row is CLAIMED: the form's
			 * values go on, it becomes a FORM row, and it appears in LIST DEVICE. Refusing
			 * would strand it — it is not in the list, so there would be nowhere to edit it
			 * from either.
			 *
			 * ALREADY ADDED — a real entry someone made. This is the error, and ADD DEVICE
			 * does not quietly update it, because that would change a record the user never
			 * chose to open.
			 */
			boolean deleted = existing.getStatus() == Constants.STATUS_DELETED;
			boolean imported = !SOURCE_FORM.equalsIgnoreCase(existing.getSource());

			if (!deleted && !imported) {
				// Says where it is: the Reference No it already has, and the lists it is in.
				String ref = existing.getReferenceNo();
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + name + "\" is already added under " + device.getDeviceName()
								+ (ref != null && !ref.isBlank() ? " (Reference No " + ref + ")" : "")
								+ ". Open it from Open / Assigned Devices to change it.");
			}
			apply(existing, in, device, name, years);
			stamp(existing);
			existing.setAddedVia(addedVia(in));
			assignReference(existing, device);
			AssetMaster savedExisting = assetMasterRepository.save(existing);
			// Open until an employee is assigned (a printer: Assigned now) — see AllocationService.
			allocationService.refresh(savedExisting);
			return withDeviceName(savedExisting, device);
		}

		AssetMaster row = new AssetMaster();
		apply(row, in, device, name, years);
		stamp(row);
		row.setAddedVia(addedVia(in));
		assignReference(row, device);

		AssetMaster saved = assetMasterRepository.save(row);
		// Open until an employee is assigned (a printer: Assigned now) — see AllocationService.
		allocationService.refresh(saved);
		return withDeviceName(saved, device);
	}

	@Transactional
	public AssetBO update(AssetBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		DeviceMaster device = requireDevice(in.getDevice_id());
		String name = requireName(in);
		BigDecimal years = requireYears(in);
		AssetMaster row = assetMasterRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That machine no longer exists."));
		// The device's CURRENT plant is checked too: a USER cannot pull a device out of another plant.
		checkScope(in, device, row.getPlant());

		// Ignores the row being edited, so re-saving it unchanged is allowed.
		AssetMaster holder = findIncludingDeleted(device.getId(), name);
		if (holder != null && !holder.getId().equals(row.getId())) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					holder.getStatus() == Constants.STATUS_DELETED
							? "\"" + name + "\" was previously used under "
									+ device.getDeviceName() + " and deleted. Add it again from "
									+ "ADD DEVICE rather than renaming this row onto it."
							: "\"" + name + "\" is already recorded under "
									+ device.getDeviceName() + ".");
		}

		// Every field as it stood before this edit, to log what the edit changes.
		Map<String, String> before = snapshot(row);

		// Renamed: the old name is free again in the catalogue.
		String oldName = row.getName();
		Long oldDeviceId = row.getDeviceId();
		if (oldName != null && (!oldName.equalsIgnoreCase(name) || !device.getId().equals(oldDeviceId))) {
			allocationService.set(oldDeviceId, oldName, DeviceNameMaster.ALLOCATION_OPEN);
		}

		apply(row, in, device, name, years);
		// A machine saved before reference numbers existed gets one the first time it is edited.
		assignReference(row, device);
		AssetMaster saved = assetMasterRepository.save(row);
		// From who holds it now, not from being saved — see AllocationService.
		allocationService.refresh(saved);
		logChanges(saved.getId(), before, snapshot(saved), in);
		return withDeviceName(saved, device);
	}

	/**
	 * THE CHANGE LOG — one row per field this edit changed, with its value before and after,
	 * who saved it and when. A field left as it was writes nothing, so re-saving a device
	 * unchanged leaves the log untouched.
	 */
	private void logChanges(Long assetId, Map<String, String> before, Map<String, String> after,
			AssetBO in) {
		LocalDate today = LocalDate.now();
		LocalTime now = LocalTime.now().withNano(0);
		after.forEach((field, newValue) -> {
			String oldValue = before.get(field);
			if (Objects.equals(oldValue, newValue)) {
				return;
			}
			AssetChangeLog log = new AssetChangeLog();
			log.setAssetId(assetId);
			log.setFieldName(field);
			/*
			 * Stored exactly as the USER LOG shows it while editing: an empty "before" is never
			 * blank — an External field shows the original part beside it ("4 GB (RAM)"), any
			 * other field "Not set"; an emptied "after" reads "Removed".
			 */
			if (oldValue == null) {
				String part = field.startsWith("External ") ? field.substring("External ".length()) : null;
				String base = part != null ? before.get(part) : null;
				oldValue = base != null ? base + " (" + part + ")" : "Not set";
			}
			log.setOldValue(oldValue);
			log.setNewValue(newValue == null ? "Removed" : newValue);
			log.setChangedByCode(trimToNull(in.getChanged_by_code()));
			log.setChangedByName(trimToNull(in.getChanged_by_name()));
			log.setChangeDate(today);
			log.setChangeTime(now);
			changeLogRepository.save(log);
		});
	}

	/**
	 * Every logged field of a machine, as the screen labels and shows it, in form order. Blank
	 * is null, so "" and a missing value never count as a change.
	 */
	private Map<String, String> snapshot(AssetMaster r) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Device Name", trimToNull(r.getName()));
		m.put("Reference No", trimToNull(r.getReferenceNo()));
		m.put("Model", trimToNull(r.getModel()));
		m.put("Asset Code", trimToNull(r.getAssetCode()));
		m.put("Asset S/N", trimToNull(r.getAssetSn()));
		m.put("Service Tag", trimToNull(r.getServiceTag()));
		m.put("Exp. Service Code", trimToNull(r.getExpServiceCode()));
		m.put("PO No.", trimToNull(r.getPoNo()));
		m.put("Processor", trimToNull(r.getProcessor()));
		m.put("External Processor", trimToNull(r.getExternalProcessor()));
		m.put("RAM", trimToNull(r.getRam()));
		m.put("External RAM", trimToNull(r.getExternalRam()));
		m.put("ROM", trimToNull(r.getRom()));
		m.put("External ROM", trimToNull(r.getExternalRom()));
		m.put("HDD", trimToNull(r.getHdd()));
		m.put("External HDD", trimToNull(r.getExternalHdd()));
		m.put("System Type", trimToNull(r.getSystemType()));
		m.put("Operating System", trimToNull(r.getOs()));
		m.put("OS Service Pack", trimToNull(r.getOsServicePack()));
		m.put("MS Office", trimToNull(r.getMsOffice()));
		m.put("Software 1", trimToNull(r.getSoftware1()));
		m.put("Software 2", trimToNull(r.getSoftware2()));
		m.put("Charger", trimToNull(r.getCharger()));
		m.put("Keyboard", trimToNull(r.getKeyboard()));
		m.put("Mouse", trimToNull(r.getMouse()));
		m.put("Plant", trimToNull(r.getPlant()));
		m.put("Department", trimToNull(r.getDepartment()));
		m.put("Location", trimToNull(r.getLocation()));
		m.put("IP Address", trimToNull(r.getIpAddress()));
		m.put("Device Status", trimToNull(r.getDeviceStatus()));
		m.put("Employee", trimToNull(joinHolder(r.getEmployeeCode(), r.getEmployeeName())));
		m.put("Warranty Status", trimToNull(r.getWarrantyStatus()));
		m.put("Warranty Start Date", r.getWarrantyStartDate() == null ? null
				: r.getWarrantyStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		m.put("Warranty End Date", r.getLastDateOfWarranty() == null ? null
				: r.getLastDateOfWarranty().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		m.put("Total Warranty (In Years)", r.getTotalWarrantyYears() == null ? null
				: r.getTotalWarrantyYears().stripTrailingZeros().toPlainString());
		m.put("Classification", trimToNull(r.getClassification()));
		m.put("Description", trimToNull(r.getDescription()));
		m.put("Status", r.getStatus() == Constants.STATUS_ACTIVE ? "Active" : "Inactive");
		return m;
	}

	/** "101890 - DEVANAND BORSE", or whichever half is there. */
	private static String joinHolder(String code, String name) {
		String c = code == null ? "" : code.trim();
		String n = name == null ? "" : name.trim();
		return c.isEmpty() ? n : n.isEmpty() ? c : c + " - " + n;
	}

	/** One machine's change log, newest first — the CHANGE HISTORY table on its page. */
	@Transactional(readOnly = true)
	public List<AssetChangeLogBO> changeLog(Long assetId) throws ApplicationException {
		if (assetId == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device is required.");
		}
		return changeLogRepository.findByAssetIdOrderByIdDesc(assetId).stream().map(c -> {
			AssetChangeLogBO bo = new AssetChangeLogBO();
			bo.setId(c.getId());
			bo.setAsset_id(c.getAssetId());
			bo.setField_name(c.getFieldName());
			bo.setOld_value(c.getOldValue());
			bo.setNew_value(c.getNewValue());
			bo.setChanged_by_code(c.getChangedByCode());
			bo.setChanged_by_name(c.getChangedByName());
			bo.setChange_date(c.getChangeDate());
			bo.setChange_time(c.getChangeTime());
			return bo;
		}).toList();
	}

	/**
	 * How a NEW device is being added: "BULK" only when BULK UPLOAD says so (it sets it on every
	 * row), otherwise "FORM". Only on save — an edit never changes how a device was added.
	 */
	private static String addedVia(AssetBO in) {
		return in != null && "BULK".equalsIgnoreCase(in.getAdded_via()) ? "BULK" : "FORM";
	}

	/**
	 * A USER's scope, from their Reporting Person row: they may save only the device types and
	 * the plant set for them there. ADMIN and VIEWER are not limited. Each part is enforced only
	 * when it is set, so a USER given no device types or no plant is not locked out entirely.
	 *
	 * The saver is the changed_by_code the form sends with every save and update.
	 */
	private void checkScope(AssetBO in, DeviceMaster device, String currentPlant) throws ApplicationException {
		String code = in == null ? null : trimToNull(in.getChanged_by_code());
		if (code == null || !code.matches("\\d+")) {
			return;
		}
		AmsLoginAccess access = amsLoginAccessRepository
				.findByEmpCodeAndStatus(Long.valueOf(code), Constants.STATUS_ACTIVE)
				.orElse(null);
		if (access == null || !"USER".equalsIgnoreCase(access.getRole())) {
			return;
		}
		List<Long> allowed = access.getDeviceIdList();
		if (!allowed.isEmpty() && !allowed.contains(device.getId())) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"You do not have access to " + device.getDeviceName() + " devices.");
		}
		// One plant or several: the device's plant has to be one of them.
		List<String> plants = access.getPlantCodeList();
		String devicePlant = String.valueOf(trimToNull(in.getPlant()));
		if (!plants.isEmpty() && plants.stream().noneMatch(p -> p.equalsIgnoreCase(devicePlant))) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"You can add or change devices only for plant " + String.join(", ", plants) + ".");
		}
		// On an update: the plant the device is in NOW must be one of theirs as well.
		String nowIn = trimToNull(currentPlant);
		if (nowIn != null && !plants.isEmpty() && plants.stream().noneMatch(p -> p.equalsIgnoreCase(nowIn))) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"This device is in plant " + nowIn + ". You can change devices only in plant "
							+ String.join(", ", plants) + ".");
		}
	}

	/**
	 * Give the machine its reference number, once — the number its name was already given in
	 * Device Name Master, so the Open Devices list, this form and the register all agree. See
	 * ReferenceNumberService for the format (DE26HR01007) and how it is made.
	 */
	private void assignReference(AssetMaster row, DeviceMaster device) {
		referenceNumberService.assign(row, device);
	}

	/**
	 * The reference number ADD DEVICE shows once a type and name are chosen: the one the name
	 * was given in Device Name Master, or for a name not in it, a preview of the next number.
	 */
	@Transactional(readOnly = true)
	public String previewReference(Long deviceId, String name) throws ApplicationException {
		DeviceMaster device = requireDevice(deviceId);
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device name is required.");
		}
		return referenceNumberService.preview(device, trimmed);
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		AssetMaster row = assetMasterRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That machine no longer exists."));

		// Retired, not removed: anything already recording this machine still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		assetMasterRepository.save(row);
		// No longer in the register, so its name is Open again in the catalogue.
		allocationService.set(row.getDeviceId(), row.getName(), DeviceNameMaster.ALLOCATION_OPEN);
	}

	/**
	 * Copy the form's values onto the row.
	 *
	 * Every field but the device and name is written unconditionally, including when it
	 * arrives blank — this form shows all of them at once, so an empty box genuinely means
	 * "clear it". That is the opposite of the admin masters, where a null means "not on this
	 * form"; the difference is that here there is one form and it carries the whole row.
	 */
	private void apply(AssetMaster row, AssetBO in, DeviceMaster device, String name,
			BigDecimal years) {
		row.setDeviceId(device.getId());
		row.setName(name);

		row.setPoNo(trimToNull(in.getPo_no()));
		row.setAssetCode(trimToNull(in.getAsset_code()));
		row.setAssetSn(trimToNull(in.getAsset_sn()));
		row.setDeviceStatus(trimToNull(in.getDevice_status()));
		row.setIpAddress(trimToNull(in.getIp_address()));
		row.setPlant(trimToNull(in.getPlant()));
		row.setDepartment(trimToNull(in.getDepartment()));
		row.setLocation(trimToNull(in.getLocation()));
		row.setModel(trimToNull(in.getModel()));
		row.setServiceTag(trimToNull(in.getService_tag()));
		row.setExpServiceCode(trimToNull(in.getExp_service_code()));
		row.setEmployeeCode(trimToNull(in.getEmployee_code()));
		row.setEmployeeName(trimToNull(in.getEmployee_name()));
		row.setProcessor(trimToNull(in.getProcessor()));
		row.setRam(trimToNull(in.getRam()));
		row.setRom(trimToNull(in.getRom()));
		row.setHdd(trimToNull(in.getHdd()));
		row.setExternalProcessor(trimToNull(in.getExternal_processor()));
		row.setExternalRam(trimToNull(in.getExternal_ram()));
		row.setExternalRom(trimToNull(in.getExternal_rom()));
		row.setExternalHdd(trimToNull(in.getExternal_hdd()));
		row.setOs(trimToNull(in.getOs()));
		row.setOsServicePack(trimToNull(in.getOs_service_pack()));
		row.setMsOffice(trimToNull(in.getMs_office()));
		row.setCharger(trimToNull(in.getCharger()));
		row.setKeyboard(trimToNull(in.getKeyboard()));
		row.setMouse(trimToNull(in.getMouse()));
		row.setSoftware1(trimToNull(in.getSoftware_1()));
		row.setSoftware2(trimToNull(in.getSoftware_2()));
		row.setClassification(trimToNull(in.getClassification()));
		row.setSystemType(trimToNull(in.getSystem_type()));
		row.setWarrantyStatus(trimToNull(in.getWarranty_status()));
		row.setDescription(trimToNull(in.getDescription()));

		row.setWarrantyStartDate(in.getWarranty_start_date());
		row.setLastDateOfWarranty(in.getLast_date_of_warranty());
		row.setTotalWarrantyYears(years);

		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	/**
	 * Stamped here, never taken from the request.
	 *
	 * Sets source too: anything that goes through save() arrived from ADD DEVICE, including a
	 * revived row and a claimed workbook one, so from here on it belongs in LIST DEVICE.
	 */
	private void stamp(AssetMaster row) {
		row.setSource(SOURCE_FORM);
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		/*
		 * reg_by is NOT set: it should be whoever saved, and the backend has no idea who that
		 * is — every endpoint is permitAll and no token is carried. It stays null until real
		 * authentication exists.
		 */
	}

	/**
	 * The chosen device, checked rather than trusted.
	 *
	 * Any id the device master holds is accepted — there is deliberately no list of allowed
	 * types. What is checked is that the device exists and has not been deleted, because the
	 * id arrives from a request and a stale page could send one that has since gone.
	 */
	private DeviceMaster requireDevice(Long deviceId) throws ApplicationException {
		if (deviceId == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device is required.");
		}
		DeviceMaster device = deviceMasterRepository.findById(deviceId)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That device no longer exists."));

		if (device.getStatus() == Constants.STATUS_DELETED) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + device.getDeviceName() + "\" has been deleted from the device master.");
		}
		return device;
	}

	/** The name column is VARCHAR(200); anything longer is refused rather than truncated. */
	private static final int NAME_MAX = 200;

	private String requireName(AssetBO in) throws ApplicationException {
		String name = in == null || in.getName() == null ? "" : in.getName().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device name is required.");
		}
		/*
		 * Checked here rather than left to the database. Without it the insert fails on the
		 * column width and surfaces as a bare "Server Error", which tells whoever typed it
		 * nothing about what to do differently.
		 */
		if (name.length() > NAME_MAX) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Device name is too long — " + name.length() + " characters, and the most "
							+ "allowed is " + NAME_MAX + ".");
		}
		return name;
	}

	/**
	 * Refused, not quietly dropped.
	 *
	 * A negative term is meaningless and would make the derived age nonsense. Nulling it
	 * silently would answer "success" while throwing away what was sent, so the caller would
	 * never learn the figure did not stick — the form already says the same thing, and the two
	 * should agree.
	 */
	private BigDecimal requireYears(AssetBO in) throws ApplicationException {
		BigDecimal years = in == null ? null : in.getTotal_warranty_years();
		if (years == null) {
			return null;
		}
		if (years.signum() < 0) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Total warranty cannot be negative.");
		}
		return years;
	}

	/**
	 * The row holding this device and name, deleted or not, or null.
	 *
	 * Case-insensitive on the name, so "it-101" and "IT-101" count once. The table's UNIQUE
	 * key is the real guarantee; this exists so the caller gets a readable message instead of
	 * a constraint violation surfacing as a 500.
	 */
	private AssetMaster findIncludingDeleted(Long deviceId, String name) {
		return assetMasterRepository.findByDeviceId(deviceId).stream()
				.filter(row -> name.equalsIgnoreCase(row.getName()))
				.findFirst()
				.orElse(null);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * How old the machine is, as the workbook's AGE column means it.
	 *
	 * NOT stored, worked out on every read. Age changes by itself as months pass, so a stored
	 * value would be wrong the day after it was written — which is exactly what the sheet
	 * suffers from.
	 *
	 * Bought is taken as (last date of warranty − term), since cover runs from purchase. Both
	 * are needed, so a row missing either has no age rather than a made-up one. The term may
	 * be a half year, so it is applied as months to keep 1.5 exact. A bought date in the
	 * future means the sheet's own figures disagree, and reads as blank rather than a
	 * negative age.
	 */
	private String deriveAge(AssetMaster row) {
		LocalDate last = row.getLastDateOfWarranty();
		BigDecimal years = row.getTotalWarrantyYears();
		if (last == null || years == null) {
			return null;
		}

		LocalDate bought = last.minusMonths(years.multiply(BigDecimal.valueOf(12)).longValue());
		LocalDate today = LocalDate.now();
		if (bought.isAfter(today)) {
			return null;
		}

		Period age = Period.between(bought, today);
		return age.getYears() == 0
				? age.getMonths() + " mo"
				: age.getYears() + " yr " + age.getMonths() + " mo";
	}

	private AssetBO withDeviceName(AssetMaster row, DeviceMaster device) {
		AssetBO bo = toBO(row);
		bo.setDevice_name(device.getDeviceName());
		return bo;
	}

	private AssetBO toBO(AssetMaster row) {
		AssetBO bo = new AssetBO();
		bo.setId(row.getId());
		bo.setDevice_id(row.getDeviceId());
		bo.setName(row.getName());

		bo.setPo_no(row.getPoNo());
		bo.setAsset_code(row.getAssetCode());
		bo.setReference_no(row.getReferenceNo());
		bo.setAsset_sn(row.getAssetSn());
		bo.setDevice_status(row.getDeviceStatus());
		bo.setIp_address(row.getIpAddress());
		bo.setPlant(row.getPlant());
		bo.setDepartment(row.getDepartment());
		bo.setLocation(row.getLocation());
		bo.setModel(row.getModel());
		bo.setService_tag(row.getServiceTag());
		bo.setExp_service_code(row.getExpServiceCode());
		bo.setEmployee_code(row.getEmployeeCode());
		bo.setEmployee_name(row.getEmployeeName());
		bo.setProcessor(row.getProcessor());
		bo.setRam(row.getRam());
		bo.setRom(row.getRom());
		bo.setHdd(row.getHdd());
		bo.setExternal_processor(row.getExternalProcessor());
		bo.setExternal_ram(row.getExternalRam());
		bo.setExternal_rom(row.getExternalRom());
		bo.setExternal_hdd(row.getExternalHdd());
		bo.setOs(row.getOs());
		bo.setOs_service_pack(row.getOsServicePack());
		bo.setMs_office(row.getMsOffice());
		bo.setCharger(row.getCharger());
		bo.setKeyboard(row.getKeyboard());
		bo.setMouse(row.getMouse());
		bo.setSoftware_1(row.getSoftware1());
		bo.setSoftware_2(row.getSoftware2());
		bo.setClassification(row.getClassification());
		bo.setSystem_type(row.getSystemType());
		bo.setWarranty_status(row.getWarrantyStatus());
		bo.setDescription(row.getDescription());

		bo.setWarranty_start_date(row.getWarrantyStartDate());
		bo.setLast_date_of_warranty(row.getLastDateOfWarranty());
		bo.setTotal_warranty_years(row.getTotalWarrantyYears());
		bo.setAge(deriveAge(row));

		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		// Out only, so ADD DEVICE can tell a name already added from one merely catalogued.
		bo.setSource(row.getSource());
		bo.setAdded_via(row.getAddedVia());

		// The newest action, summarised. Out only — AssetActionHistoryService owns these.
		bo.setLast_action_id(row.getLastActionId());
		bo.setLast_action_date(row.getLastActionDate());
		bo.setLast_action_remarks(row.getLastActionRemarks());
		bo.setLast_action_on(row.getLastActionOn());
		return bo;
	}
}
