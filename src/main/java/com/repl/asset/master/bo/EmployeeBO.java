package com.repl.asset.master.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One employee, as the Reporting Person dropdown needs them.
 *
 * A BO rather than the entity: emp_details carries emp_pass (a BCrypt hash), Aadhaar and PAN
 * numbers, bank details and salary fields. None of that belongs in a dropdown, and the hash
 * must never reach a browser.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeBO {

	@JsonProperty("emp_code")
	private Long emp_code;

	/** "GANESH BARATHE" — first + last, ready to display. */
	@JsonProperty("emp_name")
	private String emp_name;

	@JsonProperty("designation")
	private String designation;

	@JsonProperty("department")
	private String department;

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

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}
}
