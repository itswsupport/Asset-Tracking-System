package com.repl.asset.master.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only view of db_all_master.plant_mst, mapped only for its company columns.
 *
 * plant_dept_mst records a company only as company_id (1, 2, 3); this is the table that
 * names those ids. catalog, not schema — same reason as CompanyMaster.
 */
@Entity
@Immutable
@Table(name = "plant_mst", catalog = "db_all_master")
public class PlantMaster {

	@Id
	@Column(name = "plant_id")
	private Long plantId;

	@Column(name = "company_id")
	private Long companyId;

	@Column(name = "company_name")
	private String companyName;

	/** BIGINT in plant_mst, e.g. 1001. */
	@Column(name = "plant_code")
	private Long plantCode;

	@Column(name = "plant_name")
	private String plantName;

	public Long getPlantCode() {
		return plantCode;
	}

	public String getPlantName() {
		return plantName;
	}

	public Long getPlantId() {
		return plantId;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public String getCompanyName() {
		return companyName;
	}
}
