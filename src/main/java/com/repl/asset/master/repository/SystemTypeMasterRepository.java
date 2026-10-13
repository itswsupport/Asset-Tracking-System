package com.repl.asset.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.SystemTypeMaster;

@Repository
public interface SystemTypeMasterRepository extends JpaRepository<SystemTypeMaster, Long> {
}
