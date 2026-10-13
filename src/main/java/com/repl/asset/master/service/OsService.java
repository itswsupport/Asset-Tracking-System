package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.OsMaster;
import com.repl.asset.master.repository.OsMasterRepository;

/** CRUD for the Operating System master, over its own entity and its own table. */
@Service
public class OsService extends AbstractSpecService<OsMaster> {

	@Autowired
	private OsMasterRepository repository;

	@Override
	protected JpaRepository<OsMaster, Long> repository() {
		return repository;
	}

	@Override
	protected OsMaster newRow() {
		return new OsMaster();
	}

	@Override
	protected String label() {
		return "Operating System";
	}
}
