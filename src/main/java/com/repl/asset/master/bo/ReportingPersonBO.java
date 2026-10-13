package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One reporting-person row, in and out.
 *
 * Company, plant, device types, employee and role. device_names rides along on the way out
 * so a listing needs no extra lookups; on the way in only device_ids matters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportingPersonBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("company_name")
	private String company_name;

	@JsonProperty("plant_code")
	private String plant_code;

	/** The device types this person handles, as Device Master ids — one or several. */
	@JsonProperty("device_ids")
	private List<Long> device_ids;

	/** Their names, "Laptop, Desktop", resolved on the way out for the table. */
	@JsonProperty("device_names")
	private String device_names;

	@JsonProperty("emp_code")
	private Long emp_code;

	@JsonProperty("emp_name")
	private String emp_name;

	@JsonProperty("role")
	private String role;

	/** yyyy-MM-dd and HH:mm, so the UI can print them without parsing anything. */
	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

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

	public String getCompany_name() {
		return company_name;
	}

	public void setCompany_name(String company_name) {
		this.company_name = company_name;
	}

	public String getPlant_code() {
		return plant_code;
	}

	public void setPlant_code(String plant_code) {
		this.plant_code = plant_code;
	}

	public List<Long> getDevice_ids() {
		return device_ids;
	}

	public void setDevice_ids(List<Long> device_ids) {
		this.device_ids = device_ids;
	}

	public String getDevice_names() {
		return device_names;
	}

	public void setDevice_names(String device_names) {
		this.device_names = device_names;
	}

	public Long getEmp_code() {
		return emp_code;
	}

	public void setEmp_code(Long emp_code) {
		this.emp_code = emp_code;
	}

	public String getEmp_name() {
		return emp_name;
	}

	public void setEmp_name(String emp_name) {
		this.emp_name = emp_name;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
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
}
