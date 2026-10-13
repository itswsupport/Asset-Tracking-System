package com.repl.asset.master.bo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One IP address row, in and out.
 *
 * The user fills in ip_name, ip_address, description and status. create_date and create_time
 * are the server's and are ignored on the way in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IpAddressBO {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("ip_name")
	private String ip_name;

	@JsonProperty("ip_address")
	private String ip_address;

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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getIp_name() {
		return ip_name;
	}

	public void setIp_name(String ip_name) {
		this.ip_name = ip_name;
	}

	public String getIp_address() {
		return ip_address;
	}

	public void setIp_address(String ip_address) {
		this.ip_address = ip_address;
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
