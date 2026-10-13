package com.repl.asset.master.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * THE USER LOG ROW shared by the server and software logs (ams_server_user_log,
 * ams_software_user_log) — the same columns as the device log (ams_asset_user_log): one field
 * changed by one UPDATE, its value before and after, who changed it and when. Each subclass
 * adds the id of the record it belongs to. Written on UPDATE only; never edited or deleted.
 *
 * Values are stored as the text the screen shows (dates DD/MM/YYYY, status Active/Inactive).
 */
@MappedSuperclass
public abstract class ChangeLogBase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** The field's label as on the form — "RAM", "Plant", "Renewal Date". */
	@Column(name = "field_name", length = 100)
	private String fieldName;

	@Column(name = "old_value", length = 500)
	private String oldValue;

	@Column(name = "new_value", length = 500)
	private String newValue;

	/** Who made the change — their employee code and name (employee_code / employee_name). */
	@Column(name = "employee_code", length = 50)
	private String changedByCode;

	@Column(name = "employee_name", length = 150)
	private String changedByName;

	@Column(name = "change_date")
	private LocalDate changeDate;

	@Column(name = "change_time")
	private LocalTime changeTime;

	/** The id of the server / software this change belongs to. */
	public abstract Long getRecordId();

	public abstract void setRecordId(Long recordId);

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getChangedByCode() {
		return changedByCode;
	}

	public void setChangedByCode(String changedByCode) {
		this.changedByCode = changedByCode;
	}

	public String getChangedByName() {
		return changedByName;
	}

	public void setChangedByName(String changedByName) {
		this.changedByName = changedByName;
	}

	public LocalDate getChangeDate() {
		return changeDate;
	}

	public void setChangeDate(LocalDate changeDate) {
		this.changeDate = changeDate;
	}

	public LocalTime getChangeTime() {
		return changeTime;
	}

	public void setChangeTime(LocalTime changeTime) {
		this.changeTime = changeTime;
	}
}
