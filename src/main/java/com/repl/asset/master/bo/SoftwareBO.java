package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One product's licence position, in and out.
 *
 * Only product_name is required. The counts, the licence type and the renewal date are all
 * optional, because a product can be recorded before any of them is known.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SoftwareBO {

	@JsonProperty("id")
	private Long id;

	/** Product Name, from the product master. */
	@JsonProperty("product_name")
	private String product_name;

	/** Total Licenses Acquired. */
	@JsonProperty("total_licenses_acquired")
	private Integer total_licenses_acquired;

	/** License Excess or Deficiency. Negative is a deficiency. */
	@JsonProperty("license_excess_deficiency")
	private Integer license_excess_deficiency;

	/** License Type, from the license master. */
	@JsonProperty("license_type")
	private String license_type;

	/** Renewal Date. */
	@JsonProperty("renewal_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate renewal_date;

	@JsonProperty("description")
	private String description;

	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

	/** 1 = active, 0 = inactive. -1 is set by delete and never arrives from a form. */
	@JsonProperty("status")
	private Integer status;

	/** Who is saving — written beside every field an UPDATE changes (the USER LOG). */
	@JsonProperty("changed_by_code")
	private String changed_by_code;

	@JsonProperty("changed_by_name")
	private String changed_by_name;

	public String getChanged_by_code() {
		return changed_by_code;
	}

	public void setChanged_by_code(String changed_by_code) {
		this.changed_by_code = changed_by_code;
	}

	public String getChanged_by_name() {
		return changed_by_name;
	}

	public void setChanged_by_name(String changed_by_name) {
		this.changed_by_name = changed_by_name;
	}

	/** "FORM" or "BULK" — set by Bulk Upload on every row, otherwise FORM. */
	@JsonProperty("added_via")
	private String added_via;

	public String getAdded_via() {
		return added_via;
	}

	public void setAdded_via(String added_via) {
		this.added_via = added_via;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProduct_name() {
		return product_name;
	}

	public void setProduct_name(String product_name) {
		this.product_name = product_name;
	}

	public Integer getTotal_licenses_acquired() {
		return total_licenses_acquired;
	}

	public void setTotal_licenses_acquired(Integer total_licenses_acquired) {
		this.total_licenses_acquired = total_licenses_acquired;
	}

	public Integer getLicense_excess_deficiency() {
		return license_excess_deficiency;
	}

	public void setLicense_excess_deficiency(Integer license_excess_deficiency) {
		this.license_excess_deficiency = license_excess_deficiency;
	}

	public String getLicense_type() {
		return license_type;
	}

	public void setLicense_type(String license_type) {
		this.license_type = license_type;
	}

	public LocalDate getRenewal_date() {
		return renewal_date;
	}

	public void setRenewal_date(LocalDate renewal_date) {
		this.renewal_date = renewal_date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getCreate_date() {
		return create_date;
	}

	public void setCreate_date(LocalDate create_date) {
		this.create_date = create_date;
	}

	public LocalTime getCreate_time() {
		return create_time;
	}

	public void setCreate_time(LocalTime create_time) {
		this.create_time = create_time;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
