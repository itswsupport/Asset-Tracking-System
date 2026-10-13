package com.repl.asset.master.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One server: a name, a description and a status. Nothing else.
 *
 * The detail imported from the SERVER sheet — IP address, plant, department, processor, RAM,
 * disks, OS, owner, warranty — lives in ams_server_with_ip_master, a separate table. Keeping
 * them apart means this master stays the same shape as every other one, and a save here can
 * never reach the imported columns.
 *
 * Implements SpecEntity, so AbstractSpecService supplies the CRUD.
 *
 * server_name is UNIQUE. create_date and create_time are stored but not shown on screen.
 */
@Entity
@Table(name = "ams_server_master")
public class ServerMaster implements SpecEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "server_name", nullable = false, length = 150)
	private String serverName;

	/** A free-text note beside the name. */
	@Column(name = "description", length = 150)
	private String description;

	@Column(name = "create_date")
	private LocalDate createDate;

	@Column(name = "create_time")
	private LocalTime createTime;

	/** 1 = active, 0 = inactive, -1 = deleted. */
	@Column(name = "status", nullable = false)
	private int status;

	@Column(name = "reg_by")
	private Long regBy;

	@Column(name = "reg_time")
	private String regTime;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getName() {
		return serverName;
	}

	@Override
	public void setName(String name) {
		this.serverName = name;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public LocalDate getCreateDate() {
		return createDate;
	}

	@Override
	public void setCreateDate(LocalDate createDate) {
		this.createDate = createDate;
	}

	@Override
	public LocalTime getCreateTime() {
		return createTime;
	}

	@Override
	public void setCreateTime(LocalTime createTime) {
		this.createTime = createTime;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Long getRegBy() {
		return regBy;
	}

	@Override
	public void setRegBy(Long regBy) {
		this.regBy = regBy;
	}

	public String getRegTime() {
		return regTime;
	}

	public void setRegTime(String regTime) {
		this.regTime = regTime;
	}
}
