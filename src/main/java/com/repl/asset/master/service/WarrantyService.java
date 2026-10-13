package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.WarrantyMaster;
import com.repl.asset.master.repository.WarrantyMasterRepository;

/** CRUD for the warranty master, over its own entity and its own table. */
@Service
public class WarrantyService extends AbstractSpecService<WarrantyMaster> {

	@Autowired
	private WarrantyMasterRepository repository;

	@Override
	protected JpaRepository<WarrantyMaster, Long> repository() {
		return repository;
	}

	@Override
	protected WarrantyMaster newRow() {
		return new WarrantyMaster();
	}

	@Override
	protected String label() {
		return "Warranty";
	}
}
