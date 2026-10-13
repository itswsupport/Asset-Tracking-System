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
 * One software licence.
 *
 * Implements SpecEntity, so AbstractSpecService supplies the CRUD — the same duplicate
 * check, status clamping and retire-instead-of-delete the RAM and Warranty masters use.
 *
 * license_name is UNIQUE. create_date and create_time are stored but not shown on screen;
 * status 0 keeps a product on the master screen, so it can be switched back on, while
 * hiding it from anywhere that offers products.
 */
@Entity
@Table(name = "ams_license_master")
public class LicenseMaster implements SpecEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "license_name", nullable = false, length = 150)
	private String licenseName;

	/** A free-text note beside the name. */
	@Column(name = "description", length = 150)
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
		return licenseName;
	}

	@Override
	public void setName(String name) {
		this.licenseName = name;
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
