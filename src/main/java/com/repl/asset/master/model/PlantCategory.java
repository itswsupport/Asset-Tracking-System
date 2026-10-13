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
 * Which category applies at which company + plant — what the Category Master form saves.
 *
 * The company and plant are stored as their values rather than a foreign key, because those
 * rows live in db_all_master, a different database that other applications maintain. MySQL
 * will not enforce a foreign key across catalogs, and pointing AMS's own table at a schema
 * it does not own would be the wrong dependency anyway.
 *
 * category_id IS a real foreign key: ams_category_mst sits in db_asset alongside this table.
 *
 * (plant_code, category_id) is UNIQUE. The same category can be used at many plants, but not
 * added twice to the same one.
 */
@Entity
@Table(name = "ams_plant_category")
public class PlantCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "company_name", nullable = false)
	private String companyName;

	@Column(name = "plant_code", nullable = false, length = 50)
	private String plantCode;

	/** Denormalised for display, so listing needs no second lookup into the shared master. */
	@Column(name = "plant_name")
	private String plantName;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

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

	public String getPlantName() {
		return plantName;
	}

	public void setPlantName(String plantName) {
		this.plantName = plantName;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
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
