package com.repl.asset.master.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One KIND of device the hardware team works with — Laptop, Desktop, Printer, Tablet.
 *
 * NOT an individual machine. "Laptop", not "EDC712": the machines themselves live in the
 * per-type masters loaded from the asset workbook (ams_laptop_master and friends).
 *
 * Deliberately just a name and a status. It was briefly a much wider record (make, bits,
 * warranty dates) and was cut back: the register only has to answer "which kinds of device
 * exist, and which of them are still in play".
 *
 * device_name is UNIQUE. status 0 keeps a kind on the admin screen — so it can be switched
 * back on — while hiding it from the select-device dropdown users see.
 */
@Entity
@Table(name = "ams_device_master")
public class DeviceMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "device_name", nullable = false, length = 150)
	private String deviceName;

	/**
	 * Filled in from the LIST DEVICE form on the user side. Nullable, because the admin's
	 * own short form saves a device with only a name and a status.
	 */
	@Column(name = "system_type", length = 50)
	private String systemType;

	@Column(name = "warranty_status", length = 50)
	private String warrantyStatus;

	@Column(name = "last_date_of_warranty")
	private LocalDate lastDateOfWarranty;

	@Column(name = "total_warranty_years", precision = 4, scale = 1)
	private BigDecimal totalWarrantyYears;

	/** A free-text note beside the name. */
	@Column(name = "description", length = 255)
	private String description;

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

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String name) {
		this.deviceName = name;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getSystemType() {
		return systemType;
	}

	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	public String getWarrantyStatus() {
		return warrantyStatus;
	}

	public void setWarrantyStatus(String warrantyStatus) {
		this.warrantyStatus = warrantyStatus;
	}

	public LocalDate getLastDateOfWarranty() {
		return lastDateOfWarranty;
	}

	public void setLastDateOfWarranty(LocalDate d) {
		this.lastDateOfWarranty = d;
	}

	public BigDecimal getTotalWarrantyYears() {
		return totalWarrantyYears;
	}

	public void setTotalWarrantyYears(BigDecimal y) {
		this.totalWarrantyYears = y;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
