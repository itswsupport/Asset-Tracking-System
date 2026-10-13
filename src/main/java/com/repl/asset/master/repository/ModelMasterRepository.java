package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.ModelMaster;

@Repository
public interface ModelMasterRepository extends JpaRepository<ModelMaster, Long> {

	/** One device type's models — filtered in the database (idx_model_device), not in Java. */
	List<ModelMaster> findByDeviceId(Long deviceId);
}
