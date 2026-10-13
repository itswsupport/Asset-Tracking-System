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
import com.repl.asset.master.bo.AssetActionHistoryBO;
import com.repl.asset.master.service.AssetActionHistoryService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * A machine's history:
 *
 *   GET    /assetactionhistory/list?asset_id=
 *   POST   /assetactionhistory/save
 *   POST   /assetactionhistory/update
 *   DELETE /assetactionhistory/delete?id=
 *
 * Both trailing-slash forms are mapped: the frontend goes through Next's /api rewrite with
 * trailingSlash:true, so requests arrive as "/assetactionhistory/list/".
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class AssetActionHistoryController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(AssetActionHistoryController.class);

	@Autowired
	private AssetActionHistoryService service;

	@GetMapping(value = { "/assetactionhistory/list", "/assetactionhistory/list/" })
	public ResponseEntity<BaseResponseBO> list(@RequestParam("asset_id") Long assetId) {
		try {
			return getResponseModel(service.list(assetId), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to load the device history", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/assetactionhistory/save", "/assetactionhistory/save/" })
	public ResponseEntity<BaseResponseBO> save(@RequestBody AssetActionHistoryBO body) {
		try {
			return getResponseModel(service.save(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: no device, a device that has gone, or an end date before its start.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the history entry", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/assetactionhistory/update", "/assetactionhistory/update/" })
	public ResponseEntity<BaseResponseBO> update(@RequestBody AssetActionHistoryBO body) {
		try {
			return getResponseModel(service.update(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the history entry", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/assetactionhistory/delete", "/assetactionhistory/delete/" })
	public ResponseEntity<BaseResponseBO> delete(@RequestParam("id") Long id) {
		try {
			service.delete(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the history entry", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
