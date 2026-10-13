package com.repl.asset.basecontroller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.repl.asset.bo.BaseResponseBO;

public class BaseController {

	public ResponseEntity<BaseResponseBO> getResponseModel(Object responseObject, int status_code, String msgText,
			String username) {

		BaseResponseBO baseResponseModel = new BaseResponseBO();

		baseResponseModel.setResponse(responseObject);

		baseResponseModel.setMessage(msgText);

		baseResponseModel.setStatus_code(status_code);

		return new ResponseEntity<BaseResponseBO>(baseResponseModel, HttpStatus.OK);
	
	}
}
