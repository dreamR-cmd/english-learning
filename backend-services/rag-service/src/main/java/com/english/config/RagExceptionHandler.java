package com.english.config;

import com.english.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class RagExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RagExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBadRequest(IllegalArgumentException error) {
        return ApiResult.fail(HttpStatus.BAD_REQUEST.value(), error.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResult<Void> handleUploadTooLarge(MaxUploadSizeExceededException error) {
        return ApiResult.fail(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Uploaded file is too large");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleUnexpected(Exception error) {
        log.error("RAG request failed", error);
        return ApiResult.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
    }
}
