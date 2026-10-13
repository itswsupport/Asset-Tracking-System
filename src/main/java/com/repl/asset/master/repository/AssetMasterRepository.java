package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.AssetMaster;

@Repository
public interface AssetMasterRepository extends JpaRepository<AssetMaster, Long> {

	/**
	 * Every machine of one device type, deleted ones included.
	 *
	 * Filtered in the query rather than in Java: this table holds every device type at once,
	 * so one type's list is a slice of it and will only grow. The deleted rows come back so
	 * the service can revive one whose name is being re-added — the UNIQUE key means a second
	 * insert is impossible.
	 */
	List<AssetMaster> findByDeviceId(Long deviceId);

	/** The highest reference running number used for this device type in this financial year. */
	@Query("SELECT MAX(a.refSeq) FROM AssetMaster a WHERE a.deviceId = :deviceId AND a.refFy = :fy")
	Integer findMaxRefSeq(@Param("deviceId") Long deviceId, @Param("fy") Integer fy);
}
