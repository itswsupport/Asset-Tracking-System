package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.RamMaster;
import com.repl.asset.master.repository.RamMasterRepository;

/** CRUD for the RAM master, over its own entity and its own table. */
@Service
public class RamService extends AbstractSpecService<RamMaster> {

	@Autowired
	private RamMasterRepository repository;

	@Override
	protected JpaRepository<RamMaster, Long> repository() {
		return repository;
	}

	@Override
	protected RamMaster newRow() {
		return new RamMaster();
	}

	@Override
	protected String label() {
		return "RAM";
	}
}
