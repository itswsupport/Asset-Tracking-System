package com.repl.asset.master.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.AssetActionHistoryBO;
import com.repl.asset.master.bo.DeviceStatusBO;
import com.repl.asset.master.model.AssetActionHistory;
import com.repl.asset.master.model.AssetMaster;
import com.repl.asset.master.model.DeviceMaster;
import com.repl.asset.master.model.DeviceNameMaster;
import com.repl.asset.master.model.ModelMaster;
import com.repl.asset.master.repository.AssetActionHistoryRepository;
import com.repl.asset.master.repository.AssetMasterRepository;
import com.repl.asset.master.repository.DeviceMasterRepository;
import com.repl.asset.master.repository.DeviceNameMasterRepository;
import com.repl.asset.master.repository.ModelMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * DEVICE STATUS — every machine of one device type, as either ASSIGNED or OPEN.
 *
 * READ ONLY. OPEN / ASSIGNED is read from ams_device_name_master.allocation_status, which
 * AssetService sets as devices are added, renamed and deleted. The holder and history come
 * from ams_add_device_master and ams_asset_action_history.
 *
 * Assigning or releasing from the screen goes through /assetactionhistory/save and /update,
 * the same endpoints ADD DEVICE uses — there is no second way to write the log.
 *
 * Same rows as LIST DEVICE: machines added through ADD DEVICE, deleted ones left out.
 */
@Service
public class DeviceStatusService {


	public static final String ASSIGNED = "ASSIGNED";
	public static final String OPEN = "OPEN";

	@Autowired
	private AssetMasterRepository assetMasterRepository;

	@Autowired
	private AssetActionHistoryRepository historyRepository;

	/** The device-name catalogue: every name created in Device Name Master. */
	@Autowired
	private DeviceNameMasterRepository deviceNameRepository;

	/** For a catalogue name's model, which the catalogue stores only as model_id. */
	@Autowired
	private ModelMasterRepository modelRepository;

	/** The Open / Assigned rule, for machines whose name is not in the catalogue. */
	@Autowired
	private AllocationService allocationService;

	/** The reference number a name not yet saved through ADD DEVICE will get. */
	@Autowired
	private ReferenceNumberService referenceNumberService;

	@Autowired
	private DeviceMasterRepository deviceMasterRepository;

