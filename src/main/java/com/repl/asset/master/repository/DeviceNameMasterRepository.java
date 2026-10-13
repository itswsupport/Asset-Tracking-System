package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.DeviceNameMaster;

@Repository
public interface DeviceNameMasterRepository extends JpaRepository<DeviceNameMaster, Long> {

	/**
	 * The catalogue row for one machine name under one device type, whatever its case — for
	 * AssetService to mark it Assigned / Open as the machine is added, renamed or deleted.
	 */
	List<DeviceNameMaster> findByDeviceIdAndDeviceNameIgnoreCase(Long deviceId, String deviceName);

	/** One device type's names, active only — filtered in the database (idx_dnm_device), not in Java. */
	List<DeviceNameMaster> findByDeviceIdAndStatus(Long deviceId, int status);

	/** The highest reference running number given to a name of this device type in this year. */
	@Query("SELECT MAX(n.refSeq) FROM DeviceNameMaster n WHERE n.deviceId = :deviceId AND n.refFy = :fy")
	Integer findMaxRefSeq(@Param("deviceId") Long deviceId, @Param("fy") Integer fy);
}
