package com.repl.asset.master.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.repl.asset.basecontroller.BaseController;
import com.repl.asset.bo.BaseResponseBO;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.PlantCategoryBO;
import com.repl.asset.master.bo.ReportingPersonBO;
import com.repl.asset.master.bo.SubCategoryBO;
import com.repl.asset.master.service.IMasterService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * Master data for the admin screens.
 *
 * Every path is mapped with and without a trailing slash: the frontend goes through Next's
 * /api rewrite and next.config.mjs sets trailingSlash:true, so requests arrive as
 * "/company/list/" while Spring Boot 3+ no longer matches trailing slashes by default.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class MasterController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(MasterController.class);

	@Autowired
	private IMasterService masterService;

	/**
	 * Active company/plant rows from db_all_master.company_mst. One row per company AND
	 * plant, so the form takes the distinct company names for its Company dropdown and
	 * filters the same rows for that company's plants.
	 */
	@GetMapping(value = { "/company/list", "/company/list/" })
	public ResponseEntity<BaseResponseBO> getCompanyList() {
		try {
			return getResponseModel(masterService.getCompanyList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the company list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/**
	 * The Device Status choices on ADD DEVICE — whether a machine is with someone or kept in
	 * stock. Stored on the asset row as this text, the way the asset workbook kept it.
	 */
	@GetMapping(value = { "/devicestatus/list", "/devicestatus/list/" })
	public ResponseEntity<BaseResponseBO> getDeviceStatusList() {
		return getResponseModel(java.util.List.of("In Use", "In Spare"), IResponseCode.SUCCESS,
				IResponseMessage.SUCCESS, null);
	}

	/** Plant-wise departments from db_all_master.plant_dept_mst — read-only. */
	@GetMapping(value = { "/department/list", "/department/list/" })
	public ResponseEntity<BaseResponseBO> getDepartmentList() {
		try {
			return getResponseModel(masterService.getDepartmentList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the department list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** The independent category list. Depends on no company and no plant. */
	@GetMapping(value = { "/category/list", "/category/list/" })
	public ResponseEntity<BaseResponseBO> getCategoryList() {
		try {
			return getResponseModel(masterService.getCategoryList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the category list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** Company + plant + category rows already saved. */
	@GetMapping(value = { "/plantcategory/list", "/plantcategory/list/" })
	public ResponseEntity<BaseResponseBO> getPlantCategoryList() {
		try {
			return getResponseModel(masterService.getPlantCategoryList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the saved plant categories", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/**
	 * POST /plantcategory/save — what the Category Master form submits.
	 *
	 * As with every BaseController response the HTTP status is 200 even on failure; the
	 * caller must read status_code out of the body.
	 */
	@PostMapping(value = { "/plantcategory/save", "/plantcategory/save/" })
	public ResponseEntity<BaseResponseBO> savePlantCategory(@RequestBody PlantCategoryBO body) {
		try {
			PlantCategoryBO saved = masterService.savePlantCategory(body);
			return getResponseModel(saved, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: missing fields, unknown category, or already added for that plant.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the plant category", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** Sub categories, each carrying the name of the category it belongs to. */
	@GetMapping(value = { "/subcategory/list", "/subcategory/list/" })
	public ResponseEntity<BaseResponseBO> getSubCategoryList() {
		try {
			return getResponseModel(masterService.getSubCategoryList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the sub category list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** POST /subcategory/save — what the Sub Category Master form submits. */
	@PostMapping(value = { "/subcategory/save", "/subcategory/save/" })
	public ResponseEntity<BaseResponseBO> saveSubCategory(@RequestBody SubCategoryBO body) {
		try {
			SubCategoryBO saved = masterService.saveSubCategory(body);
			return getResponseModel(saved, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: missing fields, unknown category, or the name already used there.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the sub category", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** POST /plantcategory/update — the edit pencil on a saved row. */
	@PostMapping(value = { "/plantcategory/update", "/plantcategory/update/" })
	public ResponseEntity<BaseResponseBO> updatePlantCategory(@RequestBody PlantCategoryBO body) {
		try {
			return getResponseModel(masterService.updatePlantCategory(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the plant category", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** DELETE /plantcategory/delete?id= — retires the row rather than removing it. */
	@DeleteMapping(value = { "/plantcategory/delete", "/plantcategory/delete/" })
	public ResponseEntity<BaseResponseBO> deletePlantCategory(@RequestParam("id") Long id) {
		try {
			masterService.deletePlantCategory(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the plant category", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** POST /subcategory/update — the edit pencil on a saved sub category. */
	@PostMapping(value = { "/subcategory/update", "/subcategory/update/" })
	public ResponseEntity<BaseResponseBO> updateSubCategory(@RequestBody SubCategoryBO body) {
		try {
			return getResponseModel(masterService.updateSubCategory(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the sub category", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** DELETE /subcategory/delete?id= — retires the row rather than removing it. */
	@DeleteMapping(value = { "/subcategory/delete", "/subcategory/delete/" })
	public ResponseEntity<BaseResponseBO> deleteSubCategory(@RequestParam("id") Long id) {
		try {
			masterService.deleteSubCategory(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the sub category", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** Active employees from db_all_master.emp_details, for the Reporting Person dropdown. */
	@GetMapping(value = { "/employee/list", "/employee/list/" })
	public ResponseEntity<BaseResponseBO> getEmployeeList() {
		try {
			return getResponseModel(masterService.getEmployeeList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the employee list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@GetMapping(value = { "/reportingperson/list", "/reportingperson/list/" })
	public ResponseEntity<BaseResponseBO> getReportingPersonList() {
		try {
			return getResponseModel(masterService.getReportingPersonList(), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the reporting person list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/**
	 * POST /reportingperson/save
	 *
	 * Writes two rows: the assignment itself, and the employee's AMS login access with the
	 * role chosen here.
	 */
	@PostMapping(value = { "/reportingperson/save", "/reportingperson/save/" })
	public ResponseEntity<BaseResponseBO> saveReportingPerson(@RequestBody ReportingPersonBO body) {
		try {
			return getResponseModel(masterService.saveReportingPerson(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the reporting person", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** POST /reportingperson/update — the edit pencil. The employee is not changeable. */
	@PostMapping(value = { "/reportingperson/update", "/reportingperson/update/" })
	public ResponseEntity<BaseResponseBO> updateReportingPerson(@RequestBody ReportingPersonBO body) {
		try {
			return getResponseModel(masterService.updateReportingPerson(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the reporting person", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/reportingperson/delete", "/reportingperson/delete/" })
	public ResponseEntity<BaseResponseBO> deleteReportingPerson(@RequestParam("id") Long id) {
		try {
			masterService.deleteReportingPerson(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the reporting person", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
