package com.repl.asset.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.ServerWithIpMaster;

@Repository
public interface ServerWithIpMasterRepository extends JpaRepository<ServerWithIpMaster, Long> {
}
