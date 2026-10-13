package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.PrinterMaster;
import com.repl.asset.master.repository.PrinterMasterRepository;

/** CRUD for the printer master, over its own entity and its own table. */
@Service
public class PrinterService extends AbstractSpecService<PrinterMaster> {

	@Autowired
	private PrinterMasterRepository repository;

	@Override
	protected JpaRepository<PrinterMaster, Long> repository() {
		return repository;
	}

	@Override
	protected PrinterMaster newRow() {
		return new PrinterMaster();
	}

	@Override
	protected String label() {
		return "Printer number";
	}
}
