package com.repl.asset.master.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.DeviceMaster;

import jakarta.persistence.LockModeType;

@Repository
public interface DeviceMasterRepository extends JpaRepository<DeviceMaster, Long> {

	/**
	 * The device row, locked until the transaction ends (SELECT ... FOR UPDATE).
	 *
	 * Taken before a reference number is assigned, so two machines of the same type saved at
	 * the same moment queue up rather than both reading the same MAX and taking one number.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM DeviceMaster d WHERE d.id = :id")
	Optional<DeviceMaster> lockById(@Param("id") Long id);
}
