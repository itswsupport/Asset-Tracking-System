package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.AssetActionHistoryBO;
import com.repl.asset.master.model.AssetActionHistory;
import com.repl.asset.master.model.AssetMaster;
import com.repl.asset.master.repository.AssetMasterRepository;
import com.repl.asset.master.repository.AssetActionHistoryRepository;
import com.repl.asset.message.IResponseCode;

/**
 * ASSET ACTION HISTORY — one asset's chain of holders, first to last.
 *
 * ams_asset_action_history holds one row per handover: who had it, from when to when, who it
 * went to next, and why. A machine that passed between four people over three years is three
 * rows against the one asset_id, and the chain is as long as it needs to be.
 *
 * APPEND-ONLY IN SPIRIT. Entries are added as things happen and read back in order; there is
 * an update here only for correcting a mistake, not for rewriting what happened. Nothing
 * dedupes and nothing is unique, because the same person holding the same machine twice in
 * two periods is a real thing to record.
 *
 * IT ALSO OWNS THE SUMMARY ON THE MACHINE. Every write here ends by stamping the newest entry
 * onto ams_add_device_master — its holder, when they got it, why, and when it was recorded —
 * so a list of machines can say where each one is without joining the log. See stampMaster.
 */
@Service
public class AssetActionHistoryService {

	/**
	 * The Status choices on the Action History form, stored as these exact words.
	 *
	 * Checked on the way in, so the column only ever holds one of them — a typo or an old
	 * client's number would otherwise sit in the log forever and never match a report filter.
	 */
	public static final List<String> ACTION_STATUSES = List.of(
			"Assigned",
			"User Left",
			"Asset Transferred",
			"Laptop Replaced",
			"User Transferred To Another Plant",
			"Asset Transfer Internally");

	@Autowired
	private AssetActionHistoryRepository repository;

	/** Checked before an entry is written about a machine, and stamped after one is. */
	@Autowired
	private AssetMasterRepository assetMasterRepository;

	/** Every assign / release moves the machine between Open and Assigned. */
	@Autowired
	private AllocationService allocationService;

