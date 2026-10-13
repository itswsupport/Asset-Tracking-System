package com.repl.asset.master.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * THE USER LOG — one field changed on one machine by one EDIT: its value before and after,
 * who changed it, and when. Stored in ams_asset_user_log. Written by AssetService.update();
 * never edited or deleted, so a machine's record of what changed over the years can be
 * trusted.
 *
 * Values are stored as the text the screen shows (dates DD/MM/YYYY, status Active/Inactive),
 * so the log reads the same however the underlying columns change later.
 */
@Entity
@Table(name = "ams_asset_user_log")
public class AssetChangeLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** The machine, as an id into ams_add_device_master. */
	@Column(name = "asset_id", nullable = false)
	private Long assetId;

	/** The field's label as on the form — "RAM", "Plant", "External HDD". */
	@Column(name = "field_name", length = 100)
	private String fieldName;

	@Column(name = "old_value", length = 500)
	private String oldValue;

	@Column(name = "new_value", length = 500)
	private String newValue;

	@Column(name = "changed_by_code", length = 50)
	private String changedByCode;

	@Column(name = "changed_by_name", length = 150)
	private String changedByName;

	@Column(name = "change_date")
	private LocalDate changeDate;

	@Column(name = "change_time")
	private LocalTime changeTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAssetId() {
		return assetId;
	}

	public void setAssetId(Long assetId) {
		this.assetId = assetId;
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
