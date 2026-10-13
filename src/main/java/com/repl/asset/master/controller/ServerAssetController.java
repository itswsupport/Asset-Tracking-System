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
import com.repl.asset.master.bo.ServerAssetBO;
import com.repl.asset.master.service.ServerAssetService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * The server register's endpoints — the user side's ADD SERVER and its list view:
 *
 *   GET    /serverasset/list
 *   POST   /serverasset/save
 *   POST   /serverasset/update
 *   DELETE /serverasset/delete?id=
 *
 * /serverasset rather than /server, which is already taken by the thin admin master of
 * server names on the Setting page. Two different things over two different tables.
 *
 * Both trailing-slash forms are mapped: the frontend goes through Next's /api rewrite with
 * trailingSlash:true, so requests arrive as "/serverasset/list/".
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class ServerAssetController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(ServerAssetController.class);

	@Autowired
	private ServerAssetService service;

	@GetMapping(value = { "/serverasset/list", "/serverasset/list/" })
	public ResponseEntity<BaseResponseBO> list() {
		try {
			return getResponseModel(service.list(), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (Exception e) {
			logger.error("Failed to load the server list", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	/** GET /serverasset/changelog?server_id= — one server's USER LOG, newest first. */
	@GetMapping(value = { "/serverasset/changelog", "/serverasset/changelog/" })
	public ResponseEntity<BaseResponseBO> changeLog(@org.springframework.web.bind.annotation.RequestParam("server_id") Long serverId) {
		try {
			return getResponseModel(service.changeLog(serverId), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (com.repl.asset.exception.ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to load the server user log", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/serverasset/save", "/serverasset/save/" })
	public ResponseEntity<BaseResponseBO> save(@RequestBody ServerAssetBO body) {
		try {
			return getResponseModel(service.save(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			// Expected: a blank name, a name already used, or a negative warranty term.
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to save the server", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@PostMapping(value = { "/serverasset/update", "/serverasset/update/" })
	public ResponseEntity<BaseResponseBO> update(@RequestBody ServerAssetBO body) {
		try {
			return getResponseModel(service.update(body), IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to update the server", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}

	@DeleteMapping(value = { "/serverasset/delete", "/serverasset/delete/" })
	public ResponseEntity<BaseResponseBO> delete(@RequestParam("id") Long id) {
		try {
			service.delete(id);
			return getResponseModel(null, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
		} catch (ApplicationException e) {
			return getResponseModel(null, e.getCode(), e.getMessage(), null);
		} catch (Exception e) {
			logger.error("Failed to delete the server", e);
			return getResponseModel(null, IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR, null);
		}
	}
}
