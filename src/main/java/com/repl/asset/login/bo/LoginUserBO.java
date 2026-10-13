package com.repl.asset.login.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What /login puts in BaseResponseBO.response.
 *
 * The field names are chosen to match what the frontend already reads — AuthContext and
 * app-sidebar.jsx use user_id / userFname / role, and mockAuth.js returns exactly this
 * shape. Renaming anything here silently blanks the sidebar rather than erroring.
 *
 * emp_pass is deliberately absent. The master row carries a BCrypt hash and this object is
 * serialised straight to the browser.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginUserBO {

	/** emp_code as a string — the frontend treats user_id as an opaque identifier. */
	@JsonProperty("user_id")
	private String user_id;

	@JsonProperty("emp_code")
	private Long emp_code;

	@JsonProperty("userFname")
	private String userFname;

	@JsonProperty("userMname")
	private String userMname;

	@JsonProperty("userLname")
	private String userLname;

	@JsonProperty("displayName")
	private String displayName;

	/** ADMIN / USER / VIEWER, from ams_login_access — never from the master. */
	@JsonProperty("role")
	private String role;

	/** Free-text designation from the master (ENGINEER, SR OFFICER, MD). Display only. */
	@JsonProperty("designation")
	private String designation;

	@JsonProperty("department")
	private String department;

	@JsonProperty("email")
	private String email;

	@JsonProperty("mobile")
	private String mobile;

	@JsonProperty("grade")
	private String grade;

	@JsonProperty("companyName")
	private String companyName;

	/*
	 * THE USER'S SCOPE, from their Reporting Person row (ams_login_access). A USER adds and
	 * sees only these device types, in this plant; ADMIN and VIEWER are not limited by them.
	 */
	@JsonProperty("plant_code")
	private String plant_code;

	@JsonProperty("plant_company")
	private String plant_company;

	/** Their plant(s) as a list — one or several; plant_code holds the same, comma-separated. */
	@JsonProperty("plant_codes")
	private java.util.List<String> plant_codes;

	public java.util.List<String> getPlant_codes() {
		return plant_codes;
	}

	public void setPlant_codes(java.util.List<String> plant_codes) {
		this.plant_codes = plant_codes;
	}

	@JsonProperty("device_ids")
	private java.util.List<Long> device_ids;

	public String getPlant_code() {
		return plant_code;
	}

	public void setPlant_code(String plant_code) {
		this.plant_code = plant_code;
	}

	public String getPlant_company() {
		return plant_company;
	}

	public void setPlant_company(String plant_company) {
		this.plant_company = plant_company;
	}

	public java.util.List<Long> getDevice_ids() {
		return device_ids;
	}

	public void setDevice_ids(java.util.List<Long> device_ids) {
		this.device_ids = device_ids;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	public Long getEmp_code() {
		return emp_code;
	}

	public void setEmp_code(Long emp_code) {
		this.emp_code = emp_code;
	}

	public String getUserFname() {
		return userFname;
	}

	public void setUserFname(String userFname) {
		this.userFname = userFname;
	}

	public String getUserMname() {
		return userMname;
	}

	public void setUserMname(String userMname) {
		this.userMname = userMname;
	}

	public String getUserLname() {
		return userLname;
	}

	public void setUserLname(String userLname) {
		this.userLname = userLname;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
}
