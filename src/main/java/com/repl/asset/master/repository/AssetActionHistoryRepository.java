package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.AssetActionHistory;

@Repository
public interface AssetActionHistoryRepository extends JpaRepository<AssetActionHistory, Long> {

	/** One machine's entries, deleted ones included — the service filters those. */
	List<AssetActionHistory> findByAssetId(Long assetId);

	/**
	 * Many machines' entries in ONE query, deleted ones included — for DEVICE STATUS, which
	 * shows a whole device type at once and would otherwise ask once per machine.
	 */
	List<AssetActionHistory> findByAssetIdIn(List<Long> assetIds);
}
