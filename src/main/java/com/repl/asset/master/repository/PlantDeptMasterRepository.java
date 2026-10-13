package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.PlantDeptMaster;

@Repository
public interface PlantDeptMasterRepository extends JpaRepository<PlantDeptMaster, Long> {

	/** Every row in company -> plant -> department order. */
	List<PlantDeptMaster> findAllByOrderByCompanyIdAscPlantCodeAscDepartmentAsc();
}
