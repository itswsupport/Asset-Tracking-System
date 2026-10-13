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
import com.repl.asset.master.bo.ModelBO;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.ModelMaster;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.master.repository.ModelMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * Model master CRUD, over ams_model_master.
 *
 * Its own service rather than another AbstractSpecService subclass: that base class works on
 * a single name, while a model is a DEVICE and a NAME together — the same model name under
 * two device kinds is two different things, so the duplicate check needs both.
 */
@Service
public class ModelService {

	@Autowired
	private ModelMasterRepository modelMasterRepository;

	/** Read-only here: looked up to check the device exists and to name it on the way out. */
	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	/**
	 * Active AND inactive, everything but deleted.
	 *
	 * Both, because the master screen needs an inactive row visible to switch it back on.
	 * Screens that OFFER a model filter to status = 1 themselves.
	 */
	@Transactional(readOnly = true)
	public List<ModelBO> list() {
		/*
		 * Device names resolved in one pass rather than a findById per row. findAll rather
		 * than the active ones only: a model may point at a device since switched off, and
		 * showing its name beats showing a blank cell.
		 */
		Map<Long, String> deviceNames = deviceMasterRepository.findAll().stream()
				.collect(Collectors.toMap(DeviceMaster::getId, DeviceMaster::getDeviceName,
						(a, b) -> a));

		return modelMasterRepository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(ModelMaster::getModelName, String.CASE_INSENSITIVE_ORDER))
				.map(row -> {
					ModelBO bo = toBO(row);
					bo.setDevice_name(deviceNames.get(row.getDeviceId()));
					return bo;
				})
				.toList();
	}

	@Transactional
	public ModelBO save(ModelBO in) throws ApplicationException {
		DeviceMaster device = requireDevice(in);
		String name = requireName(in);

		ModelMaster existing = findIncludingDeleted(device.getId(), name);

		if (existing != null) {
			/*
			 * uk_device_model is UNIQUE on the pair, so a soft-deleted row still occupies it
			 * and a second insert is impossible. Re-adding a model that was deleted therefore
			 * revives that row — refusing would be wrong, since the screen shows nothing there.
			 */
			if (existing.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + name + "\" already exists under " + device.getDeviceName() + ".");
			}
			apply(existing, in, device, name);
			stamp(existing);
			return withDeviceName(modelMasterRepository.save(existing), device);
		}

		ModelMaster row = new ModelMaster();
		apply(row, in, device, name);
		stamp(row);

		return withDeviceName(modelMasterRepository.save(row), device);
	}

	@Transactional
	public ModelBO update(ModelBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		DeviceMaster device = requireDevice(in);
		String name = requireName(in);

		ModelMaster row = modelMasterRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That model no longer exists."));

		// Ignores the row being edited, so re-saving it unchanged is allowed.
		ModelMaster holder = findIncludingDeleted(device.getId(), name);
		if (holder != null && !holder.getId().equals(row.getId())) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					holder.getStatus() == Constants.STATUS_DELETED
							? "\"" + name + "\" was previously used under " + device.getDeviceName()
									+ " and deleted. Add it again from ADD rather than renaming this row onto it."
							: "\"" + name + "\" already exists under " + device.getDeviceName() + ".");
		}

		apply(row, in, device, name);
		return withDeviceName(modelMasterRepository.save(row), device);
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		ModelMaster row = modelMasterRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That model no longer exists."));

		// Retired, not removed: anything already recording this model still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		modelMasterRepository.save(row);
	}

	private void apply(ModelMaster row, ModelBO in, DeviceMaster device, String name) {
		row.setDeviceId(device.getId());
		row.setModelName(name);
		row.setDescription(trimToNull(in.getDescription()));
		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	/** Stamped here, never taken from the request. */
	private void stamp(ModelMaster row) {
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
	}

	/**
	 * The chosen device, checked rather than trusted: device_id arrives from the request, and
	 * a stale page could send the id of a device deleted since it loaded.
	 */
	private DeviceMaster requireDevice(ModelBO in) throws ApplicationException {
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

	private String requireName(ModelBO in) throws ApplicationException {
		String name = in == null || in.getModel_name() == null ? "" : in.getModel_name().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Model name is required.");
		}
		return name;
	}

	/**
	 * The row holding this device and model, deleted or not, or null.
	 *
	 * Case-insensitive on the name, so "dell latitude 3420" and "DELL Latitude 3420" count
	 * once. The table's UNIQUE key is the real guarantee; this exists so the caller gets a
	 * readable message instead of a constraint violation surfacing as a 500.
	 */
	private ModelMaster findIncludingDeleted(Long deviceId, String name) {
		return modelMasterRepository.findAll().stream()
				.filter(row -> deviceId.equals(row.getDeviceId()))
				.filter(row -> name.equalsIgnoreCase(row.getModelName()))
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

	private ModelBO withDeviceName(ModelMaster row, DeviceMaster device) {
		ModelBO bo = toBO(row);
		bo.setDevice_name(device.getDeviceName());
		return bo;
	}

	private ModelBO toBO(ModelMaster row) {
		ModelBO bo = new ModelBO();
		bo.setId(row.getId());
		bo.setDevice_id(row.getDeviceId());
		bo.setModel_name(row.getModelName());
		bo.setDescription(row.getDescription());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
