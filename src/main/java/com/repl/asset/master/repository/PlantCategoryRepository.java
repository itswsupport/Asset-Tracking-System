package com.repl.asset.master.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.repl.asset.master.model.PlantCategory;

@Repository
public interface PlantCategoryRepository extends JpaRepository<PlantCategory, Long> {

	List<PlantCategory> findByStatusOrderByCompanyNameAscPlantCodeAsc(int status);

	/**
	 * Everything except deleted — active AND inactive.
	 *
	 * The master screen needs both: a row switched off still has to be visible there, or
	 * there is no way to switch it back on. Screens that USE categories ask for status = 1
	 * only, which is what makes an inactive row disappear from them.
	 */
	List<PlantCategory> findByStatusNotOrderByCompanyNameAscPlantCodeAsc(int status);

	/**
	 * The row for this plant and category, whatever its status — deleted rows included.
	 *
	 * It must return the deleted one. uk_plant_category is UNIQUE on
	 * (plant_code, category_id), so a soft-deleted row still occupies that pair and a second
	 * insert for it is impossible. Re-adding a combination that was deleted therefore means
	 * bringing the existing row back, and the save cannot decide that without seeing it.
	 *
	 * This replaced an exists…() check that could not tell a live row from a deleted one, so
	 * deleting a category from a plant left it permanently un-re-addable.
	 */
	Optional<PlantCategory> findByPlantCodeAndCategoryId(String plantCode, Long categoryId);
}
