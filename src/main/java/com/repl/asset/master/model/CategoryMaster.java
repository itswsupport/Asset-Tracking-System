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
 * The category list — IT, HR, OPERATIONS and whatever gets added later.
 *
 * INDEPENDENT on purpose: a category belongs to no company and no plant. Which plants use a
 * category is recorded separately in PlantCategory; keeping that out of here is what lets a
 * category be named once and used at many plants.
 *
 * Writable, unlike CompanyMaster and EmpDetailsMaster, which are read-only views of the
 * shared db_all_master. No catalog attribute is needed: the datasource already points at
 * db_asset, so an unqualified name resolves there.
 */
@Entity
@Table(name = "ams_category_mst")
public class CategoryMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** UNIQUE in the table — the same category cannot be created twice. */
	@Column(name = "category_name", nullable = false, length = 100)
	private String categoryName;

	/** 1 = active, 0 = retired. Retired rather than deleted, so assets already filed under
	 *  a category keep a name to point at. */
	/** A free-text note beside the name. */
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

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
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
