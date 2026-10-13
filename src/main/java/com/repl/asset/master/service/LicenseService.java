package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.LicenseMaster;
import com.repl.asset.master.repository.LicenseMasterRepository;

/** CRUD for the license master, over its own entity and its own table. */
@Service
public class LicenseService extends AbstractSpecService<LicenseMaster> {

	@Autowired
	private LicenseMasterRepository repository;

	@Override
	protected JpaRepository<LicenseMaster, Long> repository() {
		return repository;
	}

	@Override
	protected LicenseMaster newRow() {
		return new LicenseMaster();
	}

	@Override
	protected String label() {
		return "License Name";
	}
}
