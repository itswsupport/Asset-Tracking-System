package com.repl.asset.login.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.repl.asset.login.model.EmpDetailsMaster;

@Repository
public interface EmpDetailsMasterRepository extends JpaRepository<EmpDetailsMaster, Long> {

	/** emp_code is UNIQUE in the master, so at most one row comes back. */
	Optional<EmpDetailsMaster> findByEmpCode(Long empCode);

	Optional<EmpDetailsMaster> findByEmpCodeAndEmpStatus(Long empCode, Integer empStatus);

	/**
	 * Several employees in one query, for resolving names on a listing. A findByEmpCode per
	 * row would be one round trip per row.
	 */
	List<EmpDetailsMaster> findByEmpCodeIn(Collection<Long> empCodes);

	/**
	 * Active employees only. 158 of the 1009 rows are emp_status = 0 (left the company) and
	 * must not appear in AMS dropdowns.
	 */
	List<EmpDetailsMaster> findByEmpStatusOrderByEmpFnameAsc(Integer empStatus);

	@Query("SELECT e FROM EmpDetailsMaster e WHERE e.empStatus = 1 AND "
			+ "(LOWER(e.empFname) LIKE LOWER(CONCAT('%', :term, '%')) "
			+ "OR LOWER(e.empLname) LIKE LOWER(CONCAT('%', :term, '%')) "
			+ "OR CAST(e.empCode AS string) LIKE CONCAT('%', :term, '%'))")
	List<EmpDetailsMaster> searchActiveEmployees(@Param("term") String term);
}
