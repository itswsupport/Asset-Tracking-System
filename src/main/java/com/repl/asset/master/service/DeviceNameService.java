package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.DeviceNameBO;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.DeviceNameMaster;
import com.repl.asset.master.model.ModelMaster;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.master.repository.DeviceNameMasterRepository;
import com.repl.asset.master.repository.ModelMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * Device name master CRUD, over ams_device_name_master.
 *
 * Its own service rather than another AbstractSpecService subclass: that base class works on a
 * single name, while this row is a device, a model and a name together.
 *
 * The rule this service exists to enforce is that the model belongs to the device. The form
 * narrows the model dropdown to the chosen device, but that is a convenience for whoever is
 * typing, not a guarantee — the request carries two independent ids and could arrive with any
 * pair at all, from a stale page or from anything that is not the form. requireModel checks it
 * on every save and update.
 */
@Service
public class DeviceNameService {

	@Autowired
	private DeviceNameMasterRepository deviceNameMasterRepository;

	/** Read-only here: looked up to check the device exists and to name it on the way out. */
	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	/** Read-only here: looked up to check the model exists, fits the device, and to name it. */
	@Autowired
	private ModelMasterRepository modelMasterRepository;

	/** Creates each name's reference number when the name is created. */
	@Autowired
	private ReferenceNumberService referenceNumberService;
	/**
	 * Active AND inactive, everything but deleted.
	 *
	 * Both, because the master screen needs an inactive row visible to switch it back on.
	 * Screens that OFFER a machine filter to status = 1 themselves.
	 */
	@Transactional(readOnly = true)
	public List<DeviceNameBO> list() {
		/*
		 * Both lookups resolved in one pass rather than a findById per row. findAll rather than
		 * the active ones only: a machine may point at a device or model since switched off,
		 * and showing the name beats showing a blank cell.
		 */
		Map<Long, String> deviceNames = deviceMasterRepository.findAll().stream()
				.collect(Collectors.toMap(DeviceMaster::getId, DeviceMaster::getDeviceName,
						(a, b) -> a));

		Map<Long, String> modelNames = modelMasterRepository.findAll().stream()
				.collect(Collectors.toMap(ModelMaster::getId, ModelMaster::getModelName,
						(a, b) -> a));

		return deviceNameMasterRepository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(DeviceNameMaster::getDeviceName,
						String.CASE_INSENSITIVE_ORDER))
				.map(row -> {
					DeviceNameBO bo = toBO(row);
					bo.setDevice(deviceNames.get(row.getDeviceId()));
					bo.setModel_name(modelNames.get(row.getModelId()));
					return bo;
				})
				.toList();
	}

	@Transactional
	public DeviceNameBO save(DeviceNameBO in) throws ApplicationException {
		DeviceMaster device = requireDevice(in);
		ModelMaster model = requireModel(in, device);
		String name = requireName(in);

		DeviceNameMaster existing = findIncludingDeleted(name);

		if (existing != null) {
			/*
			 * uk_device_name is UNIQUE, so a soft-deleted row still occupies that name and a
			 * second insert is impossible. Re-adding a name that was deleted therefore revives
			 * that row — refusing would be wrong, since the screen shows nothing there.
			 */
			if (existing.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + name + "\" already exists.");
			}
			Long oldDeviceId = existing.getDeviceId();
			String oldName = existing.getDeviceName();
			apply(existing, in, device, model, name);
			stamp(existing);
			number(existing, device, oldDeviceId, oldName);
			return withNames(deviceNameMasterRepository.save(existing), device, model);
		}

		DeviceNameMaster row = new DeviceNameMaster();
		apply(row, in, device, model, name);
		stamp(row);
		// The reference number is created now, as soon as the device, model and name are saved.
		referenceNumberService.assign(row, device);

		return withNames(deviceNameMasterRepository.save(row), device, model);
	}

	@Transactional
	public DeviceNameBO update(DeviceNameBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		DeviceMaster device = requireDevice(in);
		ModelMaster model = requireModel(in, device);
		String name = requireName(in);

		DeviceNameMaster row = deviceNameMasterRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That device name no longer exists."));

		// Ignores the row being edited, so re-saving it unchanged is allowed.
		DeviceNameMaster holder = findIncludingDeleted(name);
		if (holder != null && !holder.getId().equals(row.getId())) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					holder.getStatus() == Constants.STATUS_DELETED
							? "\"" + name + "\" was previously used and deleted. Add it again "
									+ "from ADD rather than renaming this row onto it."
							: "\"" + name + "\" already exists.");
		}

		Long oldDeviceId = row.getDeviceId();
		String oldName = row.getDeviceName();
		apply(row, in, device, model, name);
		number(row, device, oldDeviceId, oldName);
		return withNames(deviceNameMasterRepository.save(row), device, model);
	}

	/**
	 * The row's reference number after a save. Kept as it is unless the device type or the name
	 * changed — the number is built from both — and then made again, but only while the machine
	 * is not yet registered in ADD DEVICE; a registered machine keeps its number for good.
	 */
	private void number(DeviceNameMaster row, DeviceMaster device, Long oldDeviceId, String oldName) {
		boolean renamed = oldName == null || !oldName.replaceAll("\\s+", "")
				.equalsIgnoreCase(row.getDeviceName().replaceAll("\\s+", ""));
		if (!device.getId().equals(oldDeviceId) || renamed) {
			referenceNumberService.renumberIfUnregistered(row, device, oldDeviceId, oldName);
		} else {
			referenceNumberService.assign(row, device);
		}
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		DeviceNameMaster row = deviceNameMasterRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That device name no longer exists."));

		// Retired, not removed: anything already recording this machine still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		deviceNameMasterRepository.save(row);
	}

	private void apply(DeviceNameMaster row, DeviceNameBO in, DeviceMaster device,
			ModelMaster model, String name) {
		row.setDeviceId(device.getId());
		row.setModelId(model.getId());
		row.setDeviceName(name);
		row.setDescription(trimToNull(in.getDescription()));
		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	/** Stamped here, never taken from the request. */
	private void stamp(DeviceNameMaster row) {
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
	}

	/**
	 * The chosen device, checked rather than trusted: device_id arrives from the request, and a
	 * stale page could send the id of a device deleted since it loaded.
	 */
	private DeviceMaster requireDevice(DeviceNameBO in) throws ApplicationException {
		if (in == null || in.getDevice_id() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device is required.");
		}
		DeviceMaster device = deviceMasterRepository.findById(in.getDevice_id())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That device no longer exists."));

		if (device.getStatus() == Constants.STATUS_DELETED) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + device.getDeviceName() + "\" has been deleted from the device master.");
		}
		return device;
	}

	/**
	 * The chosen model, checked to exist AND to belong to the chosen device.
	 *
	 * That second check is the point of this method. Without it a request could pair a laptop
	 * with a printer's model and the row would look perfectly valid afterwards, with nothing to
	 * show which of the two ids was the wrong one.
	 */
	private ModelMaster requireModel(DeviceNameBO in, DeviceMaster device)
			throws ApplicationException {
		if (in == null || in.getModel_id() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Model is required.");
		}
		ModelMaster model = modelMasterRepository.findById(in.getModel_id())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That model no longer exists."));

		if (model.getStatus() == Constants.STATUS_DELETED) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + model.getModelName() + "\" has been deleted from the model master.");
		}
		if (!device.getId().equals(model.getDeviceId())) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"\"" + model.getModelName() + "\" is not a model of "
							+ device.getDeviceName() + ".");
		}
		return model;
	}

	private String requireName(DeviceNameBO in) throws ApplicationException {
		String name = in == null || in.getDevice_name() == null ? "" : in.getDevice_name().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device name is required.");
		}
		return name;
	}

	/**
	 * The row holding this name, deleted or not, or null.
	 *
	 * Case-insensitive, so "it-101" and "IT-101" count once. The table's UNIQUE key is the real
	 * guarantee; this exists so the caller gets a readable message instead of a constraint
	 * violation surfacing as a 500.
	 */
	private DeviceNameMaster findIncludingDeleted(String name) {
		return deviceNameMasterRepository.findAll().stream()
				.filter(row -> name.equalsIgnoreCase(row.getDeviceName()))
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

	private DeviceNameBO withNames(DeviceNameMaster row, DeviceMaster device, ModelMaster model) {
		DeviceNameBO bo = toBO(row);
		bo.setDevice(device.getDeviceName());
		bo.setModel_name(model.getModelName());
		return bo;
	}

	private DeviceNameBO toBO(DeviceNameMaster row) {
		DeviceNameBO bo = new DeviceNameBO();
		bo.setId(row.getId());
		bo.setDevice_id(row.getDeviceId());
		bo.setModel_id(row.getModelId());
		bo.setDevice_name(row.getDeviceName());
		bo.setDescription(row.getDescription());
		bo.setReference_no(row.getReferenceNo());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
