package com.repl.asset.master.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One db_all_master.plant_dept_mst row as the Department Master screen shows it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("department")
	private String department;

	@JsonProperty("dept_short_name")
	private String dept_short_name;

	@JsonProperty("plant_name")
	private String plant_name;

	@JsonProperty("plant_code")
	private String plant_code;

	@JsonProperty("company_id")
	private Long company_id;

	/** Resolved from plant_mst by company_id; plant_dept_mst holds only the id. */
	@JsonProperty("company_name")
	private String company_name;

	public Long getCompany_id() {
		return company_id;
	}

	public void setCompany_id(Long company_id) {
		this.company_id = company_id;
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getDept_short_name() {
		return dept_short_name;
	}

	public void setDept_short_name(String dept_short_name) {
		this.dept_short_name = dept_short_name;
	}

	public String getPlant_name() {
		return plant_name;
	}

	public void setPlant_name(String plant_name) {
		this.plant_name = plant_name;
	}
}
