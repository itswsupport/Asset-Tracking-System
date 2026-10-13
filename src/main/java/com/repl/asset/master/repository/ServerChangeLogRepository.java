package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.ServerChangeLog;

@Repository
public interface ServerChangeLogRepository extends JpaRepository<ServerChangeLog, Long> {

	/** One server's changes, newest first. */
	List<ServerChangeLog> findByServerIdOrderByIdDesc(Long serverId);
}
