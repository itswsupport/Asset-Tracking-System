package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.ServerMaster;
import com.repl.asset.master.repository.ServerMasterRepository;

/** CRUD for the server master, over its own entity and its own table. */
@Service
public class ServerService extends AbstractSpecService<ServerMaster> {

	@Autowired
	private ServerMasterRepository repository;

	@Override
	protected JpaRepository<ServerMaster, Long> repository() {
		return repository;
	}

	@Override
	protected ServerMaster newRow() {
		return new ServerMaster();
	}

	@Override
	protected String label() {
		return "Server Name";
	}
}
