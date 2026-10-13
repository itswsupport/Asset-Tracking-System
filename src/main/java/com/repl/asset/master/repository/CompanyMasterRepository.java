package com.repl.asset.master.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.CompanyMaster;

@Repository
public interface CompanyMasterRepository extends JpaRepository<CompanyMaster, Long> {

	/**
	 * Active rows only, ordered so the dropdown groups a company's plants together and the
	 * plant codes run in order within each company.
	 */
	List<CompanyMaster> findByStatusOrderByCompanyNameAscPlantCodeAsc(Integer status);
}
