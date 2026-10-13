package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.TabletMaster;
import com.repl.asset.master.repository.TabletMasterRepository;

/** CRUD for the tablet master, over its own entity and its own table. */
@Service
public class TabletService extends AbstractSpecService<TabletMaster> {

	@Autowired
	private TabletMasterRepository repository;

	@Override
	protected JpaRepository<TabletMaster, Long> repository() {
		return repository;
	}

	@Override
	protected TabletMaster newRow() {
		return new TabletMaster();
	}

	@Override
	protected String label() {
		return "Tablet number";
	}
}
