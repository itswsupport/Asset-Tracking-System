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
 * Every tablet, by the number written on it.
 *
 * Its own table and its own entity, not one shared device table keyed by a type column. Each
 * device type gets its own stack for the same reason RAM, Processor, HDD and OS each do: a
 * change to laptops cannot then reach desktops, and each list has its own endpoints.
 *
 * Implements SpecEntity because the row is one name plus the usual status and stamps, which
 * is exactly what AbstractSpecService already handles — so the duplicate check, the status
 * clamping and retire-instead-of-delete come from there rather than being copied again.
 *
 * tablet_number is UNIQUE. Loaded from the TABLET sheet of the asset workbook.
 */
@Entity
@Table(name = "ams_tablet_master")
public class TabletMaster implements SpecEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "tablet_number", nullable = false, length = 100)
	private String tabletNumber;

	/** From the asset workbook. Null where the sheet left the cell blank. */
	@Column(name = "system_type", length = 50)
	private String systemType;

	/** From the asset workbook. Age is NOT stored — it is derived from these two. */
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

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getName() {
		return tabletNumber;
	}

	@Override
	public void setName(String name) {
		this.tabletNumber = name;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public LocalDate getCreateDate() {
		return createDate;
	}

	@Override
	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	@Override
	public LocalTime getCreateTime() {
		return createTime;
	}

	@Override
	public void setCreateTime(LocalTime createTime) {
		this.createTime = createTime;
	}

	@Override
	public String getSystemType() {
		return systemType;
	}

	@Override
	public void setSystemType(String systemType) {
		this.systemType = systemType;
	}

	@Override
	public String getWarrantyStatus() {
		return warrantyStatus;
	}

	@Override
	public void setWarrantyStatus(String warrantyStatus) {
		this.warrantyStatus = warrantyStatus;
	}

	@Override
	public LocalDate getLastDateOfWarranty() {
		return lastDateOfWarranty;
	}

	@Override
	public void setLastDateOfWarranty(LocalDate lastDateOfWarranty) {
		this.lastDateOfWarranty = lastDateOfWarranty;
	}

	@Override
	public BigDecimal getTotalWarrantyYears() {
		return totalWarrantyYears;
	}

	@Override
	public void setTotalWarrantyYears(BigDecimal totalWarrantyYears) {
		this.totalWarrantyYears = totalWarrantyYears;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Long getRegBy() {
		return regBy;
	}

	@Override
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
