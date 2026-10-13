package com.repl.asset.master.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only view of db_all_master.plant_dept_mst — the SHARED plant-wise department master.
 *
 * catalog, NOT schema, for the same reason as CompanyMaster: MySQL has no schema/catalog
 * split, so @Table(schema=...) would be ignored and the query would run against db_asset.
 *
 * Only the columns the Department Master screen shows are mapped. The table is maintained
 * elsewhere; AMS reads it and never writes it.
 */
@Entity
@Immutable
@Table(name = "plant_dept_mst", catalog = "db_all_master")
public class PlantDeptMaster {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "department")
	private String department;

	@Column(name = "dept_short_name")
	private String deptShortName;

	@Column(name = "plant_name")
	private String plantName;

	@Column(name = "plant_code")
	private String plantCode;

	/** 1, 2, 3 — named in plant_mst.company_id / company_name. */
	@Column(name = "company_id")
	private Long companyId;

	public Long getCompanyId() {
		return companyId;
	}

	public String getPlantCode() {
		return plantCode;
	}

	public Long getId() {
		return id;
	}

	public String getDepartment() {
		return department;
	}

	public String getDeptShortName() {
		return deptShortName;
	}

	public String getPlantName() {
		return plantName;
	}
}
