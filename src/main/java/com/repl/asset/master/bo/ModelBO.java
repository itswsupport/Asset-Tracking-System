package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One model row, in and out.
 *
 * The user fills in device_id, model_name, description and status. create_date and create_time
 * are the server's and are ignored on the way in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelBO {

	@JsonProperty("id")
	private Long id;

	/** The chosen device, as an id from the device master. This is what the form sends. */
	@JsonProperty("device_id")
	private Long device_id;

	/** Resolved from device_id on the way out, for the table. Ignored on the way in. */
	@JsonProperty("device_name")
	private String device_name;

	@JsonProperty("model_name")
	private String model_name;

	@JsonProperty("description")
	private String description;

	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

	/** 1 = active, 0 = inactive. -1 is set by delete and never arrives from a form. */
	@JsonProperty("status")
	private Integer status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getDevice_id() {
		return device_id;
	}

	public void setDevice_id(Long device_id) {
		this.device_id = device_id;
	}

	public String getDevice_name() {
		return device_name;
	}

	public void setDevice_name(String device_name) {
		this.device_name = device_name;
	}

	public String getModel_name() {
		return model_name;
	}

	public void setModel_name(String model_name) {
		this.model_name = model_name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
