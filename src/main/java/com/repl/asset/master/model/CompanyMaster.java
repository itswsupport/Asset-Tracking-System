package com.repl.asset.master.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read-only view of db_all_master.company_mst — the SHARED company/plant master.
 *
 * One row per company AND plant, not one per company: RUCHA ENGINEERS PVT LTD appears many
 * times, once for each of Unit-1 … Unit-14 and the corporate office. That is why the
 * Category form derives its plant list by filtering these rows rather than joining to a
 * separate plant table.
 *
 * catalog, NOT schema. MySQL has no schema/catalog split, so Hibernate ignores
 * @Table(schema=...) and emits a bare "from company_mst", which then resolves against
 * db_asset and fails with "table doesn't exist". Same reason as EmpDetailsMaster.
 *
 * @Immutable with no setters: this table is maintained elsewhere and read by several
 * applications. AMS reads it and never writes it.
 */
@Entity
@Immutable
@Table(name = "company_mst", catalog = "db_all_master")
public class CompanyMaster {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "company_name")
	private String companyName;

	@Column(name = "company_short_name")
	private String companyShortName;

	@Column(name = "plant_code")
	private String plantCode;

	@Column(name = "plant_name")
	private String plantName;

	/** 1 = active. Inactive plants must not reach the dropdowns. */
	@Column(name = "status")
	private Integer status;

	public Long getId() {
		return id;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getCompanyShortName() {
		return companyShortName;
	}

	public String getPlantCode() {
		return plantCode;
	}

	public String getPlantName() {
		return plantName;
	}

	public Integer getStatus() {
		return status;
	}
}
