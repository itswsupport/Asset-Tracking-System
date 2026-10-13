package com.repl.asset.master.bo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of any of the four hardware spec masters.
 *
 * Deliberately generic — "name" rather than ram_name / processor_name — so the same BO,
 * the same endpoints and the same frontend panel serve all four lists.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("name")
	private String name;

	/** Serialised as yyyy-MM-dd rather than an array, which is Jackson's default for a date. */
	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	/** HH:mm. The column keeps seconds; nothing on screen needs them. */
	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

	/**
	 * Only the device masters set this; NON_NULL keeps it out of the RAM/HDD/OS responses.
	 * It is what lets the Warranty form fill System Type in from the device chosen.
	 */
	@JsonProperty("system_type")
	private String system_type;

	/** The device's own warranty details, from the asset workbook. Device masters only. */
	@JsonProperty("warranty_status")
	private String warranty_status;

	@JsonProperty("last_date_of_warranty")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate last_date_of_warranty;

	@JsonProperty("total_warranty_years")
	private BigDecimal total_warranty_years;

	/** A free-text note beside the name. */
	@JsonProperty("description")
	private String description;

	@JsonProperty("status")
	private Integer status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public void setLast_date_of_warranty(LocalDate last_date_of_warranty) {
		this.last_date_of_warranty = last_date_of_warranty;
	}

	public BigDecimal getTotal_warranty_years() {
		return total_warranty_years;
	}

	public void setTotal_warranty_years(BigDecimal total_warranty_years) {
		this.total_warranty_years = total_warranty_years;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
