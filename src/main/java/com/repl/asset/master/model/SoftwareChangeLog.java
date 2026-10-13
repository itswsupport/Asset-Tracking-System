package com.repl.asset.master.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** THE SOFTWARE USER LOG — one field changed on one software entry by one UPDATE (ams_software_user_log). */
@Entity
@Table(name = "ams_software_user_log")
public class SoftwareChangeLog extends ChangeLogBase {

	/** The software entry, as an id into ams_software_master. */
	@Column(name = "software_id", nullable = false)
	private Long softwareId;

	@Override
	public Long getRecordId() {
		return softwareId;
	}

	@Override
	public void setRecordId(Long recordId) {
		this.softwareId = recordId;
	}
}
