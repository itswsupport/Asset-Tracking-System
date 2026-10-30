package com.repl.asset.master.bo;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One row of the role master, as the Role Master and Login Access screens use it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleMasterBO {

	@JsonProperty("id")
	private Long id;

	/** Made from the name when the role is added — fixed after that. */
	@JsonProperty("role_code")
	private String role_code;

	@JsonProperty("role_name")
	private String role_name;

	/** ADMIN, USER or VIEWER — what the role may do. Chosen when added, fixed after. */
	@JsonProperty("access_type")
	private String access_type;

	/** Admin, User, Viewer: cannot be deleted. Set on the way out only. */
	@JsonProperty("built_in")
	private Boolean built_in;

	@JsonProperty("description")
	private String description;

	@JsonProperty("create_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate create_date;

	@JsonProperty("status")
	private Integer status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRole_code() {
		return role_code;
	}

	public void setRole_code(String role_code) {
		this.role_code = role_code;
	}

	public String getRole_name() {
		return role_name;
	}

	public void setRole_name(String role_name) {
		this.role_name = role_name;
	}

	public String getAccess_type() {
		return access_type;
	}

	public void setAccess_type(String access_type) {
		this.access_type = access_type;
	}

	public Boolean getBuilt_in() {
		return built_in;
	}

	public void setBuilt_in(Boolean built_in) {
		this.built_in = built_in;
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
