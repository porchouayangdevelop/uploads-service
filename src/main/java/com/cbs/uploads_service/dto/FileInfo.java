package com.cbs.uploads_service.dto;

import lombok.*;

import java.io.Serializable;
import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Data
public class FileInfo implements Serializable {

  private String fileName;
  private long size;
  private String contentType;
  private ZonedDateTime lastModified;
  private String url;
}
