package com.repl.asset.login.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * AMS's own access list, in db_asset. This is what decides who may use AMS and with which
 * role — the employee master says who someone IS, this says what they may DO here.
 *
 * The master cannot answer that: its own "role" column reads "Employee" for 979 of the 1009
 * rows, and desig_id is free text maintained by HR, so deriving authority from either would
 * hand out or revoke admin rights on a typo.
 *
 * No row (or status = 0) means no AMS access, even for a perfectly valid employee.
 */
@Entity
@Table(name = "ams_login_access")
public class AmsLoginAccess {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** Matches emp_details.emp_code in the master. Unique — one AMS access row per employee. */
	@Column(name = "emp_code", nullable = false, unique = true)
	private Long empCode;

	/**
	 * Where this person works — company, plant and the device types they handle (device_ids).
	 *
	 * All NULLABLE, and deliberately so. An administrator granted access directly has no
	 * plant or device type to be limited to, and forcing one would mean inventing data. A row with
	 * these filled in came from the Reporting Person screen; a row without them is plain
	 * access.
	 *
	 * emp_name is a denormalised snapshot for display, so listing does not need a join into
	 * db_all_master. emp_code remains the identity.
	 */
	@Column(name = "company_name")
	private String companyName;

	/**
	 * The plant(s) this person works in: one code ("1001") or several separated by commas
	 * ("1001,1003"), as picked on the Reporting Person form. Only the codes are kept — a plant's
	 * name is in the plant master, looked up from the company when needed.
	 */
	@Column(name = "plant_code", length = 255)
	private String plantCode;

	/**
	 * The device types this person may add and see, as ams_device_master ids separated by
	 * commas — "2" for Laptop, "1,2" for Desktop + Laptop. Together with plant_code it is a
	 * USER's whole scope: only these types, only in that plant. Null for plain access.
	 */
	@Column(name = "device_ids", length = 255)
	private String deviceIds;

	@Column(name = "emp_name")
	private String empName;

	/** ADMIN, USER or VIEWER — the exact strings the frontend's ROLES uses. */
	@Column(name = "role", nullable = false, length = 20)
	private String role;

	/** 1 = access enabled, 0 = revoked. */
	/** A free-text note beside the row. */
	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "status", nullable = false)
	private int status;

	@Column(name = "reg_by")
	private Long regBy;

	/**
	 * When this row was created. Alongside reg_date/reg_time rather than replacing them:
	 * those are varchars carried over from the shared base code, these are real DATE and TIME
	 * columns, which is what sorting and filtering on screen need.
	 */
	@Column(name = "create_date")
	private LocalDate createDate;

	@Column(name = "create_time")
	private LocalTime createTime;

	@Column(name = "reg_date")
	private String regDate;

	@Column(name = "reg_time")
	private String regTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getEmpCode() {
		return empCode;
	}

	public void setEmpCode(Long empCode) {
		this.empCode = empCode;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getPlantCode() {
		return plantCode;
	}

	public void setPlantCode(String plantCode) {
		this.plantCode = plantCode;
	}

	/** plant_code as a list of codes ("1001,1003" -> [1001, 1003]); empty when none is set. */
	public java.util.List<String> getPlantCodeList() {
		if (plantCode == null || plantCode.isBlank()) {
			return java.util.List.of();
		}
		return java.util.Arrays.stream(plantCode.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.distinct()
				.toList();
	}

	public String getDeviceIds() {
		return deviceIds;
	}

	public void setDeviceIds(String deviceIds) {
		this.deviceIds = deviceIds;
	}

	/** device_ids as a list of ids; empty when none are set. */
	public java.util.List<Long> getDeviceIdList() {
		if (deviceIds == null || deviceIds.isBlank()) {
			return java.util.List.of();
		}
		return java.util.Arrays.stream(deviceIds.split(","))
				.map(String::trim)
				.filter(s -> s.matches("\\d+"))
				.map(Long::valueOf)
				.distinct()
				.toList();
	}

	public String getEmpName() {
		return empName;
	}

	public void setEmpName(String empName) {
		this.empName = empName;
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

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Long getRegBy() {
		return regBy;
	}

	public void setRegBy(Long regBy) {
		this.regBy = regBy;
	}

	public String getRegDate() {
		return regDate;
	}

	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}

	public String getRegTime() {
		return regTime;
	}

	public void setRegTime(String regTime) {
		this.regTime = regTime;
	}

	public LocalDate getCreateDate() {
		return createDate;
	}

	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	public LocalTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalTime createTime) {
		this.createTime = createTime;
	}
}
