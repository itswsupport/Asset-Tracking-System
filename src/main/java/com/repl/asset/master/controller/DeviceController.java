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
import com.repl.asset.master.bo.DeviceBO;
import com.repl.asset.master.service.DeviceService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * The device master's own endpoints:
 *
 *   GET    /device/list
 *   POST   /device/save
 *   POST   /device/update
 *   DELETE /device/delete?id=
 *
 * Both trailing-slash forms are mapped: the frontend goes through Next's /api rewrite with
 * trailingSlash:true, so requests arrive as "/device/list/".
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class DeviceController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

	@Autowired
	private DeviceService service;

	@GetMapping(value = { "/device/list", "/device/list/" })
	public ResponseEntity<BaseResponseBO> list() {
		try {
			return getResponseModel(service.list(), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the device master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/device/save", "/device/save/" })
	public ResponseEntity<BaseResponseBO> save(@RequestBody DeviceBO body) {
		try {
			return getResponseModel(service.save(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: blank name, or the value already exists in this master.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save into the device master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/device/update", "/device/update/" })
	public ResponseEntity<BaseResponseBO> update(@RequestBody DeviceBO body) {
		try {
			return getResponseModel(service.update(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the device master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/device/delete", "/device/delete/" })
	public ResponseEntity<BaseResponseBO> delete(@RequestParam("id") Long id) {
		try {
			service.delete(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete from the device master", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
