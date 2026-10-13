package com.repl.asset.master.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One db_all_master.location_master row, as the Location Master screen sends and reads it.
 *
 * plant_code is what location_master stores in plant_id. company_name and plant_name are out
 * only, looked up from plant_mst.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationBO {

	@JsonProperty("id")
	private Integer id;

	@JsonProperty("company_name")
	private String company_name;

	@JsonProperty("plant_code")
	private String plant_code;

	@JsonProperty("plant_name")
	private String plant_name;

	@JsonProperty("location_name")
	private String location_name;

	@JsonProperty("location_name_hindi")
	private String location_name_hindi;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCompany_name() {
		return company_name;
	}

	public void setCompany_name(String company_name) {
		this.company_name = company_name;
	}

	public String getPlant_code() {
		return plant_code;
	}

	public void setPlant_code(String plant_code) {
		this.plant_code = plant_code;
	}

	public String getPlant_name() {
		return plant_name;
	}

	public void setPlant_name(String plant_name) {
		this.plant_name = plant_name;
	}

	public String getLocation_name() {
		return location_name;
	}

	public void setLocation_name(String location_name) {
		this.location_name = location_name;
	}

	public String getLocation_name_hindi() {
		return location_name_hindi;
	}

	public void setLocation_name_hindi(String location_name_hindi) {
		this.location_name_hindi = location_name_hindi;
	}
}
