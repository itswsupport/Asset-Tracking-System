package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One machine as DEVICE STATUS shows it — who has it, or why nobody does. Read only.
 *
 *   state        "ASSIGNED" (a holder with no end date) or "OPEN" (nobody has it)
 *   since        ASSIGNED: when the holder got it. OPEN: when it was released, or for a
 *                machine never assigned, the day it was added
 *   history      that machine's entries, newest first
 */
public class DeviceStatusBO {

	@JsonProperty("id")
	private Long id;

	/**
	 * true: a machine registered through ADD DEVICE (ams_add_device_master).
	 * false: only a name in Device Name Master, never registered or given to anyone — OPEN,
	 * with a negative id, no reference number and no history.
	 */
	@JsonProperty("registered")
	private boolean registered;

	@JsonProperty("reference_no")
	private String reference_no;

	@JsonProperty("name")
	private String name;

	@JsonProperty("model")
	private String model;

	@JsonProperty("asset_code")
	private String asset_code;

	/** The plant code a registered device is at; null for a name not registered yet. */
	@JsonProperty("plant")
	private String plant;

	public String getPlant() {
		return plant;
	}

	public void setPlant(String plant) {
		this.plant = plant;
	}

	@JsonProperty("state")
	private String state;

	/** Id of the holder's open history row — what a release closes. */
	@JsonProperty("holder_entry_id")
	private Long holder_entry_id;

	@JsonProperty("holder_code")
	private String holder_code;

	@JsonProperty("holder_name")
	private String holder_name;

	@JsonProperty("since")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate since;

	@JsonProperty("history")
	private List<AssetActionHistoryBO> history;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public String getReference_no() {
		return reference_no;
	}

	public void setReference_no(String reference_no) {
		this.reference_no = reference_no;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getAsset_code() {
		return asset_code;
	}

	public void setAsset_code(String asset_code) {
		this.asset_code = asset_code;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Long getHolder_entry_id() {
		return holder_entry_id;
	}

	public void setHolder_entry_id(Long holder_entry_id) {
		this.holder_entry_id = holder_entry_id;
	}

	public String getHolder_code() {
		return holder_code;
	}

	public void setHolder_code(String holder_code) {
		this.holder_code = holder_code;
	}

	public String getHolder_name() {
		return holder_name;
	}

	public void setHolder_name(String holder_name) {
		this.holder_name = holder_name;
	}

	public LocalDate getSince() {
		return since;
	}

	public void setSince(LocalDate since) {
		this.since = since;
	}

	public List<AssetActionHistoryBO> getHistory() {
		return history;
	}

	public void setHistory(List<AssetActionHistoryBO> history) {
		this.history = history;
	}
}
