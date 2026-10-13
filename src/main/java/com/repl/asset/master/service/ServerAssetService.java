package com.repl.asset.master.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.ServerAssetBO;
import com.repl.asset.master.model.ServerWithIpMaster;
import com.repl.asset.master.repository.ServerWithIpMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * The server register — every server in full, over ams_server_with_ip_master.
 *
 * The counterpart of AssetService, which does the same job for laptops, desktops and
 * printers. Separate rather than shared because a server's row is a different shape: it has
 * no device type, no model and no device name, and it carries columns the others do not.
 *
 * NOT ServerService, which is the thin admin master of server names on the Setting page.
 */
@Service
public class ServerAssetService {

	/** The name column is VARCHAR(100); anything longer is refused rather than truncated. */
	private static final int NAME_MAX = 100;

	@Autowired
	private ServerWithIpMasterRepository repository;

	/** What each UPDATE changed — the USER LOG (ams_server_user_log). */
	@Autowired
	private com.repl.asset.master.repository.ServerChangeLogRepository changeLogRepository;

	/**
	 * Every server, active AND inactive, newest first.
	 *
	 * Newest first rather than alphabetically: this is a register being added to, so the row
	 * just saved belongs at the top where it can be seen.
	 */
	@Transactional(readOnly = true)
	public List<ServerAssetBO> list() {
		return repository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(ServerWithIpMaster::getId).reversed())
				.map(this::toBO)
				.toList();
	}

	@Transactional
	public ServerAssetBO save(ServerAssetBO in) throws ApplicationException {
		String name = requireName(in);
		BigDecimal years = requireYears(in);

		ServerWithIpMaster existing = findIncludingDeleted(name);

		if (existing != null) {
			/*
			 * server_name is UNIQUE, so a soft-deleted row still occupies that name and a
			 * second insert is impossible. Re-adding a server that was deleted revives that
			 * row — refusing would be wrong, since the screen shows nothing there.
			 */
			if (existing.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + name + "\" is already added. Open it from List Server to change it.");
			}
			apply(existing, in, name, years);
			stamp(existing);
			existing.setAddedVia(addedVia(in));
			return toBO(repository.save(existing));
		}

		ServerWithIpMaster row = new ServerWithIpMaster();
		apply(row, in, name, years);
		stamp(row);
		row.setAddedVia(addedVia(in));

		return toBO(repository.save(row));
	}

	/**
	 * How a NEW server is being added: "BULK" only when BULK UPLOAD says so, otherwise "FORM".
	 * Only on save — an edit never changes how a server was added.
	 */
	private static String addedVia(ServerAssetBO in) {
		return in != null && "BULK".equalsIgnoreCase(in.getAdded_via()) ? "BULK" : "FORM";
	}

	@Transactional
	public ServerAssetBO update(ServerAssetBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		String name = requireName(in);
		BigDecimal years = requireYears(in);

		ServerWithIpMaster row = repository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That server no longer exists."));

		// Ignores the row being edited, so re-saving it unchanged is allowed.
		ServerWithIpMaster holder = findIncludingDeleted(name);
		if (holder != null && !holder.getId().equals(row.getId())) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					holder.getStatus() == Constants.STATUS_DELETED
							? "\"" + name + "\" was previously used and deleted. Add it again "
									+ "from ADD SERVER rather than renaming this row onto it."
							: "\"" + name + "\" is already added.");
		}

		// The USER LOG: every field as it was, then as saved — one log row per field changed.
		java.util.Map<String, String> before = snapshot(row);
		apply(row, in, name, years);
		ServerWithIpMaster saved = repository.save(row);
		changeLogRepository.saveAll(ChangeLogSupport.changes(saved.getId(), before, snapshot(saved),
				in.getChanged_by_code(), in.getChanged_by_name(), com.repl.asset.master.model.ServerChangeLog::new));
		return toBO(saved);
	}

	/** One server's USER LOG, newest first — shown below the View / Edit Server form. */
	@Transactional(readOnly = true)
	public List<com.repl.asset.master.bo.AssetChangeLogBO> changeLog(Long serverId) throws ApplicationException {
		if (serverId == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Server is required.");
		}
		return changeLogRepository.findByServerIdOrderByIdDesc(serverId).stream()
				.map(ChangeLogSupport::toBO)
				.toList();
	}

	/**
	 * Every logged field of a server, labelled and shown as on the form, in form order. Blank is
	 * null, so "" and a missing value never count as a change.
	 */
	private java.util.Map<String, String> snapshot(ServerWithIpMaster r) {
		DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
		m.put("Server Name", ChangeLogSupport.blankToNull(r.getServerName()));
		m.put("Type", ChangeLogSupport.blankToNull(r.getServerType()));
		m.put("S/N", ChangeLogSupport.blankToNull(r.getSerialNo()));
		m.put("Processor Ghz", ChangeLogSupport.blankToNull(r.getProcessor()));
		m.put("RAM", ChangeLogSupport.blankToNull(r.getRam()));
		m.put("HDD (GB)", ChangeLogSupport.blankToNull(r.getHdd()));
		m.put("System Type", ChangeLogSupport.blankToNull(r.getSystemType()));
		m.put("OS", ChangeLogSupport.blankToNull(r.getOsName()));
		m.put("OS Service Pack", ChangeLogSupport.blankToNull(r.getOsServicePack()));
		m.put("Other Software 1", ChangeLogSupport.blankToNull(r.getOtherSoftware1()));
		m.put("Other Software 2", ChangeLogSupport.blankToNull(r.getOtherSoftware2()));
		m.put("IP Address", ChangeLogSupport.blankToNull(r.getIpAddress()));
		m.put("Plant", ChangeLogSupport.blankToNull(r.getPlant()));
		m.put("Dept.", ChangeLogSupport.blankToNull(r.getDepartment()));
		m.put("Location", ChangeLogSupport.blankToNull(r.getLocation()));
		m.put("Warranty Status", ChangeLogSupport.blankToNull(r.getWarrantyStatus()));
		m.put("Warranty Start Date", r.getWarrantyStartDate() == null ? null : r.getWarrantyStartDate().format(dmy));
		m.put("Last Date Of Warranty", r.getLastDateOfWarranty() == null ? null : r.getLastDateOfWarranty().format(dmy));
		m.put("Total Warranty (In Years)", r.getTotalWarrantyYears() == null ? null
				: r.getTotalWarrantyYears().stripTrailingZeros().toPlainString());
		m.put("Description", ChangeLogSupport.blankToNull(r.getDescription()));
		m.put("Status", r.getStatus() == Constants.STATUS_ACTIVE ? "Active" : "Inactive");
		return m;
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		ServerWithIpMaster row = repository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That server no longer exists."));

		// Retired, not removed: anything already recording this server still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		repository.save(row);
	}

	/**
	 * Copy the form's values onto the row.
	 *
	 * Every field but the name is written unconditionally, including when it arrives blank —
	 * this form shows all of them at once, so an empty box genuinely means "clear it".
	 */
	private void apply(ServerWithIpMaster row, ServerAssetBO in, String name, BigDecimal years) {
		row.setServerName(name);

		row.setIpAddress(trimToNull(in.getIp_address()));
		row.setPlant(trimToNull(in.getPlant()));
		row.setDepartment(trimToNull(in.getDepartment()));
		row.setLocation(trimToNull(in.getLocation()));
		row.setServerType(trimToNull(in.getServer_type()));
		row.setSerialNo(trimToNull(in.getSerial_no()));
		row.setProcessor(trimToNull(in.getProcessor()));
		row.setRam(trimToNull(in.getRam()));
		row.setHdd(trimToNull(in.getHdd()));
		row.setWarrantyStatus(trimToNull(in.getWarranty_status()));
		row.setOwnerName(trimToNull(in.getOwner_name()));
		row.setOsName(trimToNull(in.getOs_name()));
		row.setOsServicePack(trimToNull(in.getOs_service_pack()));
		row.setSystemType(trimToNull(in.getSystem_type()));
		row.setOtherSoftware1(trimToNull(in.getOther_software_1()));
		row.setOtherSoftware2(trimToNull(in.getOther_software_2()));
		row.setDescription(trimToNull(in.getDescription()));

		row.setWarrantyStartDate(in.getWarranty_start_date());
		row.setLastDateOfWarranty(in.getLast_date_of_warranty());
		row.setTotalWarrantyYears(years);

		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	/** Stamped here, never taken from the request. */
	private void stamp(ServerWithIpMaster row) {
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
	}

	private String requireName(ServerAssetBO in) throws ApplicationException {
		String name = in == null || in.getServer_name() == null ? "" : in.getServer_name().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Server name is required.");
		}
		if (name.length() > NAME_MAX) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Server name is too long — " + name.length() + " characters, and the most "
							+ "allowed is " + NAME_MAX + ".");
		}
		return name;
	}

	/**
	 * TOTAL WARRANTY (IN YEARS). With both dates it is WORKED OUT, never taken from the request —
	 * the same rule as ADD DEVICE: whole months from the start to the day after the last date, in
	 * years to two decimals (01/10/2026 to 30/09/2029 is 3). The last date must be after the start.
	 * Without both dates (older servers kept only a typed term) the value sent is kept.
	 * Refused, not quietly dropped, when negative — a negative term would make the derived age nonsense.
	 */
	private BigDecimal requireYears(ServerAssetBO in) throws ApplicationException {
		LocalDate start = in == null ? null : in.getWarranty_start_date();
		LocalDate end = in == null ? null : in.getLast_date_of_warranty();
		if (start != null && end != null) {
			if (!end.isAfter(start)) {
				throw new ApplicationException(IResponseCode.INVALID_DATA,
						"Last Date Of Warranty must be after the Warranty Start Date.");
			}
			long months = java.time.Period.between(start, end.plusDays(1)).toTotalMonths();
			return months > 0
					? BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP)
					: null;
		}
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
	 * The row holding this name, deleted or not, or null.
	 *
	 * Case-insensitive. The table's UNIQUE key is the real guarantee; this exists so the
	 * caller gets a readable message instead of a constraint violation surfacing as a 500.
	 */
	private ServerWithIpMaster findIncludingDeleted(String name) {
		return repository.findAll().stream()
				.filter(row -> name.equalsIgnoreCase(row.getServerName()))
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
	 * AGE (Year.Month), worked out rather than stored.
	 *
	 * Bought is taken as (last date of warranty − term), since cover runs from purchase. Both
	 * are needed, so a row missing either has no age rather than a made-up one. A stored age
	 * would be wrong the day after it was written, which is exactly what the sheet suffers
	 * from.
	 */
	private String deriveAge(ServerWithIpMaster row) {
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

	private ServerAssetBO toBO(ServerWithIpMaster row) {
		ServerAssetBO bo = new ServerAssetBO();
		bo.setId(row.getId());
		bo.setServer_name(row.getServerName());

		bo.setIp_address(row.getIpAddress());
		bo.setPlant(row.getPlant());
		bo.setDepartment(row.getDepartment());
		bo.setLocation(row.getLocation());
		bo.setServer_type(row.getServerType());
		bo.setSerial_no(row.getSerialNo());
		bo.setProcessor(row.getProcessor());
		bo.setRam(row.getRam());
		bo.setHdd(row.getHdd());
		bo.setWarranty_status(row.getWarrantyStatus());
		bo.setOwner_name(row.getOwnerName());
		bo.setOs_name(row.getOsName());
		bo.setOs_service_pack(row.getOsServicePack());
		bo.setSystem_type(row.getSystemType());
		bo.setOther_software_1(row.getOtherSoftware1());
		bo.setOther_software_2(row.getOtherSoftware2());
		bo.setDescription(row.getDescription());

		bo.setWarranty_start_date(row.getWarrantyStartDate());
		bo.setLast_date_of_warranty(row.getLastDateOfWarranty());
		bo.setTotal_warranty_years(row.getTotalWarrantyYears());
		bo.setAge(deriveAge(row));

		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		bo.setAdded_via(row.getAddedVia());
		return bo;
	}
}
