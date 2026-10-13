package com.repl.asset.login.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.login.model.AmsLoginAccess;

@Repository
public interface AmsLoginAccessRepository extends JpaRepository<AmsLoginAccess, Long> {

	Optional<AmsLoginAccess> findByEmpCode(Long empCode);

	/** status = 1 only — a revoked row must not let anyone in. */
	Optional<AmsLoginAccess> findByEmpCodeAndStatus(Long empCode, int status);

	List<AmsLoginAccess> findByStatus(int status);

	/** For the Reporting Person screen: everyone with access, grouped by company and plant. */
	List<AmsLoginAccess> findByStatusOrderByCompanyNameAscPlantCodeAsc(int status);

	/**
	 * Everything except deleted — active AND inactive.
	 *
	 * An inactive row must stay on the Reporting Person screen so access can be restored;
	 * login itself still requires status = 1, so an inactive person cannot sign in.
	 */
	List<AmsLoginAccess> findByStatusNotOrderByCompanyNameAscPlantCodeAsc(int status);

	boolean existsByEmpCode(Long empCode);
}
