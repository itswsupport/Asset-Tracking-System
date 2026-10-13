package com.repl.asset.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.ServerMaster;

@Repository
public interface ServerMasterRepository extends JpaRepository<ServerMaster, Long> {
}
