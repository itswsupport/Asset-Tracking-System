package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.LaptopMaster;
import com.repl.asset.master.repository.LaptopMasterRepository;

/** CRUD for the laptop master, over its own entity and its own table. */
@Service
public class LaptopService extends AbstractSpecService<LaptopMaster> {

	@Autowired
	private LaptopMasterRepository repository;

	@Override
	protected JpaRepository<LaptopMaster, Long> repository() {
		return repository;
	}

	@Override
	protected LaptopMaster newRow() {
		return new LaptopMaster();
	}

	@Override
	protected String label() {
		return "Laptop number";
	}
}
