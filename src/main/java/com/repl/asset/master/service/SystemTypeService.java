package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.SystemTypeMaster;
import com.repl.asset.master.repository.SystemTypeMasterRepository;

/** CRUD for the RAM master, over its own entity and its own table. */
@Service
public class SystemTypeService extends AbstractSpecService<SystemTypeMaster> {

	@Autowired
	private SystemTypeMasterRepository repository;

	@Override
	protected JpaRepository<SystemTypeMaster, Long> repository() {
		return repository;
	}

	@Override
	protected SystemTypeMaster newRow() {
		return new SystemTypeMaster();
	}

	@Override
	protected String label() {
		return "System type";
	}
}
