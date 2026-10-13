package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.LocationMaster;

@Repository
public interface LocationMasterRepository extends JpaRepository<LocationMaster, Integer> {

	/** Every location of one plant (plant_id holds the plant code). */
	List<LocationMaster> findByPlantId(Integer plantId);
}
