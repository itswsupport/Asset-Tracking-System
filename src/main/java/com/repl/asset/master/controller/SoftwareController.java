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
import com.repl.asset.master.bo.SoftwareBO;
import com.repl.asset.master.service.SoftwareService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * The software licence register's endpoints — the user side's ADD SOFTWARE and its list:
 *
 *   GET    /software/list
 *   POST   /software/save
 *   POST   /software/update
 *   DELETE /software/delete?id=
 *
 * Both trailing-slash forms are mapped: the frontend goes through Next's /api rewrite with
 * trailingSlash:true, so requests arrive as "/software/list/".
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class SoftwareController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(SoftwareController.class);

	@Autowired
	private SoftwareService service;

	@GetMapping(value = { "/software/list", "/software/list/" })
	public ResponseEntity<BaseResponseBO> list() {
		try {
			return getResponseModel(service.list(), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the software list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** GET /software/changelog?software_id= — one software entry's USER LOG, newest first. */
	@GetMapping(value = { "/software/changelog", "/software/changelog/" })
	public ResponseEntity<BaseResponseBO> changeLog(@org.springframework.web.bind.annotation.RequestParam("software_id") Long softwareId) {
		try {
			return getResponseModel(service.changeLog(softwareId), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (com.repl.asset.exception.ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to load the software user log", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/software/save", "/software/save/" })
	public ResponseEntity<BaseResponseBO> save(@RequestBody SoftwareBO body) {
		try {
			return getResponseModel(service.save(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: a blank product, or one already recorded.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the software", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/software/update", "/software/update/" })
	public ResponseEntity<BaseResponseBO> update(@RequestBody SoftwareBO body) {
		try {
			return getResponseModel(service.update(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the software", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/software/delete", "/software/delete/" })
	public ResponseEntity<BaseResponseBO> delete(@RequestParam("id") Long id) {
		try {
			service.delete(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the software", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
