package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.repl.asset.master.bo.AssetChangeLogBO;
import com.repl.asset.master.model.ChangeLogBase;

/**
 * THE USER LOG, shared by servers and software — the same rule as the device log
 * (AssetService.logChanges): compare every logged field before and after an UPDATE and write one
 * row per field that changed, with who saved it and when. A field left as it was writes nothing.
 * An empty "before" reads "Not set", an emptied "after" reads "Removed".
 */
final class ChangeLogSupport {

	private ChangeLogSupport() {
	}

	/** The rows to save for one UPDATE — empty when nothing changed. */
	static <T extends ChangeLogBase> List<T> changes(Long recordId, Map<String, String> before,
			Map<String, String> after, String changedByCode, String changedByName, Supplier<T> newRow) {
		LocalDate today = LocalDate.now();
		LocalTime now = LocalTime.now().withNano(0);
		List<T> rows = new ArrayList<>();
		after.forEach((field, newValue) -> {
			String oldValue = before.get(field);
			if (Objects.equals(oldValue, newValue)) {
				return;
			}
			T row = newRow.get();
			row.setRecordId(recordId);
			row.setFieldName(field);
			row.setOldValue(oldValue == null ? "Not set" : oldValue);
			row.setNewValue(newValue == null ? "Removed" : newValue);
			row.setChangedByCode(blankToNull(changedByCode));
			row.setChangedByName(blankToNull(changedByName));
			row.setChangeDate(today);
			row.setChangeTime(now);
			rows.add(row);
		});
		return rows;
	}

	/** One log row, out to the USER LOG table (the same shape as the device log). */
	static AssetChangeLogBO toBO(ChangeLogBase c) {
		AssetChangeLogBO bo = new AssetChangeLogBO();
		bo.setId(c.getId());
		bo.setAsset_id(c.getRecordId());
		bo.setField_name(c.getFieldName());
		bo.setOld_value(c.getOldValue());
		bo.setNew_value(c.getNewValue());
		bo.setChanged_by_code(c.getChangedByCode());
		bo.setChanged_by_name(c.getChangedByName());
		bo.setChange_date(c.getChangeDate());
		bo.setChange_time(c.getChangeTime());
		return bo;
	}

	/** Blank is null, so "" and a missing value never count as a change. */
	static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
