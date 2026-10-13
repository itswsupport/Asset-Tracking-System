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
import com.repl.asset.master.bo.DeviceNameBO;
import com.repl.asset.master.service.DeviceNameService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * The device name master's own endpoints:
 *
 *   GET    /devicename/list
 *   POST   /devicename/save
 *   POST   /devicename/update
 *   DELETE /devicename/delete?id=
 *
 * Both trailing-slash forms are mapped: the frontend goes through Next's /api rewrite with
 * trailingSlash:true, so requests arrive as "/devicename/list/".
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class DeviceNameController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(DeviceNameController.class);

	@Autowired
	private DeviceNameService service;

	@GetMapping(value = { "/devicename/list", "/devicename/list/" })
	public ResponseEntity<BaseResponseBO> list() {
		try {
			return getResponseModel(service.list(), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the device name master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/devicename/save", "/devicename/save/" })
	public ResponseEntity<BaseResponseBO> save(@RequestBody DeviceNameBO body) {
		try {
			return getResponseModel(service.save(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: blank name, a model that is not of the chosen device, or a duplicate.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save into the device name master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/devicename/update", "/devicename/update/" })
	public ResponseEntity<BaseResponseBO> update(@RequestBody DeviceNameBO body) {
		try {
			return getResponseModel(service.update(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the device name master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/devicename/delete", "/devicename/delete/" })
	public ResponseEntity<BaseResponseBO> delete(@RequestParam("id") Long id) {
		try {
			service.delete(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete from the device name master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
