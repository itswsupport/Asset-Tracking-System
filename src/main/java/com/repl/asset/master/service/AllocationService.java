package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.repl.asset.constant.Constants;
import com.repl.asset.master.model.AssetMaster;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.DeviceNameMaster;
import com.repl.asset.master.repository.AssetActionHistoryRepository;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.master.repository.DeviceNameMasterRepository;

/**
 * OPEN OR ASSIGNED — ams_device_name_master.allocation_status, kept in step with who actually
 * holds each machine. The one place the rule lives:
 *
 *   ASSIGNED  an employee holds it — a live Action History row with no end date; or it is a
 *             PRINTER that has been registered (a printer goes to a department, not a person,
 *             and keeps no employee history)
 *   OPEN      everything else — a name not yet registered, a machine registered with nobody
 *             assigned, one whose holder was released, one that was deleted
 *
 * Called after every write that can change it: a machine saved, updated, renamed or deleted
 * (AssetService), and every Action History save, update or delete (AssetActionHistoryService).
 * Worked out from the data each time rather than nudged, so it cannot drift.
 */
@Service
public class AllocationService {

	@Autowired
	private AssetActionHistoryRepository historyRepository;

	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	@Autowired
	private DeviceNameMasterRepository deviceNameMasterRepository;

	/** Re-work one machine's Open / Assigned from who holds it now, and store it on its name. */
	public void refresh(AssetMaster asset) {
		if (asset == null) {
			return;
		}
		String status = asset.getStatus() != Constants.STATUS_DELETED
				&& (isPrinter(asset.getDeviceId()) || hasHolder(asset.getId()))
						? DeviceNameMaster.ALLOCATION_ASSIGNED
						: DeviceNameMaster.ALLOCATION_OPEN;
		set(asset.getDeviceId(), asset.getName(), status);
	}

	/** Set a catalogue name's status directly — e.g. Open for the old name after a rename. */
	public void set(Long deviceId, String name, String allocation) {
		if (deviceId == null || name == null || name.isBlank()) {
			return;
		}
		for (DeviceNameMaster n : deviceNameMasterRepository
				.findByDeviceIdAndDeviceNameIgnoreCase(deviceId, name.trim())) {
			if (!allocation.equals(n.getAllocationStatus())) {
				n.setAllocationStatus(allocation);
				deviceNameMasterRepository.save(n);
			}
		}
	}

	/** Whether an employee holds the machine right now: a live history row with no end date. */
	public boolean hasHolder(Long assetId) {
		if (assetId == null) {
			return false;
		}
		return historyRepository.findByAssetId(assetId).stream()
				.filter(h -> h.getStatus() != Constants.STATUS_DELETED)
				.filter(h -> h.getEndDate() == null)
				.anyMatch(h -> notBlank(h.getAssignedEmpCode()) || notBlank(h.getAssignedEmpName()));
	}

	/** A printer is held by its department as soon as it is registered. */
	public boolean isPrinter(Long deviceId) {
		if (deviceId == null) {
			return false;
		}
		return deviceMasterRepository.findById(deviceId)
				.map(DeviceMaster::getDeviceName)
				.map(n -> "printer".equalsIgnoreCase(n.trim()))
				.orElse(false);
	}

	private static boolean notBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
