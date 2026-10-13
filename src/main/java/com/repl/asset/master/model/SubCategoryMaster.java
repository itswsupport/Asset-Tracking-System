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
 * Sub categories — Laptop, Desktop, Printer, Chair and so on.
 *
 * Unlike CategoryMaster, this one DOES belong to something: every sub category hangs off
 * exactly one category, which is why the form asks for the category first.
 *
 * category_id is a real foreign key; ams_category_mst sits in db_asset alongside this table.
 *
 * (category_id, sub_category_name) is UNIQUE. "Chair" under ADMIN and "Chair" under HR are
 * legitimately different rows; two "Chair"s under the same category are not.
 */
@Entity
@Table(name = "ams_sub_category_mst")
public class SubCategoryMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "sub_category_name", nullable = false, length = 150)
	private String subCategoryName;

	/** 1 = active, 0 = retired. Retired rather than deleted, so assets already filed under
	 *  a sub category keep a name to point at. */
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

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getSubCategoryName() {
		return subCategoryName;
	}

	public void setSubCategoryName(String subCategoryName) {
		this.subCategoryName = subCategoryName;
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