	/**
	 * One machine's entries, newest first.
	 *
	 * Newest first because the top of a log is the part anyone reads — the most recent move
	 * is what tells you where the machine is now.
	 */
	@Transactional(readOnly = true)
	public List<AssetActionHistoryBO> list(Long assetId) throws ApplicationException {
		if (assetId == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device is required.");
		}
		return repository.findByAssetId(assetId).stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(AssetActionHistory::getId).reversed())
				.map(this::toBO)
				.toList();
	}

	/**
	 * ONE ROW PER EMPLOYEE. Each entry names one holder in the assigned_* columns, with their
	 * own start and end. The new_* columns are no longer written: a handover is two rows — the
	 * previous holder's, closed, and the new holder's, open.
	 *
	 * So saving a new holder CLOSES whoever still has the machine open: their end date becomes
	 * the new holder's start date. Without that, two people would both show as holding it.
	 */
	@Transactional
	public AssetActionHistoryBO save(AssetActionHistoryBO in) throws ApplicationException {
		Long assetId = requireAsset(in);

		AssetActionHistory row = new AssetActionHistory();
		row.setAssetId(assetId);
		apply(row, in);

		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

		closeOpenHolders(assetId, row);

		AssetActionHistory saved = repository.save(row);
		stampMaster(assetId);
		return toBO(saved);
	}

	/**
	 * Ends every period on this machine that is still open and began on or before the new
	 * holder's start — the machine cannot be with two people at once. A period that starts
	 * AFTER the new entry is left alone: the new entry is history being typed in after the fact,
	 * not a handover from that later holder.
	 */
	private void closeOpenHolders(Long assetId, AssetActionHistory incoming) {
		LocalDate from = incoming.getStartDate();
		boolean namesHolder = trimToNull(incoming.getAssignedEmpCode()) != null
				|| trimToNull(incoming.getAssignedEmpName()) != null;
		if (!namesHolder || from == null) {
			return;
		}
		for (AssetActionHistory open : repository.findByAssetId(assetId)) {
			if (open.getStatus() == Constants.STATUS_DELETED) {
				continue;
			}
			if (open.getEndDate() == null && open.getStartDate() != null
					&& !open.getStartDate().isAfter(from)) {
				open.setEndDate(from);
				repository.save(open);
			}
		}
	}

	@Transactional
	public AssetActionHistoryBO update(AssetActionHistoryBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		AssetActionHistory row = repository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That history entry no longer exists."));

		apply(row, in);
		AssetActionHistory saved = repository.save(row);
		// A correction can change who the newest entry names, so the summary is redone.
		stampMaster(saved.getAssetId());
		return toBO(saved);
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		AssetActionHistory row = repository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That history entry no longer exists."));

		// Retired, not removed. A log that can be silently emptied is not worth keeping.
		row.setStatus(Constants.STATUS_DELETED);
		repository.save(row);
		// Removing the newest entry has to fall the summary back to the one before it.
		stampMaster(row.getAssetId());
	}

	/**
	 * COPY THE NEWEST ENTRY ONTO THE MACHINE.
	 *
	 * Called after every write here, and nowhere else — these columns are derived, so the one
	 * place that changes the log is the one place that may change the summary of it. Recomputed
	 * from scratch rather than nudged, which is what makes a delete fall correctly back to the
	 * entry before it instead of leaving the removed one stamped on the machine.
	 *
	 * NEWEST MEANS BY START DATE, NOT BY ID. Handovers are often typed in after the fact,
	 * sometimes years after, so the row entered last is not always the move that happened last.
	 * An entry with no date at all sorts to the bottom rather than jumping to the top, and two
	 * moves on one day settle in the order they were recorded.
	 *
	 * The holder is stamped too, so ams_add_device_master.employee_code cannot drift from the
	 * log — except where the newest entry names nobody, when whatever the machine already held
	 * is left alone rather than being blanked.
	 */
	private void stampMaster(Long assetId) {
		AssetMaster asset = assetMasterRepository.findById(assetId).orElse(null);
		if (asset == null) {
			return;
		}

		List<AssetActionHistory> live = repository.findByAssetId(assetId).stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.toList();

		if (live.isEmpty()) {
			// Nothing left to summarise. The holder stays: an imported machine can have one
			// with no log behind it, and clearing it here would lose that.
			asset.setLastActionId(null);
			asset.setLastActionDate(null);
			asset.setLastActionRemarks(null);
			asset.setLastActionOn(null);
			assetMasterRepository.save(asset);
			// Nobody holds it any more: Open (a printer stays Assigned) — see AllocationService.
			allocationService.refresh(asset);
			return;
		}

		AssetActionHistory newest = live.stream()
				.max(Comparator
						.comparing(AssetActionHistory::getStartDate,
								Comparator.nullsFirst(Comparator.naturalOrder()))
						// Same day: the one RECORDED later wins.
						.thenComparing(AssetActionHistory::getCreateDate,
								Comparator.nullsFirst(Comparator.naturalOrder()))
						.thenComparing(AssetActionHistory::getCreateTime,
								Comparator.nullsFirst(Comparator.naturalOrder()))
						.thenComparing(AssetActionHistory::getId))
				.orElseThrow();

		String holderCode = newest.getAssignedEmpCode();
		String holderName = newest.getAssignedEmpName();

		asset.setLastActionId(newest.getId());
		asset.setLastActionDate(newest.getStartDate());
		// Action History no longer records remarks, so there is none to summarise.
		asset.setLastActionRemarks(null);
		asset.setLastActionOn(newest.getCreateDate() == null ? null
				: LocalDateTime.of(newest.getCreateDate(),
						newest.getCreateTime() == null ? LocalTime.MIDNIGHT : newest.getCreateTime()));

		if (trimToNull(holderCode) != null || trimToNull(holderName) != null) {
			asset.setEmployeeCode(trimToNull(holderCode));
			asset.setEmployeeName(trimToNull(holderName));
		}

		assetMasterRepository.save(asset);
		// Assigned while an employee holds it, Open once released — see AllocationService.
		allocationService.refresh(asset);
	}

	/**
	 * One employee, one period.
	 *
	 * A START DATE IS REQUIRED wherever a holder is named: a period with no beginning cannot
	 * be placed on a timeline, and the whole point of this log is being able to say who had
	 * the machine when. THE END DATE IS OPTIONAL, because the current holder has not given it
	 * up yet — that is what a null end date means.
	 *
	 * An end before its start is refused. It is not a period anyone can act on, and it would
	 * quietly break any later attempt to work out how long a machine was held.
	 */
	private void apply(AssetActionHistory row, AssetActionHistoryBO in) throws ApplicationException {
		boolean hasAssigned = trimToNull(in.getAssigned_emp_code()) != null
				|| trimToNull(in.getAssigned_emp_name()) != null;

		if (hasAssigned && in.getStart_date() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Start date is required for the assigned employee.");
		}
		if (in.getStart_date() != null && in.getEnd_date() != null
				&& in.getEnd_date().isBefore(in.getStart_date())) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"End date cannot be before the start date.");
		}

		row.setAssignedEmpCode(trimToNull(in.getAssigned_emp_code()));
		row.setAssignedEmpName(trimToNull(in.getAssigned_emp_name()));
		row.setStartDate(in.getStart_date());
		row.setEndDate(in.getEnd_date());

		// Saved as the chosen words, matched case-insensitively and stored in the list's spelling.
		String actionStatus = trimToNull(in.getAction_status());
		if (actionStatus != null) {
			actionStatus = ACTION_STATUSES.stream()
					.filter(s -> s.equalsIgnoreCase(in.getAction_status().trim()))
					.findFirst()
					.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_DATA,
							"Unknown status. Choose one of: " + String.join(", ", ACTION_STATUSES) + "."));
		}
		row.setActionStatus(actionStatus);

		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	/**
	 * The machine, checked rather than trusted — an entry about a device that does not exist
	 * is a row nobody will ever find again.
	 */
	private Long requireAsset(AssetActionHistoryBO in) throws ApplicationException {
		if (in == null || in.getAsset_id() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Device is required.");
		}
		if (!assetMasterRepository.existsById(in.getAsset_id())) {
			throw new ApplicationException(IResponseCode.INVALID_ENTITY,
					"That device no longer exists.");
		}
		return in.getAsset_id();
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private AssetActionHistoryBO toBO(AssetActionHistory row) {
		AssetActionHistoryBO bo = new AssetActionHistoryBO();
		bo.setId(row.getId());
		bo.setAsset_id(row.getAssetId());
		bo.setAssigned_emp_code(row.getAssignedEmpCode());
		bo.setAssigned_emp_name(row.getAssignedEmpName());
		bo.setStart_date(row.getStartDate());
		bo.setEnd_date(row.getEndDate());
		bo.setAction_status(row.getActionStatus());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
