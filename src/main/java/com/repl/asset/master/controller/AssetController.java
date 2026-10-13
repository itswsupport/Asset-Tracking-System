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
import com.repl.asset.master.bo.AssetBO;
import com.repl.asset.master.service.AssetService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * The asset register's endpoints — the user side's ADD DEVICE and its list view:
 *
 *   GET    /asset/list?device_id=
 *   POST   /asset/save                 device_id in the body
 *   POST   /asset/update               device_id in the body
 *   DELETE /asset/delete?id=
 *
 * Keyed on device_id throughout, so EVERY device in the device master works and none of them
 * is named here. There is no /asset/types any more and nothing for the user side to filter
 * against: it offers whatever /device/list returns, and all of it can be saved.
 *
 * Both trailing-slash forms are mapped: the frontend goes through Next's /api rewrite with
 * trailingSlash:true, so requests arrive as "/asset/list/".
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class AssetController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

	@Autowired
	private AssetService service;

	/**
	 * @param includeImported true also returns the machines carried over from the asset
	 *                        workbook. LIST DEVICE leaves it off, so it shows only what was
	 *                        added through ADD DEVICE; ADD DEVICE passes it, because its
	 *                        dropdowns and its duplicate check need the whole catalogue.
	 */
	@GetMapping(value = { "/asset/list", "/asset/list/" })
	public ResponseEntity<BaseResponseBO> list(@RequestParam("device_id") Long deviceId,
			@RequestParam(name = "include_imported", required = false,
					defaultValue = "false") boolean includeImported) {
		try {
			return getResponseModel(service.list(deviceId, includeImported), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to load the asset list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/**
	 * GET /asset/nextreference?device_id=&name= — the reference number the next machine of this
	 * device type would get, e.g. PR26SOU646001. A preview for ADD DEVICE; nothing is reserved.
	 */
	@GetMapping(value = { "/asset/nextreference", "/asset/nextreference/" })
	public ResponseEntity<BaseResponseBO> nextReference(@RequestParam("device_id") Long deviceId,
			@RequestParam("name") String name) {
		try {
			return getResponseModel(service.previewReference(deviceId, name), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to preview the reference number", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/asset/save", "/asset/save/" })
	public ResponseEntity<BaseResponseBO> save(@RequestBody AssetBO body) {
		try {
			return getResponseModel(service.save(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: no device, a blank name, or that name already used for this device.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the asset", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/asset/update", "/asset/update/" })
	public ResponseEntity<BaseResponseBO> update(@RequestBody AssetBO body) {
		try {
			return getResponseModel(service.update(body), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the asset", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/asset/delete", "/asset/delete/" })
	public ResponseEntity<BaseResponseBO> delete(@RequestParam("id") Long id) {
		try {
			service.delete(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the asset", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
