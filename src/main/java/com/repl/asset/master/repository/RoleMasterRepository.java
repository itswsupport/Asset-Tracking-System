package com.repl.asset.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.RoleMaster;

@Repository
public interface RoleMasterRepository extends JpaRepository<RoleMaster, Long> {
}
