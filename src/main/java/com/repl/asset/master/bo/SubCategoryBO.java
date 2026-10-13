package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One sub category row, in and out.
 *
 * category_name rides along on the way out so a listing does not need a second lookup; on
 * the way in only category_id matters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubCategoryBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("category_id")
	private Long category_id;

	@JsonProperty("category_name")
	private String category_name;

	@JsonProperty("sub_category_name")
	private String sub_category_name;

	/** yyyy-MM-dd and HH:mm, so the UI can print them without parsing anything. */
	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("create_time")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime create_time;

	@JsonProperty("description")
	private String description;

	@JsonProperty("status")
	private Integer status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCategory_id() {
		return category_id;
	}

	public void setCategory_id(Long category_id) {
		this.category_id = category_id;
	}

	public String getCategory_name() {
		return category_name;
	}

	public void setCategory_name(String category_name) {
		this.category_name = category_name;
	}

	public String getSub_category_name() {
		return sub_category_name;
	}

	public void setSub_category_name(String sub_category_name) {
		this.sub_category_name = sub_category_name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
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
}
