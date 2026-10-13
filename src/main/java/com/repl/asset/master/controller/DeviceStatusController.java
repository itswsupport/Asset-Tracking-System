package com.repl.asset.master.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.repl.asset.basecontroller.BaseController;
import com.repl.asset.bo.BaseResponseBO;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.service.DeviceStatusService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * DEVICE STATUS — read only:
 *
 *   GET /asset/status?device_id=   every machine of that type, ASSIGNED or OPEN, with history
 *
 * Writes go through /assetactionhistory/*, as from ADD DEVICE. Both trailing-slash forms are
 * mapped, as everywhere: the frontend's /api rewrite uses trailingSlash:true.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class DeviceStatusController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(DeviceStatusController.class);

	@Autowired
	private DeviceStatusService service;

	@GetMapping(value = { "/asset/status", "/asset/status/" })
	public ResponseEntity<BaseResponseBO> list(@RequestParam("device_id") Long deviceId) {
		try {
			return getResponseModel(service.list(deviceId), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to load the device status", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
