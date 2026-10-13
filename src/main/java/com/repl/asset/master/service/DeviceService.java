package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.DeviceBO;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * Device master CRUD, over ams_device_master.
 *
 * Its own service rather than another AbstractSpecService subclass: that base class works on
 * a single name, while a device is a TYPE and a NAME together — the same name means different
 * machines under different types, so the duplicate check has to consider both.
 */
@Service
public class DeviceService {

	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	/**
	 * Active AND inactive, everything but deleted.
	 *
	 * Both, because the admin screen needs an inactive row visible to switch it back on. The
	 * select-device dropdown users see filters to status = 1 on its own — that is what makes
	 * an inactive device disappear from their side while staying here.
	 */
	@Transactional(readOnly = true)
	public List<DeviceBO> list() {
		return deviceMasterRepository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(DeviceMaster::getDeviceName, String.CASE_INSENSITIVE_ORDER))
				.map(this::toBO)
				.toList();
	}

	@Transactional
	public DeviceBO save(DeviceBO in) throws ApplicationException {
		String name = requireName(in);

		DeviceMaster existing = findIncludingDeleted(name);

		if (existing != null) {
			/*
			 * uk_device_name is UNIQUE, so a soft-deleted row still occupies that name and a
			 * second insert is impossible. Re-adding a name that was deleted therefore revives
			 * that row — refusing would be wrong, since the screen shows
			 * nothing there.
			 */
			if (existing.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + name + "\" already exists.");
			}
			// Re-typed casing wins, so re-adding "edc712" as "EDC712" corrects it.
			existing.setDeviceName(name);
			existing.setDescription(in.getDescription());
			existing.setStatus(statusOrActive(in.getStatus()));
			existing.setCreateDate(LocalDate.now());
			existing.setCreateTime(LocalTime.now().withNano(0));
			return toBO(deviceMasterRepository.save(existing));
		}

		DeviceMaster row = new DeviceMaster();
		row.setDescription(in.getDescription());
		row.setDeviceName(name);
		row.setStatus(statusOrActive(in.getStatus()));
		if (in.getDescription() != null) {
			row.setDescription(in.getDescription());
		}
		applyDetails(row, in);
		// Stamped here, never taken from the request.
		row.setCreateDate(LocalDate.now());
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		row.setCreateTime(LocalTime.now().withNano(0));

		return toBO(deviceMasterRepository.save(row));
	}

	@Transactional
	public DeviceBO update(DeviceBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		String name = requireName(in);

		DeviceMaster row = deviceMasterRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That device no longer exists."));

		// Ignores the row being edited, so re-saving it unchanged is allowed.
		DeviceMaster holder = findIncludingDeleted(name);
		if (holder != null && !holder.getId().equals(row.getId())) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					holder.getStatus() == Constants.STATUS_DELETED
							? "\"" + name + "\" was previously used and deleted. Add it again from "
									+ "ADD rather than renaming this row onto it."
							: "\"" + name + "\" already exists.");
		}

		row.setDeviceName(name);
		if (in.getStatus() != null) {
			row.setStatus(statusOrActive(in.getStatus()));
		}
		applyDetails(row, in);
		return toBO(deviceMasterRepository.save(row));
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		DeviceMaster row = deviceMasterRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That device no longer exists."));

		// Retired, not removed: anything already recording this device still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		deviceMasterRepository.save(row);
	}

	private String requireName(DeviceBO in) throws ApplicationException {
		String name = in == null || in.getDevice_name() == null ? "" : in.getDevice_name().trim();
		if (name.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device name is required.");
		}
		return name;
	}

	/**
	 * The row holding this type and name, deleted or not, or null.
	 *
	 * Case-insensitive on both, so "laptop"/"101" and "Laptop"/"101" are one device rather
	 * than two. The table's UNIQUE key is the real guarantee; this exists so the caller gets
	 * a readable message instead of a constraint violation surfacing as a 500.
	 */
	private DeviceMaster findIncludingDeleted(String name) {
		return deviceMasterRepository.findAll().stream()
				.filter(row -> name.equalsIgnoreCase(row.getDeviceName()))
				.findFirst()
				.orElse(null);
	}

	/**
	 * The LIST DEVICE form's fields, applied only when they were actually sent.
	 *
	 * Null means "not on this form", not "clear it". The admin screen saves a device with a
	 * name and a status alone, and it must not wipe the system type or the warranty the user
	 * side filled in on the same row.
	 */
	private void applyDetails(DeviceMaster row, DeviceBO in) {
		if (in.getSystem_type() != null) {
			row.setSystemType(trimToNull(in.getSystem_type()));
		}
		if (in.getWarranty_status() != null) {
			row.setWarrantyStatus(trimToNull(in.getWarranty_status()));
		}
		if (in.getLast_date_of_warranty() != null) {
			row.setLastDateOfWarranty(in.getLast_date_of_warranty());
		}
		if (in.getTotal_warranty_years() != null) {
			row.setTotalWarrantyYears(in.getTotal_warranty_years());
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/** Only 1 or 0 may arrive from a form; -1 is reserved for delete. */
	private int statusOrActive(Integer status) {
		return status != null && status == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE;
	}

	private DeviceBO toBO(DeviceMaster row) {
		DeviceBO bo = new DeviceBO();
		bo.setId(row.getId());
		bo.setDevice_name(row.getDeviceName());
		bo.setSystem_type(row.getSystemType());
		bo.setWarranty_status(row.getWarrantyStatus());
		bo.setLast_date_of_warranty(row.getLastDateOfWarranty());
		bo.setTotal_warranty_years(row.getTotalWarrantyYears());
		bo.setDescription(row.getDescription());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
