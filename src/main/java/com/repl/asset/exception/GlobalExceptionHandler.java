package com.repl.asset.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.repl.asset.bo.BaseResponseBO;
import com.repl.asset.message.IResponseCode;
import com.repl.asset.message.IResponseMessage;

/**
 * The last line of defence for anything a controller does not catch itself.
 *
 * WHY IT EXISTS. Every endpoint here answers HTTP 200 with a BaseResponseBO, and the frontend
 * reads status_code out of the body to decide what happened — that is the contract the whole
 * application is built on. But a request that fails BEFORE a controller method runs never
 * reaches that code: a device_id of "abc" cannot be bound to a Long, a date of "31-12-2027"
 * cannot be parsed, a missing query parameter has nothing to bind at all. Spring answered
 * those with its own error page, which is a different shape entirely AND carried a full Java
 * stack trace in the body.
 *
 * Two problems with that. The frontend's `res?.status_code !== 200` check sees undefined and
 * reports "Could not save" with no reason, and the stack trace tells anyone who asks which
 * framework, which version and which classes are in use.
 *
 * So everything below is turned back into the same envelope as every other response, with a
 * message worth showing someone. The detail still goes to the log, where it belongs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** The shared envelope, so these read identically to a controller's own answers. */
	private ResponseEntity<BaseResponseBO> answer(int code, String message) {
		BaseResponseBO body = new BaseResponseBO();
		body.setStatus_code(code);
		body.setMessage(message);
		// HTTP 200 deliberately: the real outcome is status_code, as everywhere else here.
		return new ResponseEntity<>(body, HttpStatus.OK);
	}

	/**
	 * A body Jackson could not read — a number where text belongs, a date in the wrong format,
	 * or malformed JSON.
	 *
	 * The cause is not shown. It names Java classes and field paths, which means nothing to
	 * whoever is looking at the screen and describes the internals to whoever is not.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<BaseResponseBO> unreadable(HttpMessageNotReadableException e) {
		logger.warn("Unreadable request body: {}", e.getMostSpecificCause().getMessage());
		return answer(IResponseCode.INVALID_DATA,
				"Some values were not in the expected format. Check dates and numbers, "
						+ "then try again.");
	}

	/** A query parameter the endpoint needs was not sent at all. */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<BaseResponseBO> missingParam(MissingServletRequestParameterException e) {
		logger.warn("Missing request parameter: {}", e.getParameterName());
		return answer(IResponseCode.INVALID_DATA, "\"" + e.getParameterName() + "\" is required.");
	}

	/** A query parameter that was sent but could not be converted — id=abc, say. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<BaseResponseBO> badParam(MethodArgumentTypeMismatchException e) {
		logger.warn("Bad request parameter {}={}", e.getName(), e.getValue());
		return answer(IResponseCode.INVALID_DATA,
				"\"" + e.getName() + "\" is not valid.");
	}

	/**
	 * The database refused the row — a value too long for its column, a UNIQUE key already
	 * taken, a NOT NULL left empty.
	 *
	 * The services check these themselves and give a better message, so reaching here means
	 * one slipped past. It is logged at error level for that reason: it is a gap to close,
	 * not an expected outcome.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<BaseResponseBO> integrity(DataIntegrityViolationException e) {
		logger.error("Database rejected the row", e);
		return answer(IResponseCode.INVALID_DATA,
				"That could not be saved. A value may be too long, or already in use.");
	}

	/**
	 * Anything still unaccounted for.
	 *
	 * Deliberately says nothing about what went wrong. The log has the stack trace; the
	 * response does not, because whoever triggered it may not be someone to hand it to.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponseBO> unexpected(Exception e) {
		logger.error("Unhandled exception", e);
		return answer(IResponseCode.SERVER_ERROR, IResponseMessage.SERVER_ERROR);
	}
}
