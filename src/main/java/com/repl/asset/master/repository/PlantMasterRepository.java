package com.repl.asset.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.PlantMaster;

@Repository
public interface PlantMasterRepository extends JpaRepository<PlantMaster, Long> {
}
