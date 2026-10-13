package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.HddMaster;
import com.repl.asset.master.repository.HddMasterRepository;

/** CRUD for the HDD master, over its own entity and its own table. */
@Service
public class HddService extends AbstractSpecService<HddMaster> {

	@Autowired
	private HddMasterRepository repository;

	@Override
	protected JpaRepository<HddMaster, Long> repository() {
		return repository;
	}

	@Override
	protected HddMaster newRow() {
		return new HddMaster();
	}

	@Override
	protected String label() {
		return "HDD";
	}
}
