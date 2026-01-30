package com.cbs.uploads_service.services;

import com.cbs.uploads_service.config.MinioConfig;
import com.cbs.uploads_service.dto.FileInfo;
import com.cbs.uploads_service.response.Response;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioService {

  private final MinioClient client;
  private final MinioConfig minioConfig;

  @PostConstruct
  public void initialize() {
    createBucketIfNotExists();
  }

  private void createBucketIfNotExists() {
    try {
      boolean found = client.bucketExists(
          BucketExistsArgs.builder()
              .bucket(minioConfig.getBucketName())
              .build()
      );

      if (!found) {
        client.makeBucket(
            MakeBucketArgs.builder()
                .bucket(minioConfig.getBucketName())
                .build()
        );
        log.info("Bucket '{}' created successfully", minioConfig.getBucketName());
      } else {
        log.info("Bucket '{}' already exists", minioConfig.getBucketName());
      }
    } catch (Exception e) {
      log.error("Error creating bucket: {}", e.getMessage());
      throw new RuntimeException("Could not initialize MinIO bucket", e);
    }
  }


  public Response uploadFile(MultipartFile file) {
    return uploadFile(file, null);
  }

  public Response uploadFile(MultipartFile file, String dir) {
    try {
      String originFileName = file.getOriginalFilename();
      String extension = getFileExtension(originFileName);
      String fileName = UUID.randomUUID().toString() + extension;

      if (dir != null && !dir.isEmpty()) {
        dir = dir.replaceAll("^/+|/+$", "");
        fileName = dir + "/" + fileName;
      }

      client.putObject(
          PutObjectArgs.builder()
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .stream(file.getInputStream(), file.getSize(), -1)
              .contentType(file.getContentType())
              .build()
      );

      log.info("File upload successfully: {}", fileName);

      String url = getPresignedUrl(fileName);

      return Response.builder()
          .fileName(fileName)
          .originalFileName(originFileName)
          .contentType(file.getContentType())
          .size(file.getSize())
          .url(url)
          .message("File uploaded successfully")
          .build();

    } catch (Exception e) {
      log.error("Error uploading file: {}", e.getMessage());
      throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
    }
  }


  public List<Response> uploadFiles(MultipartFile[] files, String dir) {
    List<Response> responses = new ArrayList<>();
    for (MultipartFile file : files) {
      responses.add(uploadFile(file, dir));
    }
    return responses;
  }

  public InputStream downloadFile(String fileName) {
    try {
      return client.getObject(
          GetObjectArgs.builder()
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .build()
      );
    } catch (Exception e) {
      log.error("Error downloading file: {}", e.getMessage());
      throw new RuntimeException("Error downloading file: " + e.getMessage(), e);
    }
  }

  public StatObjectResponse getFileInfo(String fileName) {
    try {
      return client.statObject(
          StatObjectArgs.builder()
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .build()
      );
    } catch (Exception e) {
      log.error("Error getting file info: {}", e.getMessage());
      throw new RuntimeException("Error getting file info: " + e.getMessage(), e);
    }
  }

  public void deleteFile(String fileName) {
    try {
      client.removeObject(
          RemoveObjectArgs.builder()
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .build()
      );

      log.info("File deleted successfully: {}", fileName);
    } catch (Exception e) {
      log.error("Error deleting file: {}", e.getMessage());
      throw new RuntimeException("Error deleting file: " + e.getMessage(), e);
    }
  }

  public List<FileInfo> listFiles() {
    return listFiles(null);
  }

  public List<FileInfo> listFiles(String prefix) {
    List<FileInfo> files = new ArrayList<>();
    try {
      ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
          .bucket(minioConfig.getBucketName())
          .recursive(true);

      if (prefix != null && !prefix.isEmpty()) {
        builder.prefix(prefix);
      }

      Iterable<Result<Item>> results = client.listObjects(builder.build());

      for (Result<Item> result : results) {
        Item item = result.get();
        if (!item.isDir()) {
          files.add(FileInfo.builder()
              .fileName(item.objectName())
              .size(item.size())
              .lastModified(item.lastModified())
              .url(getPresignedUrl(item.objectName()))
              .build());
        }
      }
    } catch (Exception e) {
      log.error("Error listing files: {}", e.getMessage());
      throw new RuntimeException("Error listing files: " + e.getMessage(), e);
    }
    return files;
  }

  public String getPresignedUploadUrl(String fileName) {
    try {
      return client.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .expiry(1, TimeUnit.DAYS)
              .build()
      );
    } catch (Exception e) {
      log.error("Error generating presigned upload URL: {}", e.getMessage());
      throw new RuntimeException("Error generating presigned upload URL: " + e.getMessage(), e);
    }
  }

  public boolean fileExists(String fileName) {
    try {
      client.statObject(
          StatObjectArgs.builder()
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .build()
      );
      return true;

    } catch (ErrorResponseException e) {
      log.error("NoSuchKey {}", e.getMessage());
      throw new RuntimeException("Error checking file existence: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error checking file existence: " + e.getMessage(), e);
    }
  }

  public String getPresignedUrl(String fileName) {
    try {
      return client.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(minioConfig.getBucketName())
              .object(fileName)
              .expiry(7, TimeUnit.DAYS)
              .build()
      );
    } catch (Exception e) {
      log.error("Error generating presigned URL: {}", e.getMessage());
      throw new RuntimeException("Error generating presigned URL: " + e.getMessage(), e);
    }
  }

  private String getFileExtension(String originFileName) {
    if (originFileName == null || originFileName.isEmpty()) {
      return "";
    }
    int index = originFileName.lastIndexOf('.');
    return (index == -1) ? "" : originFileName.substring(index);
  }
}
