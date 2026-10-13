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
import com.repl.asset.master.service.AssetService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * A machine's CHANGE HISTORY — read only:
 *
 *   GET /asset/changelog?asset_id=   every field changed by an edit, before → after, newest first
 *
 * Written by /asset/update itself (AssetService.logChanges); there is nothing to post here.
 * Both trailing-slash forms are mapped, as everywhere: the frontend's /api rewrite uses
 * trailingSlash:true.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class AssetChangeLogController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(AssetChangeLogController.class);

	@Autowired
	private AssetService service;

	@GetMapping(value = { "/asset/changelog", "/asset/changelog/" })
	public ResponseEntity<BaseResponseBO> list(@RequestParam("asset_id") Long assetId) {
		try {
			return getResponseModel(service.changeLog(assetId), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to load the change history", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
