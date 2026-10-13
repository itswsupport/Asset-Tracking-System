package com.repl.asset.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.HddMaster;

@Repository
public interface HddMasterRepository extends JpaRepository<HddMaster, Long> {
}
