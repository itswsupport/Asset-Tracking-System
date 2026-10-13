package com.repl.asset.master.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.repl.asset.master.model.ProductMaster;
import com.repl.asset.master.repository.ProductMasterRepository;

/** CRUD for the product master, over its own entity and its own table. */
@Service
public class ProductService extends AbstractSpecService<ProductMaster> {

	@Autowired
	private ProductMasterRepository repository;

	@Override
	protected JpaRepository<ProductMaster, Long> repository() {
		return repository;
	}

	@Override
	protected ProductMaster newRow() {
		return new ProductMaster();
	}

	@Override
	protected String label() {
		return "Product Name";
	}
}
