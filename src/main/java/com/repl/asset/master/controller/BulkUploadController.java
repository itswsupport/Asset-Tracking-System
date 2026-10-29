package com.repl.asset.master.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.repl.asset.basecontroller.BaseController;
import com.repl.asset.bo.BaseResponseBO;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.AssetActionHistoryBO;
import com.repl.asset.master.bo.AssetBO;
import com.repl.asset.master.bo.BulkRowResultBO;
import com.repl.asset.master.bo.ServerAssetBO;
import com.repl.asset.master.bo.SoftwareBO;
import com.repl.asset.master.model.BulkUploadLog;
import com.repl.asset.master.repository.BulkUploadLogRepository;
import com.repl.asset.master.service.AssetService;
import com.repl.asset.master.service.ServerAssetService;
import com.repl.asset.master.service.SoftwareService;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * BULK UPLOAD — the user side's Excel import, one endpoint per register:
 *
 *   POST /bulk/device    rows for ADD DEVICE    -> ams_add_device_master
 *   POST /bulk/server    rows for ADD SERVER    -> ams_server_with_ip_master
 *   POST /bulk/software  rows for ADD SOFTWARE  -> ams_software_master
 *
 * NOTHING NEW ABOUT HOW A ROW IS SAVED. Each row goes through the very same save() the form
 * uses, so it gets the same checks (duplicates, device type / model, a USER's plant and device
 * types), the same reference number and the same Open / Assigned status.
 *
 * ONE ROW, ONE TRANSACTION. This controller has no transaction of its own, so every save()
 * commits or rolls back on its own: a bad row is reported and skipped, and the good rows
 * before and after it are kept. The answer lists every row, in the order sent.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class BulkUploadController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(BulkUploadController.class);

	/** More than this in one upload is refused, so one request cannot run for minutes. */
	private static final int MAX_ROWS = 1000;

	@Autowired
	private AssetService assetService;

	@Autowired
	private ServerAssetService serverAssetService;

	@Autowired
	private SoftwareService softwareService;

	/** A device's Action History — where an employee assigned in the sheet is recorded. */
	@Autowired
	private com.repl.asset.master.service.AssetActionHistoryService historyService;

	/** Turns each row's JSON into the register's object, one row at a time. */
	@Autowired
	private ObjectMapper objectMapper;

	/** The record of each imported sheet — ams_bulk_upload_log. */
	@Autowired
	private BulkUploadLogRepository uploadLogRepository;

	/** One step of an import: save a row, return the name and reference to report back. */
	@FunctionalInterface
	private interface RowSaver<T> {
		String[] save(T row) throws ApplicationException;
	}

	/**
	 * DEVICES — each row is SAVED as a new device, exactly as ADD DEVICE does. A name already
	 * added is refused for that row (with its Reference No), never changed from a sheet.
	 */
	@PostMapping(value = { "/bulk/device", "/bulk/device/" })
	public ResponseEntity<BaseResponseBO> devices(@RequestBody List<JsonNode> rows,
			@RequestParam(value = "file_hash", required = false) String fileHash) {
		ResponseEntity<BaseResponseBO> refused = refuseIfUploaded("device", fileHash);
		if (refused != null) {
			return refused;
		}
		return run(rows, AssetBO.class, "name", AssetBO::getName, row -> {
			// Marked as added through BULK UPLOAD (ams_add_device_master.added_via = 'BULK').
			row.setAdded_via("BULK");
			AssetBO saved = assetService.save(row);
			/*
			 * ASSIGN TO EMPLOYEE, as at the bottom of ADD DEVICE: once the device is saved, its
			 * first Action History entry — that employee, from that Start Date, "Assigned" —
			 * through the same service, which then marks the device Assigned.
			 */
			String note = null;
			String code = row.getEmployee_code() == null ? "" : row.getEmployee_code().trim();
			if (!code.isEmpty() && row.getAssign_start_date() != null) {
				try {
					AssetActionHistoryBO entry = new AssetActionHistoryBO();
					entry.setAsset_id(saved.getId());
					entry.setAssigned_emp_code(code);
					entry.setAssigned_emp_name(row.getEmployee_name());
					entry.setStart_date(row.getAssign_start_date());
					entry.setEnd_date(null);
					entry.setAction_status("Assigned");
					// Assigned by whoever uploaded the sheet — the same person recorded as ADDED BY.
					entry.setChanged_by_code(row.getChanged_by_code());
					entry.setChanged_by_name(row.getChanged_by_name());
					historyService.save(entry);
					note = "Assigned to " + code;
				} catch (Exception e) {
					// The device is saved; only its assignment is not — said on the row.
					logger.warn("Bulk upload: {} saved, assignment failed", saved.getName(), e);
					note = "Saved, but not assigned: "
							+ (e instanceof ApplicationException ? e.getMessage() : "the assignment could not be recorded")
							+ ". Assign it from Open Devices.";
				}
			}
			return new String[] { saved.getName(), saved.getReference_no(), note };
		});
	}

	@PostMapping(value = { "/bulk/server", "/bulk/server/" })
	public ResponseEntity<BaseResponseBO> servers(@RequestBody List<JsonNode> rows,
			@RequestParam(value = "file_hash", required = false) String fileHash) {
		ResponseEntity<BaseResponseBO> refused = refuseIfUploaded("server", fileHash);
		if (refused != null) {
			return refused;
		}
		return run(rows, ServerAssetBO.class, "server_name", ServerAssetBO::getServer_name, row -> {
			// Marked as added through BULK UPLOAD (ams_server_with_ip_master.added_via = 'BULK').
			row.setAdded_via("BULK");
			ServerAssetBO saved = serverAssetService.save(row);
			return new String[] { saved.getServer_name(), null };
		});
	}

	@PostMapping(value = { "/bulk/software", "/bulk/software/" })
	public ResponseEntity<BaseResponseBO> software(@RequestBody List<JsonNode> rows,
			@RequestParam(value = "file_hash", required = false) String fileHash) {
		ResponseEntity<BaseResponseBO> refused = refuseIfUploaded("software", fileHash);
		if (refused != null) {
			return refused;
		}
		return run(rows, SoftwareBO.class, "product_name", SoftwareBO::getProduct_name, row -> {
			// Marked as added through BULK UPLOAD (ams_software_master.added_via = 'BULK').
			row.setAdded_via("BULK");
			SoftwareBO saved = softwareService.save(row);
			return new String[] { saved.getProduct_name(), null };
		});
	}

	// ================= UPLOADED SHEETS: one record per imported sheet =================

	private static final java.util.Set<String> TYPES = java.util.Set.of("device", "server", "software");

	/**
	 * The earlier import of the same sheet (same register, same data fingerprint), or null.
	 * The fingerprint is of the sheet's data, so renaming or re-saving it in Excel does not
	 * make it a different sheet.
	 */
	private BulkUploadLog earlier(String type, String fileHash) {
		if (fileHash == null || fileHash.isBlank()) {
			return null;
		}
		return uploadLogRepository.findFirstByUploadTypeAndFileHash(type, fileHash.trim()).orElse(null);
	}

	/** "Already uploaded on 09/10/2026 by GANESH BARATHE (file.xlsx)". */
	private static String alreadyUploaded(BulkUploadLog log) {
		String on = log.getUploadDate() == null ? ""
				: " on " + log.getUploadDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		String by = log.getUploadedByName() == null || log.getUploadedByName().isBlank() ? ""
				: " by " + log.getUploadedByName();
		String file = log.getFileName() == null ? "" : " (" + log.getFileName() + ")";
		return "This sheet was already uploaded" + on + by + file + ". The same sheet cannot be uploaded again.";
	}

	/** Refuses an import of a sheet already imported — checked here too, not only on screen. */
	private ResponseEntity<BaseResponseBO> refuseIfUploaded(String type, String fileHash) {
		BulkUploadLog log = earlier(type, fileHash);
		return log == null ? null
				: getResponseModel(summary(log), IResponseCode.EXISTING_ENTITY, alreadyUploaded(log), null);
	}

	/** A sheet's record without its rows, for lists and messages. */
	private static java.util.Map<String, Object> summary(BulkUploadLog log) {
		java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
		m.put("id", log.getId());
		m.put("upload_type", log.getUploadType());
		m.put("file_name", log.getFileName());
		m.put("total_rows", log.getTotalRows());
		m.put("imported", log.getImported());
		m.put("failed", log.getFailed());
		m.put("uploaded_by_code", log.getUploadedByCode());
		m.put("uploaded_by_name", log.getUploadedByName());
		m.put("upload_date", log.getUploadDate() == null ? null : log.getUploadDate().toString());
		m.put("upload_time", log.getUploadTime() == null ? null
				: log.getUploadTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
		return m;
	}

	/** GET /bulk/check?type=&file_hash= — the earlier upload of this sheet, if any (response null if none). */
	@GetMapping(value = { "/bulk/check", "/bulk/check/" })
	public ResponseEntity<BaseResponseBO> check(@RequestParam("type") String type,
			@RequestParam("file_hash") String fileHash) {
		if (!TYPES.contains(type)) {
			return unknownType(type);
		}
		BulkUploadLog log = earlier(type, fileHash);
		return getResponseModel(log == null ? null : summary(log), IResponseCode.SUCCESS,
				log == null ? IResponseMessage.SUCCESS : alreadyUploaded(log), null);
	}

	/**
	 * GET /bulk/uploaded?type= — EVERY row imported so far for this register, from all its
	 * uploaded sheets, newest sheet first: 50 from earlier sheets and 10 from today's are all
	 * 60 here. Only rows that were imported (a failed row never became a device). Each carries
	 * the sheet it came from — file_name, uploaded_on and uploaded_by — and a unique id.
	 */
	@GetMapping(value = { "/bulk/uploaded", "/bulk/uploaded/" })
	public ResponseEntity<BaseResponseBO> uploaded(@RequestParam("type") String type) {
		if (!TYPES.contains(type)) {
			return unknownType(type);
		}
		List<JsonNode> all = new ArrayList<>();
		for (BulkUploadLog log : uploadLogRepository.findByUploadTypeOrderByIdDesc(type)) {
			if (log.getRowsJson() == null) {
				continue;
			}
			String on = (log.getUploadDate() == null ? ""
					: log.getUploadDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
					+ (log.getUploadTime() == null ? ""
							: " " + log.getUploadTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
			String by = log.getUploadedByName() == null ? "" : log.getUploadedByName();
			JsonNode rows;
			try {
				rows = objectMapper.readTree(log.getRowsJson());
			} catch (Exception e) {
				logger.warn("Bulk upload {}: stored rows could not be read", log.getId());
				continue;
			}
			int n = 0;
			for (JsonNode row : rows) {
				if (!(row instanceof tools.jackson.databind.node.ObjectNode obj)) {
					continue;
				}
				/*
				 * A sheet's record keeps only the rows saved, with the sheet's own values. (A
				 * record from before kept every row with its result: only its imported ones count.)
				 */
				String status = obj.path("import_status").asString("");
				if (!status.isEmpty() && !"Imported".equalsIgnoreCase(status)) {
					continue;
				}
				obj.remove("row_no");
				obj.remove("import_status");
				obj.remove("import_message");
				obj.put("id", log.getId() + "-" + (++n));
				obj.put("upload_id", log.getId());
				obj.put("file_name", log.getFileName());
				obj.put("uploaded_on", on.trim());
				obj.put("uploaded_by", by);
				all.add(obj);
			}
		}
		return getResponseModel(all, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
	}

	/** A register name that is not device, server or software — said plainly, not a server error. */
	private ResponseEntity<BaseResponseBO> unknownType(String type) {
		return getResponseModel(null, IResponseCode.INVALID_DATA,
				"Unknown register \"" + type + "\" — use device, server or software.", null);
	}

	/**
	 * POST /bulk/log — records a sheet once it has been imported: its name, fingerprint,
	 * counts, who uploaded it and every row with its result. The same sheet twice is refused.
	 */
	@PostMapping(value = { "/bulk/log", "/bulk/log/" })
	public ResponseEntity<BaseResponseBO> log(@RequestBody JsonNode body) {
		String type = text(body, "upload_type");
		String hash = text(body, "file_hash");
		if (type == null || !TYPES.contains(type) || hash == null) {
			return getResponseModel(null, IResponseCode.INVALID_DATA, "Upload type and sheet fingerprint are required.", null);
		}
		BulkUploadLog existing = earlier(type, hash);
		if (existing != null) {
			return getResponseModel(summary(existing), IResponseCode.EXISTING_ENTITY, alreadyUploaded(existing), null);
		}
		BulkUploadLog log = new BulkUploadLog();
		log.setUploadType(type);
		log.setFileHash(hash);
		log.setFileName(text(body, "file_name"));
		log.setTotalRows(body.path("total_rows").asInt(0));
		log.setImported(body.path("imported").asInt(0));
		log.setFailed(body.path("failed").asInt(0));
		JsonNode rows = body.get("rows");
		log.setRowsJson(rows == null || rows.isNull() ? null : rows.toString());
		log.setUploadedByCode(text(body, "uploaded_by_code"));
		log.setUploadedByName(text(body, "uploaded_by_name"));
		log.setUploadDate(java.time.LocalDate.now());
		log.setUploadTime(java.time.LocalTime.now().withNano(0));
		try {
			return getResponseModel(summary(uploadLogRepository.saveAndFlush(log)), IResponseCode.SUCCESS,
					IResponseMessage.SUCCESS, null);
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			/*
			 * Two people recording the same sheet at the same moment: the UNIQUE key lets one
			 * through. The other is told it is already uploaded — by whom — not a server error.
			 */
			BulkUploadLog winner = earlier(type, hash);
			return winner == null
					? getResponseModel(null, IResponseCode.EXISTING_ENTITY, "This sheet was already uploaded.", null)
					: getResponseModel(summary(winner), IResponseCode.EXISTING_ENTITY, alreadyUploaded(winner), null);
		}
	}

	private static String text(JsonNode body, String field) {
		JsonNode v = body == null ? null : body.get(field);
		if (v == null || v.isNull()) {
			return null;
		}
		String s = v.asString().trim();
		return s.isEmpty() ? null : s;
	}

	/**
	 * Reads and saves every row on its own and reports each one; a failed row never stops the
	 * rest. The rows arrive as plain JSON and are turned into the register's object ONE BY ONE:
	 * read as a typed list, a single bad date anywhere refused the whole file before any row
	 * was looked at.
	 */
	private <T> ResponseEntity<BaseResponseBO> run(List<JsonNode> nodes, Class<T> type, String nameField,
			java.util.function.Function<T, String> nameOf, RowSaver<T> saver) {
		if (nodes == null || nodes.isEmpty()) {
			return getResponseModel(null, IResponseCode.INVALID_DATA, "The file has no rows to upload.", null);
		}
		List<T> rows = new ArrayList<>(nodes.size());
		List<String> unreadable = new ArrayList<>(nodes.size());
		for (JsonNode node : nodes) {
			try {
				rows.add(objectMapper.treeToValue(node, type));
				unreadable.add(null);
			} catch (Exception e) {
				rows.add(null);
				JsonNode name = node == null ? null : node.get(nameField);
				unreadable.add(name == null || name.isNull() ? "" : name.asString());
			}
		}
		return save(rows, unreadable, nameOf, saver);
	}

	private <T> ResponseEntity<BaseResponseBO> save(List<T> rows, List<String> unreadable,
			java.util.function.Function<T, String> nameOf, RowSaver<T> saver) {
		if (rows == null || rows.isEmpty()) {
			return getResponseModel(null, IResponseCode.INVALID_DATA, "The file has no rows to upload.", null);
		}
		if (rows.size() > MAX_ROWS) {
			return getResponseModel(null, IResponseCode.INVALID_DATA,
					"Upload at most " + MAX_ROWS + " rows at a time.", null);
		}
		List<BulkRowResultBO> results = new ArrayList<>(rows.size());
		for (int i = 0; i < rows.size(); i++) {
			T row = rows.get(i);
			if (row == null && unreadable.get(i) != null) {
				results.add(BulkRowResultBO.failed(i, unreadable.get(i),
						"A date or number in this row is not valid (dates as DD/MM/YYYY)."));
				continue;
			}
			String name = row == null ? null : nameOf.apply(row);
			try {
				if (row == null) {
					throw new ApplicationException(IResponseCode.INVALID_DATA, "Empty row.");
				}
				String[] saved = saver.save(row);
				results.add(BulkRowResultBO.ok(i, saved[0], saved[1], saved.length > 2 ? saved[2] : null));
			} catch (ApplicationException e) {
				results.add(BulkRowResultBO.failed(i, name, e.getMessage()));
			} catch (Exception e) {
				logger.error("Bulk upload: row " + i + " (" + name + ") failed", e);
				results.add(BulkRowResultBO.failed(i, name, "Could not save this row."));
			}
		}
		return getResponseModel(results, IResponseCode.SUCCESS, IResponseMessage.SUCCESS, null);
	}
}
