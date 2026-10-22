package com.repl.asset.master.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One entry in a machine's history — ONE EMPLOYEE and the period they held it.
 *
 * A LOG, not a state. The machine's current holder lives on the asset row itself; this is the
 * record of how it got there. A handover is two rows: the previous holder's, closed with an end
 * date, and the new holder's, open. Never two people in one row.
 *
 * The employee NAME is copied in beside the code rather than looked up on read. A log that
 * reads differently a year later because somebody was renamed in the employee master is not a
 * log — it has to say what was true when it was written.
 *
 * end_date is null while the machine is still held. Filling it in is what closes a period.
 */
@Entity
@Table(name = "ams_asset_action_history")
public class AssetActionHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** The machine this entry is about, as an id into ams_add_device_master. */
	@Column(name = "asset_id", nullable = false)
	private Long assetId;

	/**
	 * The machine's PLANT and LOCATION when this entry was written, copied from
	 * ams_add_device_master — not looked up on read. Moving the device to another plant later
	 * must not rewrite where it was while this employee held it.
	 */
	@Column(name = "plant", length = 150)
	private String plant;

	@Column(name = "location", length = 150)
	private String location;

	@Column(name = "assigned_emp_code", length = 50)
	private String assignedEmpCode;

	@Column(name = "assigned_emp_name", length = 150)
	private String assignedEmpName;

	@Column(name = "start_date")
	private LocalDate startDate;

	/** Null while the machine is still held. */
	@Column(name = "end_date")
	private LocalDate endDate;

	/**
	 * What happened, stored as the words chosen — "User Left", "Asset Transferred", … — not
	 * as a number. Not to be confused with `status` below, which is the row's own
	 * active/deleted flag.
	 */
	@Column(name = "action_status", length = 50)
	private String actionStatus;


	@Column(name = "create_date")
	private LocalDate createDate;

	@Column(name = "create_time")
	private LocalTime createTime;

	/** 1 = active, 0 = inactive, -1 = deleted. */
	@Column(name = "status", nullable = false)
	private int status;

	@Column(name = "reg_by")
	private Long regBy;

	@Column(name = "reg_time")
	private String regTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAssetId() {
		return assetId;
	}

	public void setAssetId(Long assetId) {
		this.assetId = assetId;
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

	public String getAssignedEmpCode() {
		return assignedEmpCode;
	}

	public void setAssignedEmpCode(String assignedEmpCode) {
		this.assignedEmpCode = assignedEmpCode;
	}

	public String getAssignedEmpName() {
		return assignedEmpName;
	}

	public void setAssignedEmpName(String assignedEmpName) {
		this.assignedEmpName = assignedEmpName;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public String getActionStatus() {
		return actionStatus;
	}

	public void setActionStatus(String actionStatus) {
		this.actionStatus = actionStatus;
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

	public String getRegTime() {
		return regTime;
	}

	public void setRegTime(String regTime) {
		this.regTime = regTime;
	}
}
