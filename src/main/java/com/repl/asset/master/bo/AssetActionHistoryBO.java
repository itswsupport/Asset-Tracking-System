package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One history entry, in and out — one employee and their period with the machine.
 *
 * asset_id says which machine it belongs to and is required. Any extra field the client sends
 * (new_emp_code from an old page, say) is ignored by Jackson's default settings, not saved.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetActionHistoryBO {

	@JsonProperty("id")
	private Long id;

	/** The machine, as an id from the asset register. */
	@JsonProperty("asset_id")
	private Long asset_id;

	/**
	 * Where the machine was when this entry was written. Set by the server from the device —
	 * a value sent by the client is ignored.
	 */
	@JsonProperty("plant")
	private String plant;

	@JsonProperty("location")
	private String location;

	/** Assigned To Employee — the code. */
	@JsonProperty("assigned_emp_code")
	private String assigned_emp_code;

	/** Their name, stored beside the code so the log does not change when a master does. */
	@JsonProperty("assigned_emp_name")
	private String assigned_emp_name;

	@JsonProperty("start_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate start_date;

	/** Null while the machine is still held. */
	@JsonProperty("end_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate end_date;


	/** "User Left", "Asset Transferred", … — the text itself, see AssetActionHistory. */
	@JsonProperty("action_status")
	private String action_status;

	/**
	 * IN: the signed-in employee saving this entry, as ADD DEVICE and BULK UPLOAD send it for a
	 * device (changed_by_*). Recorded as action_by on a new entry.
	 */
	@JsonProperty("changed_by_code")
	private String changed_by_code;

	@JsonProperty("changed_by_name")
	private String changed_by_name;

	/** OUT: who assigned the device (made the entry). Set by the server. */
	@JsonProperty("action_by_emp_code")
	private String action_by_emp_code;

	@JsonProperty("action_by_emp_name")
	private String action_by_emp_name;

	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

	@JsonProperty("status")
	private Integer status;

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

	public String getPlant() {
		return plant;
	}

	public void setPlant(String plant) {
		this.plant = plant;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getAssigned_emp_code() {
		return assigned_emp_code;
	}

	public void setAssigned_emp_code(String assigned_emp_code) {
		this.assigned_emp_code = assigned_emp_code;
	}

	public String getAssigned_emp_name() {
		return assigned_emp_name;
	}

	public void setAssigned_emp_name(String assigned_emp_name) {
		this.assigned_emp_name = assigned_emp_name;
	}

	public LocalDate getStart_date() {
		return start_date;
	}

	public void setStart_date(LocalDate start_date) {
		this.start_date = start_date;
	}

	public LocalDate getEnd_date() {
		return end_date;
	}

	public void setEnd_date(LocalDate end_date) {
		this.end_date = end_date;
	}

	public String getAction_status() {
		return action_status;
	}

	public void setAction_status(String action_status) {
		this.action_status = action_status;
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

	public String getAction_by_emp_code() {
		return action_by_emp_code;
	}

	public void setAction_by_emp_code(String action_by_emp_code) {
		this.action_by_emp_code = action_by_emp_code;
	}

	public String getAction_by_emp_name() {
		return action_by_emp_name;
	}

	public void setAction_by_emp_name(String action_by_emp_name) {
		this.action_by_emp_name = action_by_emp_name;
	}

	public LocalDate getCreate_date() {
		return create_date;
	}

	public void setCreate_date(LocalDate create_date) {
		this.create_date = create_date;
	}

	public LocalTime getCreate_time() {
		return create_time;
	}

	public void setCreate_time(LocalTime create_time) {
		this.create_time = create_time;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
