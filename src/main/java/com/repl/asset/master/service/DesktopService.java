package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.DesktopMaster;
import com.repl.asset.master.repository.DesktopMasterRepository;

/** CRUD for the desktop master, over its own entity and its own table. */
@Service
public class DesktopService extends AbstractSpecService<DesktopMaster> {

	@Autowired
	private DesktopMasterRepository repository;

	@Override
	protected JpaRepository<DesktopMaster, Long> repository() {
		return repository;
	}

	@Override
	protected DesktopMaster newRow() {
		return new DesktopMaster();
	}

	@Override
	protected String label() {
		return "Desktop number";
	}
}
