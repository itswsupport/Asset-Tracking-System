package com.repl.asset.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.BulkUploadLog;

@Repository
public interface BulkUploadLogRepository extends JpaRepository<BulkUploadLog, Long> {

	/** The earlier import of this very sheet, if there was one. */
	Optional<BulkUploadLog> findFirstByUploadTypeAndFileHash(String uploadType, String fileHash);

	/** One register's uploaded sheets, newest first — for the list of every row uploaded. */
	List<BulkUploadLog> findByUploadTypeOrderByIdDesc(String uploadType);
}
