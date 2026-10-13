package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.AssetChangeLog;

@Repository
public interface AssetChangeLogRepository extends JpaRepository<AssetChangeLog, Long> {

	/** One machine's changes, newest first. */
	List<AssetChangeLog> findByAssetIdOrderByIdDesc(Long assetId);
}
