package com.repl.asset.master.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * One sheet imported through BULK UPLOAD — ams_bulk_upload_log.
 *
 * file_hash is a fingerprint of the sheet's DATA (not of the file's bytes, which change every
 * time Excel re-saves it), so the same sheet cannot be imported twice even renamed or
 * re-saved: (upload_type, file_hash) is UNIQUE. rows_json keeps every row as it was shown,
 * with its result, so the sheet can be looked at again later.
 */
@Entity
@Table(name = "ams_bulk_upload_log")
public class BulkUploadLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** device, server or software. */
	@Column(name = "upload_type", nullable = false, length = 20)
	private String uploadType;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "file_hash", nullable = false, length = 64)
	private String fileHash;

	@Column(name = "total_rows", nullable = false)
	private int totalRows;

	@Column(name = "imported", nullable = false)
	private int imported;

	@Column(name = "failed", nullable = false)
	private int failed;

	@Lob
	@Column(name = "rows_json", columnDefinition = "LONGTEXT")
	private String rowsJson;

	@Column(name = "uploaded_by_code", length = 50)
	private String uploadedByCode;

	@Column(name = "uploaded_by_name", length = 150)
	private String uploadedByName;

	@Column(name = "upload_date")
	private LocalDate uploadDate;

	@Column(name = "upload_time")
	private LocalTime uploadTime;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getUploadType() { return uploadType; }
	public void setUploadType(String uploadType) { this.uploadType = uploadType; }
	public String getFileName() { return fileName; }
	public void setFileName(String fileName) { this.fileName = fileName; }
	public String getFileHash() { return fileHash; }
	public void setFileHash(String fileHash) { this.fileHash = fileHash; }
	public int getTotalRows() { return totalRows; }
	public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
	public int getImported() { return imported; }
	public void setImported(int imported) { this.imported = imported; }
	public int getFailed() { return failed; }
	public void setFailed(int failed) { this.failed = failed; }
	public String getRowsJson() { return rowsJson; }
	public void setRowsJson(String rowsJson) { this.rowsJson = rowsJson; }
	public String getUploadedByCode() { return uploadedByCode; }
	public void setUploadedByCode(String uploadedByCode) { this.uploadedByCode = uploadedByCode; }
	public String getUploadedByName() { return uploadedByName; }
	public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }
	public LocalDate getUploadDate() { return uploadDate; }
	public void setUploadDate(LocalDate uploadDate) { this.uploadDate = uploadDate; }
	public LocalTime getUploadTime() { return uploadTime; }
	public void setUploadTime(LocalTime uploadTime) { this.uploadTime = uploadTime; }
}
