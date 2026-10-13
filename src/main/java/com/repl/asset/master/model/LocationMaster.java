package com.repl.asset.master.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * db_all_master.location_master — a replica of db_ems_version2.sf_loc_mst's columns, loaded
 * from its CSV export. db_ems_version2 itself is never read or written by AMS.
 *
 * plant_id holds the PLANT CODE (1001, 1041 …), not plant_mst.plant_id; the company and plant
 * names are looked up from plant_mst by that code.
 *
 * catalog, not schema: MySQL has no schema/catalog split, so @Table(schema=...) would be
 * ignored and the query would run against db_asset.
 */
@Entity
@Table(name = "location_master", catalog = "db_all_master")
public class LocationMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "location_name", length = 255)
	private String locationName;

	@Column(name = "reg_by")
	private Long regBy;

	@Column(name = "plant_id")
	private Integer plantId;

	@Column(name = "location_name_hindi", length = 255)
	private String locationNameHindi;

	public Integer getId() {
		return id;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Long getRegBy() {
		return regBy;
	}

	public void setRegBy(Long regBy) {
		this.regBy = regBy;
	}

	public Integer getPlantId() {
		return plantId;
	}

	public void setPlantId(Integer plantId) {
		this.plantId = plantId;
	}

	public String getLocationNameHindi() {
		return locationNameHindi;
	}

	public void setLocationNameHindi(String locationNameHindi) {
		this.locationNameHindi = locationNameHindi;
	}
}