	/** Newest entry first: by start date, then the one recorded later, then id. */
	private static final Comparator<AssetActionHistory> NEWEST_FIRST = Comparator
			.comparing(AssetActionHistory::getStartDate, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(AssetActionHistory::getCreateDate, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(AssetActionHistory::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(AssetActionHistory::getId)
			.reversed();

	@Transactional(readOnly = true)
	public List<DeviceStatusBO> list(Long deviceId) throws ApplicationException {
		if (deviceId == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device is required.");
		}

		List<AssetMaster> assets = assetMasterRepository.findByDeviceId(deviceId).stream()
				.filter(a -> a.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(AssetMaster::getId).reversed())
				.toList();

		// Every machine's history in one query, grouped by machine.
		Map<Long, List<AssetActionHistory>> historyByAsset = assets.isEmpty()
				? Map.of()
				: historyRepository.findByAssetIdIn(assets.stream().map(AssetMaster::getId).toList()).stream()
						.filter(h -> h.getStatus() != Constants.STATUS_DELETED)
						.filter(h -> notBlank(h.getAssignedEmpCode()) || notBlank(h.getAssignedEmpName()))
						.collect(Collectors.groupingBy(AssetActionHistory::getAssetId));

		Map<String, AssetMaster> assetsByName = assets.stream()
				.collect(Collectors.toMap(a -> normalise(a.getName()), a -> a, (x, y) -> x));
		// Only this device type's models, read from the database by device_id.
		Map<Long, String> modelNames = modelRepository.findByDeviceId(deviceId).stream()
				.collect(Collectors.toMap(ModelMaster::getId, ModelMaster::getModelName, (a, b) -> a));

		/*
		 * THE STATE IS STORED, in ams_device_name_master.allocation_status: "Open" when the name is
		 * created in Device Name Master, "Assigned" once that device is added through ADD DEVICE
		 * (AssetService keeps it in step). Each catalogue name of this type is one row here,
		 * matched to its registered machine by name, ignoring case ("it383" is "IT383").
		 */
		List<DeviceStatusBO> result = new ArrayList<>();
		Set<Long> shown = new HashSet<>();
		// Only this type's active names, read from the database — not every name of every type.
		deviceNameRepository.findByDeviceIdAndStatus(deviceId, Constants.STATUS_ACTIVE).stream()
				.filter(n -> notBlank(n.getDeviceName()))
				.sorted(Comparator.comparing(DeviceNameMaster::getId).reversed())
				.forEach(n -> {
					String state = DeviceNameMaster.ALLOCATION_ASSIGNED.equalsIgnoreCase(n.getAllocationStatus())
							? ASSIGNED
							: OPEN;
					AssetMaster asset = assetsByName.get(normalise(n.getDeviceName()));
					if (asset != null) {
						shown.add(asset.getId());
						result.add(toStatus(asset, historyByAsset.getOrDefault(asset.getId(), List.of()), state));
					} else {
						result.add(catalogueOnly(n, modelNames.get(n.getModelId()), state));
					}
				});

		/*
		 * A machine added through ADD DEVICE whose name is not in the catalogue has no stored
		 * status, so it is worked out the same way AllocationService does: ASSIGNED while an
		 * employee holds it (or it is a printer), otherwise OPEN.
		 */
		boolean printer = allocationService.isPrinter(deviceId);
		assets.stream()
				.filter(a -> !shown.contains(a.getId()))
				.forEach(a -> {
					List<AssetActionHistory> entries = historyByAsset.getOrDefault(a.getId(), List.of());
					boolean held = entries.stream().anyMatch(h -> h.getEndDate() == null);
					result.add(toStatus(a, entries, printer || held ? ASSIGNED : OPEN));
				});

		/*
		 * Safety net: every name gets its number stored when it is created, so this only covers
		 * a name that somehow still has none — it shows the next free numbers, oldest name first
		 * (the list is newest first, hence the reverse), never a blank.
		 */
		List<DeviceStatusBO> unregistered = result.stream()
				.filter(r -> !r.isRegistered())
				.filter(r -> r.getReference_no() == null || r.getReference_no().isBlank())
				.toList();
		if (!unregistered.isEmpty()) {
			DeviceMaster device = deviceMasterRepository.findById(deviceId).orElse(null);
			if (device != null) {
				List<String> oldestFirst = new ArrayList<>(unregistered.stream().map(DeviceStatusBO::getName).toList());
				Collections.reverse(oldestFirst);
				Map<String, String> previews = referenceNumberService.previewAll(device, oldestFirst);
				unregistered.forEach(r -> r.setReference_no(previews.get(r.getName())));
			}
		}

		return result;
	}

	/**
	 * A catalogue name with no machine record yet. Its id is the NEGATIVE of its Device Name
	 * Master id, so it can never collide with a real machine's id. It has no history yet, but
	 * already has its reference number, stored when the name was created.
	 */
	private DeviceStatusBO catalogueOnly(DeviceNameMaster name, String modelName, String state) {
		DeviceStatusBO bo = new DeviceStatusBO();
		bo.setId(-name.getId());
		bo.setRegistered(false);
		// Created with the name in Device Name Master; the one ADD DEVICE will give the machine.
		bo.setReference_no(name.getReferenceNo());
		bo.setName(name.getDeviceName());
		bo.setModel(modelName);
		bo.setState(state);
		bo.setSince(name.getCreateDate());
		bo.setHistory(List.of());
		return bo;
	}

	private static String normalise(String s) {
		return s == null ? "" : s.trim().toLowerCase();
	}

	/**
	 * A registered machine, in the state the catalogue gives it. The HOLDER is whoever has the
	 * open history row; a device with no such row (a printer, which goes to a department and
	 * keeps no employee history) shows its department instead.
	 */
	private DeviceStatusBO toStatus(AssetMaster asset, List<AssetActionHistory> entries, String state) {
		List<AssetActionHistory> newestFirst = entries.stream().sorted(NEWEST_FIRST).toList();

		DeviceStatusBO bo = new DeviceStatusBO();
		bo.setId(asset.getId());
		bo.setRegistered(true);
		bo.setReference_no(asset.getReferenceNo());
		bo.setName(asset.getName());
		bo.setModel(asset.getModel());
		bo.setAsset_code(asset.getAssetCode());
		bo.setPlant(asset.getPlant());
		bo.setHistory(newestFirst.stream().map(this::toHistoryBO).toList());
		bo.setState(state);

		AssetActionHistory holding = newestFirst.stream()
				.filter(h -> h.getEndDate() == null)
				.findFirst()
				.orElse(null);
		if (holding != null) {
			bo.setHolder_entry_id(holding.getId());
			bo.setHolder_code(holding.getAssignedEmpCode());
			bo.setHolder_name(holding.getAssignedEmpName());
			bo.setSince(holding.getStartDate());
		} else {
			bo.setHolder_name(asset.getDepartment());
			bo.setSince(asset.getCreateDate());
		}
		return bo;
	}

	private AssetActionHistoryBO toHistoryBO(AssetActionHistory row) {
		AssetActionHistoryBO bo = new AssetActionHistoryBO();
		bo.setId(row.getId());
		bo.setAsset_id(row.getAssetId());
		bo.setAssigned_emp_code(row.getAssignedEmpCode());
		bo.setAssigned_emp_name(row.getAssignedEmpName());
		bo.setStart_date(row.getStartDate());
		bo.setEnd_date(row.getEndDate());
		bo.setAction_status(row.getActionStatus());
		bo.setAction_by_emp_code(row.getActionByEmpCode());
		bo.setAction_by_emp_name(row.getActionByEmpName());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		return bo;
	}

	private static boolean notBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
