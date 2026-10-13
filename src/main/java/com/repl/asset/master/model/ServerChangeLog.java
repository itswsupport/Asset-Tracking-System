package com.repl.asset.master.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** THE SERVER USER LOG — one field changed on one server by one UPDATE (ams_server_user_log). */
@Entity
@Table(name = "ams_server_user_log")
public class ServerChangeLog extends ChangeLogBase {

	/** The server, as an id into ams_server_with_ip_master. */
	@Column(name = "server_id", nullable = false)
	private Long serverId;

	@Override
	public Long getRecordId() {
		return serverId;
	}

	@Override
	public void setRecordId(Long recordId) {
		this.serverId = recordId;
	}
}
