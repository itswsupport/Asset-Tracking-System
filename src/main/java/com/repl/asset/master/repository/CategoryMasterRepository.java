package com.repl.asset.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.CategoryMaster;

@Repository
public interface CategoryMasterRepository extends JpaRepository<CategoryMaster, Long> {

	/** Active categories for the form's dropdown, alphabetical. */
	List<CategoryMaster> findByStatusOrderByCategoryNameAsc(int status);

	Optional<CategoryMaster> findByCategoryNameIgnoreCase(String categoryName);
}
