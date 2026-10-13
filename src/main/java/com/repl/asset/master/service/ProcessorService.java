package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.ProcessorMaster;
import com.repl.asset.master.repository.ProcessorMasterRepository;

/** CRUD for the Processor master, over its own entity and its own table. */
@Service
public class ProcessorService extends AbstractSpecService<ProcessorMaster> {

	@Autowired
	private ProcessorMasterRepository repository;

	@Override
	protected JpaRepository<ProcessorMaster, Long> repository() {
		return repository;
	}

	@Override
	protected ProcessorMaster newRow() {
		return new ProcessorMaster();
	}

	@Override
	protected String label() {
		return "Processor";
	}
}
