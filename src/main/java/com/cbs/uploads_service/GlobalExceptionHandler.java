package com.cbs.uploads_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
    log.error("File to large: ", ex.getMessage());

    Map<String, Object> response = new HashMap<>();
    response.put("error", "PAYLOAD_TO_LARGE");
    response.put("message", "File Size exceeds the maximum allowed limit (100MB)");
    response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
    response.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    log.error("Runtime error: {}", ex.getMessage());

    Map<String, Object> response = new HashMap<>();
    response.put("error", "INTERNAL_SERVER_ERROR");
    response.put("message", ex.getMessage());
    response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    response.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
    log.error("Unexpected error: {}", e.getMessage(), e);

    Map<String, Object> response = new HashMap<>();
    response.put("error", "Internal Server Error");
    response.put("message", "An unexpected error occurred");
    response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    response.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
