package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One changed field, out to the CHANGE HISTORY table on the device's View / Edit page. */
public class AssetChangeLogBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("asset_id")
	private Long asset_id;

	@JsonProperty("field_name")
	private String field_name;

	@JsonProperty("old_value")
	private String old_value;

	@JsonProperty("new_value")
	private String new_value;

	@JsonProperty("changed_by_code")
	private String changed_by_code;

	@JsonProperty("changed_by_name")
	private String changed_by_name;

	@JsonProperty("change_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate change_date;

	@JsonProperty("change_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime change_time;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAsset_id() {
		return asset_id;
	}

	public void setAsset_id(Long asset_id) {
		this.asset_id = asset_id;
	}

	public String getField_name() {
		return field_name;
	}

	public void setField_name(String field_name) {
		this.field_name = field_name;
	}

	public String getOld_value() {
		return old_value;
	}

	public void setOld_value(String old_value) {
		this.old_value = old_value;
	}

	public String getNew_value() {
		return new_value;
	}

	public void setNew_value(String new_value) {
		this.new_value = new_value;
	}

	public String getChanged_by_code() {
		return changed_by_code;
	}

	public void setChanged_by_code(String changed_by_code) {
		this.changed_by_code = changed_by_code;
	}

	public String getChanged_by_name() {
		return changed_by_name;
	}

	public void setChanged_by_name(String changed_by_name) {
		this.changed_by_name = changed_by_name;
	}

	public LocalDate getChange_date() {
		return change_date;
	}

	public void setChange_date(LocalDate change_date) {
		this.change_date = change_date;
	}

	public LocalTime getChange_time() {
		return change_time;
	}

	public void setChange_time(LocalTime change_time) {
		this.change_time = change_time;
	}
}
