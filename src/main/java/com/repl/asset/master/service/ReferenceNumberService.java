package com.repl.asset.master.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.repl.asset.constant.Constants;
import com.repl.asset.master.model.AssetMaster;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.DeviceNameMaster;
import com.repl.asset.master.repository.AssetMasterRepository;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.master.repository.DeviceNameMasterRepository;

/**
 * THE REFERENCE NUMBER — the one place it is made, for DEVICE NAME MASTER and ADD DEVICE alike.
 *
 * Four parts run together with no separators, e.g. DE26HR01007:
 *
 *   DE      first two letters of the device type (DEsktop, LAptop, PRinter, TAblet)
 *   26      the financial year it was created in (1 April - 31 March), two digits
 *   HR01    the device name, spaces removed, upper case
 *   007     running number per device type, starting again at 001 each financial year
 *
 * CREATED AS SOON AS THE NAME IS CREATED in Device Name Master (device + model + name), and
 * stored there. ADD DEVICE then gives the machine that SAME number when it is registered, so
 * the device lists, the form and the register all show one number from the start.
 *
 * The running number is the next after the highest in EITHER table for that type and year, so
 * a number is never handed out twice. The device row is locked first so two saves at the same
 * moment queue up instead of both reading the same MAX.
 *
 * Never changed once a machine is registered: a number already written on a label or a form
 * moving would be worse than none.
 */
@Service
public class ReferenceNumberService {

	private static final Logger logger = LoggerFactory.getLogger(ReferenceNumberService.class);


	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	@Autowired
	private AssetMasterRepository assetMasterRepository;

	@Autowired
	private DeviceNameMasterRepository deviceNameRepository;

	/** For the start-up backfill's own transaction. */
	@Autowired
	private PlatformTransactionManager transactionManager;

	/**
	 * A catalogue name's number — kept if it has one, otherwise the registered machine's (when
	 * it was added through ADD DEVICE first), otherwise a new one.
	 */
	public void assign(DeviceNameMaster name, DeviceMaster device) {
		if (notBlank(name.getReferenceNo())) {
			return;
		}
		AssetMaster asset = registered(device.getId(), name.getDeviceName());
		if (asset != null && notBlank(asset.getReferenceNo())) {
			name.setRefFy(asset.getRefFy());
			name.setRefSeq(asset.getRefSeq());
			name.setReferenceNo(asset.getReferenceNo());
			return;
		}
		deviceMasterRepository.lockById(device.getId());
		int fy = financialYear(LocalDate.now());
		int seq = nextSeq(device.getId(), fy);
		name.setRefFy(fy);
		name.setRefSeq(seq);
		name.setReferenceNo(build(device, name.getDeviceName(), fy, seq));
	}

	/**
	 * Makes a catalogue name's number again after a rename or a change of device type — the old
	 * number would name the wrong machine — but only while it is not registered in ADD DEVICE.
	 */
	public void renumberIfUnregistered(DeviceNameMaster name, DeviceMaster device, Long oldDeviceId,
			String oldName) {
		// Registered under its old name and type: that machine's number is fixed, so keep it.
		if (notBlank(name.getReferenceNo()) && oldDeviceId != null && registered(oldDeviceId, oldName) != null) {
			return;
		}
		name.setReferenceNo(null);
		name.setRefFy(null);
		name.setRefSeq(null);
		assign(name, device);
	}

	/**
	 * A machine's number, the first time it is saved: the one its name was given in Device Name
	 * Master, or — for a name not in it — a new one, written back onto the catalogue row too if
	 * that has none, so the two always agree.
	 */
	public void assign(AssetMaster row, DeviceMaster device) {
		if (notBlank(row.getReferenceNo())) {
			return;
		}
		DeviceNameMaster name = catalogue(device.getId(), row.getName());
		if (name != null && notBlank(name.getReferenceNo())) {
			row.setRefFy(name.getRefFy());
			row.setRefSeq(name.getRefSeq());
			row.setReferenceNo(name.getReferenceNo());
			return;
		}
		deviceMasterRepository.lockById(device.getId());
		int fy = financialYear(LocalDate.now());
		int seq = nextSeq(device.getId(), fy);
		row.setRefFy(fy);
		row.setRefSeq(seq);
		row.setReferenceNo(build(device, row.getName(), fy, seq));
		if (name != null) {
			name.setRefFy(fy);
			name.setRefSeq(seq);
			name.setReferenceNo(row.getReferenceNo());
			deviceNameRepository.save(name);
		}
	}

	/**
	 * What ADD DEVICE shows once a type and name are chosen: the name's own number when it has
	 * one — the number the machine WILL get — otherwise a preview of the next one.
	 */
	public String preview(DeviceMaster device, String name) {
		DeviceNameMaster catalogued = catalogue(device.getId(), name);
		if (catalogued != null && notBlank(catalogued.getReferenceNo())) {
			return catalogued.getReferenceNo();
		}
		int fy = financialYear(LocalDate.now());
		return build(device, name, fy, nextSeq(device.getId(), fy));
	}

