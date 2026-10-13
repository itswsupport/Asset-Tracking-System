package com.repl.asset.master.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One company/plant row as the Category form needs it.
 *
 * A BO rather than the entity itself: company_mst also carries bank account numbers, IFSC
 * codes and a corporate id, none of which the dropdowns need and none of which should be
 * serialised to a browser.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("company_name")
	private String company_name;

	@JsonProperty("company_short_name")
	private String company_short_name;

	@JsonProperty("plant_code")
	private String plant_code;

	@JsonProperty("plant_name")
	private String plant_name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCompany_name() {
		return company_name;
	}

	public void setCompany_name(String company_name) {
		this.company_name = company_name;
	}

	public String getCompany_short_name() {
		return company_short_name;
	}

	public void setCompany_short_name(String company_short_name) {
		this.company_short_name = company_short_name;
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
}
