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
 * One product's licence position — how many were bought, how many over or short, what kind of
 * licence it is and when it renews.
 *
 * product_name is NOT unique: the same product may be added more than once (indexed for
 * lookups, idx_software_product). It is stored as text rather than an id into ams_product_master because it is what the sheet this
 * came from holds, and the product master is a list of names with nothing else to point at.
 *
 * license_excess_deficiency is SIGNED on purpose. Negative means short of what is needed,
 * which is the number worth acting on; an unsigned column would silently turn a deficiency
 * into a surplus.
 */
@Entity
@Table(name = "ams_software_master")
public class SoftwareMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** Product Name — from the product master. */
	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	/** Total Licenses Acquired. */
	@Column(name = "total_licenses_acquired")
	private Integer totalLicensesAcquired;

	/** License Excess or Deficiency. Negative is a deficiency. */
	@Column(name = "license_excess_deficiency")
	private Integer licenseExcessDeficiency;

	/** License Type — from the license master. */
	@Column(name = "license_type", length = 100)
	private String licenseType;

	/** Renewal Date. */
	@Column(name = "renewal_date")
	private LocalDate renewalDate;

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

	/** How the row was added: "FORM" (ADD SOFTWARE) or "BULK" (Bulk Upload). */
	@Column(name = "added_via", nullable = false, length = 10)
	private String addedVia = "FORM";

	public String getAddedVia() {
		return addedVia;
	}

	public void setAddedVia(String addedVia) {
		this.addedVia = addedVia;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getTotalLicensesAcquired() {
		return totalLicensesAcquired;
	}

	public void setTotalLicensesAcquired(Integer totalLicensesAcquired) {
		this.totalLicensesAcquired = totalLicensesAcquired;
	}

	public Integer getLicenseExcessDeficiency() {
		return licenseExcessDeficiency;
	}

	public void setLicenseExcessDeficiency(Integer licenseExcessDeficiency) {
		this.licenseExcessDeficiency = licenseExcessDeficiency;
	}

	public String getLicenseType() {
		return licenseType;
	}

	public void setLicenseType(String licenseType) {
		this.licenseType = licenseType;
	}

	public LocalDate getRenewalDate() {
		return renewalDate;
	}

	public void setRenewalDate(LocalDate renewalDate) {
		this.renewalDate = renewalDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
