package com.repl.asset.common.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.repl.asset.exception.ApplicationException;
import com.repl.asset.message.IResponseCode;

@Service
public class FileStorageService {

	@Value("${file.upload.base-path:/upload}")
	private String uploadBasePath;

	// Subfolder constants for AMS modules. HRMS has /hrms/resume, /hrms/aadhar and so on;
	// these are the asset equivalents.
	public static final String FOLDER_ASSET_IMAGE = "/ams/assetImage";
	public static final String FOLDER_INVOICE = "/ams/invoice";
	public static final String FOLDER_WARRANTY = "/ams/warranty";
	public static final String FOLDER_SERVICE_REPORT = "/ams/serviceReport";
	public static final String FOLDER_DOCUMENT = "/ams/documents";

	public static final long MB = 1024L * 1024L;
	public static final long LARGE_FILE_SIZE = 50 * MB;
	public static final long DEFAULT_FILE_SIZE = 10 * MB;

	// Logical input names that get the larger 50 MB allowance.
	private static final Set<String> LARGE_DOC_INPUTS = Set.of("serviceReport", "invoice");

	public static long getMaxFileSize(String inputName) {
		return LARGE_DOC_INPUTS.contains(inputName) ? LARGE_FILE_SIZE : DEFAULT_FILE_SIZE;
	}

	/**
	 * Enforces PDF-only + per-document size limit on the server. No-op for null/empty files
	 * (optional uploads).
	 */
	public void validateUploadFile(MultipartFile file, String inputName) throws ApplicationException {
		if (file == null || file.isEmpty()) {
			return;
		}
		String name = file.getOriginalFilename();
		boolean isPdf = "application/pdf".equalsIgnoreCase(file.getContentType())
				|| (name != null && name.toLowerCase().endsWith(".pdf"));
		if (!isPdf) {
			throw new ApplicationException(IResponseCode.WRONG_FILE_FORMAT, "Only PDF files are allowed.");
		}
		long maxSize = getMaxFileSize(inputName);
		if (file.getSize() > maxSize) {
			throw new ApplicationException(IResponseCode.FILE_UPLOAD_ERROR,
					"File size must be " + (maxSize / MB) + " MB or less.");
		}
	}

	/**
	 * Store a file in the specified folder.
	 *
	 * @param file       MultipartFile to store
	 * @param folder     Target folder (use constants like FOLDER_INVOICE)
	 * @param filePrefix Prefix for the filename (e.g., "INVOICE_123_")
	 * @return The full path where file was stored
	 */
	public String storeFile(MultipartFile file, String folder, String filePrefix) throws IOException {
		Path uploadDir = Paths.get(uploadBasePath + folder);
		if (!Files.exists(uploadDir)) {
			Files.createDirectories(uploadDir);
		}

		String originalFilename = file.getOriginalFilename();
		String fileExtension = getFileExtension(originalFilename);
		String uniqueFilename = filePrefix + System.currentTimeMillis() + "_"
				+ UUID.randomUUID().toString().substring(0, 8) + fileExtension;

		Path filePath = uploadDir.resolve(uniqueFilename);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		return filePath.toString();
	}

	/**
	 * Store a file with its original filename.
	 */
	public String storeFileWithOriginalName(MultipartFile file, String folder) throws IOException {
		Path uploadDir = Paths.get(uploadBasePath + folder);
		if (!Files.exists(uploadDir)) {
			Files.createDirectories(uploadDir);
		}

		// Use only the base name and strip path separators to prevent path traversal from a
		// crafted filename
		String rawName = file.getOriginalFilename();
		String fileName = (rawName == null || rawName.isBlank())
				? "file"
				: rawName.replaceAll("[\\\\/]+", "_").replaceAll("\\.\\.", "_").trim();
		Path filePath = uploadDir.resolve(fileName).normalize();
		if (!filePath.startsWith(uploadDir.normalize())) {
			throw new IOException("Invalid file name: " + rawName);
		}
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		return filePath.toString();
	}

	/**
	 * Load a file as Resource for download.
	 */
	public Resource loadFileAsResource(String fullPath) throws MalformedURLException {
		Path filePath = Paths.get(fullPath).normalize();
		return new UrlResource(filePath.toUri());
	}

	/**
	 * Delete a file.
	 */
	public boolean deleteFile(String fullPath) {
		try {
			return Files.deleteIfExists(Paths.get(fullPath));
		} catch (IOException e) {
			System.err.println("Error deleting file: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Check if file exists.
	 */
	public boolean fileExists(String fullPath) {
		return Files.exists(Paths.get(fullPath));
	}

	public String getUploadBasePath() {
		return uploadBasePath;
	}

	private String getFileExtension(String filename) {
		if (filename != null && filename.contains(".")) {
			return filename.substring(filename.lastIndexOf("."));
		}
		return "";
	}
}
