package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.SoftwareChangeLog;

@Repository
public interface SoftwareChangeLogRepository extends JpaRepository<SoftwareChangeLog, Long> {

	/** One software entry's changes, newest first. */
	List<SoftwareChangeLog> findBySoftwareIdOrderByIdDesc(Long softwareId);
}