	/**
	 * Preview numbers for names that somehow have none yet, oldest first, each the next free
	 * number so no two rows show the same one. A safety net only — every name gets a stored
	 * number on creation, and old ones at start-up (backfill).
	 */
	public Map<String, String> previewAll(DeviceMaster device, List<String> names) {
		int fy = financialYear(LocalDate.now());
		int seq = nextSeq(device.getId(), fy);
		Map<String, String> out = new LinkedHashMap<>();
		for (String name : names) {
			out.put(name, build(device, name, fy, seq++));
		}
		return out;
	}

	/**
	 * ON START-UP: every device saved through ADD DEVICE before numbers existed, and every name
	 * created before they were made in Device Name Master, gets its number — oldest first.
	 * Machines first, so a registered name then takes its machine's number. Does nothing once
	 * every one has a number, so restarting is harmless.
	 *
	 * In its own transaction and never fatal: if it fails (e.g. the reference_no / ref_fy /
	 * ref_seq columns not yet added to ams_device_name_master) it is logged and the server
	 * carries on running.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void backfill() {
		try {
			new TransactionTemplate(transactionManager).executeWithoutResult(s -> fillMissing());
		} catch (Exception e) {
			logger.error("Could not fill in missing reference numbers — have the reference_no, ref_fy "
					+ "and ref_seq columns been added to ams_device_name_master?", e);
		}
	}

	private void fillMissing() {
		Map<Long, DeviceMaster> devices = deviceMasterRepository.findAll().stream()
				.collect(Collectors.toMap(DeviceMaster::getId, Function.identity(), (a, b) -> a));

		List<AssetMaster> machines = assetMasterRepository.findAll().stream()
				.filter(a -> a.getStatus() != Constants.STATUS_DELETED)
				.filter(a -> notBlank(a.getName()))
				.filter(a -> !notBlank(a.getReferenceNo()))
				.sorted(Comparator.comparing(AssetMaster::getId))
				.toList();
		int doneMachines = 0;
		for (AssetMaster a : machines) {
			DeviceMaster device = devices.get(a.getDeviceId());
			if (device == null) {
				continue;
			}
			assign(a, device);
			assetMasterRepository.saveAndFlush(a);
			doneMachines++;
		}

		List<DeviceNameMaster> names = deviceNameRepository.findAll().stream()
				.filter(n -> n.getStatus() != Constants.STATUS_DELETED)
				.filter(n -> notBlank(n.getDeviceName()))
				.filter(n -> !notBlank(n.getReferenceNo()))
				.sorted(Comparator.comparing(DeviceNameMaster::getId))
				.toList();
		int doneNames = 0;
		for (DeviceNameMaster n : names) {
			DeviceMaster device = devices.get(n.getDeviceId());
			if (device == null) {
				continue;
			}
			assign(n, device);
			deviceNameRepository.saveAndFlush(n);
			doneNames++;
		}

		if (doneMachines + doneNames > 0) {
			logger.info("Reference numbers filled in for {} device(s) and {} device name(s).",
					doneMachines, doneNames);
		}
	}

	/** The next running number for the type and year, past the highest in either table. */
	private int nextSeq(Long deviceId, int fy) {
		Integer assets = assetMasterRepository.findMaxRefSeq(deviceId, fy);
		Integer names = deviceNameRepository.findMaxRefSeq(deviceId, fy);
		return Math.max(assets == null ? 0 : assets, names == null ? 0 : names) + 1;
	}

	/** %03d pads but never truncates, so 1000 stays 1000. */
	private String build(DeviceMaster device, String name, int fy, int seq) {
		String type = device.getDeviceName() == null ? ""
				: device.getDeviceName().replaceAll("[^A-Za-z]", "").toUpperCase();
		String initials = type.length() >= 2 ? type.substring(0, 2) : type;
		String cleanName = name == null ? "" : name.replaceAll("\\s+", "").toUpperCase();
		return initials + String.format("%02d", fy % 100) + cleanName + String.format("%03d", seq);
	}

	/**
	 * The financial year, the same rule as the TMS ticket number: 1 April -> 31 March, named
	 * for the April it began in. 15 Feb 2027 is still FY 2026 ("26").
	 */
	private int financialYear(LocalDate date) {
		return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
	}

	/** The catalogue row for this name under this type, ignoring case; null if none. */
	private DeviceNameMaster catalogue(Long deviceId, String name) {
		if (!notBlank(name)) {
			return null;
		}
		return deviceNameRepository.findByDeviceIdAndDeviceNameIgnoreCase(deviceId, name.trim()).stream()
				.filter(n -> n.getStatus() != Constants.STATUS_DELETED)
				.findFirst()
				.orElse(null);
	}

	/** The machine registered through ADD DEVICE under this name and type; null if none. */
	private AssetMaster registered(Long deviceId, String name) {
		if (!notBlank(name)) {
			return null;
		}
		String wanted = name.trim();
		return assetMasterRepository.findByDeviceId(deviceId).stream()
				.filter(a -> a.getStatus() != Constants.STATUS_DELETED)
				.filter(a -> a.getName() != null && wanted.equalsIgnoreCase(a.getName().trim()))
				.findFirst()
				.orElse(null);
	}

	private static boolean notBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
