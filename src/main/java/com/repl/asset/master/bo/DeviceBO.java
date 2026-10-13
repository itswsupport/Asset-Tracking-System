package com.repl.asset.master.bo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One device row, in and out.
 *
 * The admin fills in two things: device_name and status. create_date and
 * create_time are the server's and are ignored on the way in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("device_name")
	private String device_name;

	@JsonProperty("system_type")
	private String system_type;

	@JsonProperty("warranty_status")
	private String warranty_status;

	@JsonProperty("last_date_of_warranty")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate last_date_of_warranty;

	@JsonProperty("total_warranty_years")
	private BigDecimal total_warranty_years;

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

	public String getDevice_name() {
		return device_name;
	}

	public void setDevice_name(String device_name) {
		this.device_name = device_name;
	}

	public String getSystem_type() {
		return system_type;
	}

	public void setSystem_type(String system_type) {
		this.system_type = system_type;
	}

	public String getWarranty_status() {
		return warranty_status;
	}

	public void setWarranty_status(String warranty_status) {
		this.warranty_status = warranty_status;
	}

	public LocalDate getLast_date_of_warranty() {
		return last_date_of_warranty;
	}

	public void setLast_date_of_warranty(LocalDate d) {
		this.last_date_of_warranty = d;
	}

	public BigDecimal getTotal_warranty_years() {
		return total_warranty_years;
	}

	public void setTotal_warranty_years(BigDecimal y) {
		this.total_warranty_years = y;
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
