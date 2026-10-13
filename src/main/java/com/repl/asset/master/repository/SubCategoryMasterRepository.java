package com.repl.asset.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.SubCategoryMaster;

@Repository
public interface SubCategoryMasterRepository extends JpaRepository<SubCategoryMaster, Long> {

	List<SubCategoryMaster> findByStatusOrderBySubCategoryNameAsc(int status);

	/**
	 * Everything except deleted — active AND inactive.
	 *
	 * The master screen needs both: a row switched off still has to be visible there, or
	 * there is no way to switch it back on. Dropdowns elsewhere ask for status = 1 only.
	 */
	List<SubCategoryMaster> findByStatusNotOrderBySubCategoryNameAsc(int status);

	/**
	 * The row with this name under this category, whatever its status — deleted included.
	 *
	 * Same reason as PlantCategoryRepository's lookup: uk_sub_category is UNIQUE on
	 * (category_id, sub_category_name), so a soft-deleted row still holds that name and
	 * re-adding it means reviving the row rather than inserting a second one.
	 */
	Optional<SubCategoryMaster> findByCategoryIdAndSubCategoryNameIgnoreCase(Long categoryId,
			String subCategoryName);
}
