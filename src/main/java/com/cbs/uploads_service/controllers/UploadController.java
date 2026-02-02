package com.cbs.uploads_service.controllers;

import com.cbs.uploads_service.dto.FileInfo;
import com.cbs.uploads_service.response.Response;
import com.cbs.uploads_service.services.MinioService;
import io.minio.StatObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/uploads")
@CrossOrigin(origins = "*", originPatterns = "*", maxAge = 3600)
@RequiredArgsConstructor
@Tag(name = "Upload file API", description = "APIs for managing file")
public class UploadController {

  private final MinioService service;

  /**
   * Upload single file
   * POST /api/files/upload
   */
  @PostMapping("/file")
  @Operation(summary = "Upload single file", description = "upload file", tags = "Upload file API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Successfully upload file",
          content = @io.swagger.v3.oas.annotations.media.Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal Server Error"
      )


  })
  public ResponseEntity<Response> uploadFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "dir", required = false) String dir
  ) {

    if (file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(Response.builder()
              .message("Please select a file to upload")
              .build());
    }

    Response response = service.uploadFile(file, dir);
    return ResponseEntity.ok(response);
  }

  /**
   * Upload multiple files
   * POST /api/files/upload/multiple
   */
  @PostMapping("/files")
  public ResponseEntity<List<Response>> uploadFiles(
      @RequestParam(value = "files") MultipartFile[] files,
      @RequestParam(value = "dir", required = false) String dir
  ) {
    if (files.length == 0) {
      return ResponseEntity.badRequest().build();
    }

    List<Response> res = service.uploadFiles(files, dir);
    return ResponseEntity.ok(res);
  }

  /**
   * Download file
   * GET /api/files/download/{fileName}
   */
  @GetMapping("/download/**")
  public ResponseEntity<byte[]> downloadFile(
      @RequestParam("path") String filePath
  ) {
    try {
      StatObjectResponse stat = service.getFileInfo(filePath);
      InputStream stream = service.downloadFile(filePath);
      byte[] content = IOUtils.toByteArray(stream);
      stream.close();

      String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
      String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(stat.contentType()));
      headers.setContentLength(stat.size());
      headers.setContentDispositionFormData("attachment", encodedFilename);

      return new ResponseEntity<>(content, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("Error downloading file: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }


  /**
   * View file (inline display)
   * GET /api/files/view?path=xxx
   */
  @GetMapping("/view")
  public ResponseEntity<byte[]> viewFile(@RequestParam("path") String filePath) {
    try {
      StatObjectResponse stat = service.getFileInfo(filePath);
      InputStream stream = service.downloadFile(filePath);
      byte[] content = IOUtils.toByteArray(stream);
      stream.close();


      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(stat.contentType()));
      headers.setContentLength(stat.size());
      headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline");

      return new ResponseEntity<>(content, headers, HttpStatus.OK);


    } catch (Exception ex) {
      log.error("Error viewing file: {}", ex.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Delete file
   * DELETE /api/files/delete?path=xxx
   */
  @DeleteMapping("/delete")
  public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("path") String filePath) {
    Map<String, String> res = new HashMap<>();

    try {

      if (!service.fileExists(filePath)) {
        res.put("message", "File Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
      }

      service.deleteFile(filePath);
      res.put("message", "File Deleted successfully");
      res.put("fileName", filePath);
      return ResponseEntity.ok(res);

    } catch (Exception e) {
      log.error("Error deleting file: {}", e.getMessage());
      res.put("message", "Error deleting file: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }
  }

  /**
   * List all files
   * GET /api/files/list
   */
  @GetMapping("/files")
  public ResponseEntity<List<FileInfo>> listFiles(@RequestParam(value = "prefix", required = false) String prefix) {
    List<FileInfo> files = service.listFiles(prefix);

    return ResponseEntity.ok(files);
  }

  /**
   * Get file info/metadata
   * GET /api/files/info?path=xxx
   */
  @GetMapping("/file/info")
  public ResponseEntity<Map<String, Object>> getFileInfo(
      @RequestParam("path") String filePath
  ) {
    try {
      StatObjectResponse stat = service.getFileInfo(filePath);
      String url = service.getPresignedUrl(filePath);

      Map<String, Object> info = new HashMap<>();
      info.put("fileName", filePath);
      info.put("size", stat.size());
      info.put("contentType", stat.contentType());
      info.put("lastModified", stat.lastModified());
      info.put("etag", stat.etag());
      info.put("url", url);

      return ResponseEntity.ok(info);
    } catch (Exception e) {
      log.error("Error getting file info: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Get presigned URL for direct access
   * GET /api/files/presigned-url?path=xxx
   */
  @GetMapping("/presigned-url")
  public ResponseEntity<Map<String, String>> getPresignedUrl(
      @RequestParam("path") String filePath
  ) {
    Map<String, String> res = new HashMap<>();

    try {
      if (!service.fileExists(filePath)) {
        res.put("message", "File Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
      }

      String url = service.getPresignedUrl(filePath);
      res.put("url", url);
      res.put("fileName", filePath);
      return ResponseEntity.ok(res);
    } catch (Exception e) {
      log.error("Error generating presigned URL: {}", e.getMessage());
      res.put("message", "Error generating URL: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }
  }

  /**
   * Get presigned URL for upload (client-side upload)
   * POST /api/files/presigned-upload
   */
  @PostMapping("/presigned-upload")
  public ResponseEntity<Map<String, String>> getPresignedUploadUrl(
      @RequestParam("fileName") String fileName,
      @RequestParam(value = "dir", required = false) String dir) {

    Map<String, String> response = new HashMap<>();

    try {
      String fullPath = fileName;
      if (dir != null && !dir.isEmpty()) {
        dir = dir.replaceAll("^/+|/+$", "");
        fullPath = dir + "/" + fileName;
      }

      String url = service.getPresignedUploadUrl(fullPath);
      response.put("uploadUrl", url);
      response.put("fileName", fullPath);
      response.put("message", "Use PUT request to upload file to this URL");
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error generating presigned upload URL: {}", e.getMessage());
      response.put("message", "Error generating upload URL: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  /**
   * Health check
   * GET /api/files/health
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> healthCheck() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("service", "MinIO Upload Service");
    return ResponseEntity.ok(response);
  }

}
