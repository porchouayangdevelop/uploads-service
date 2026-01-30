package com.cbs.uploads_service.response;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@ToString
@Data
public class Response implements Serializable {
  private String fileName;
  private String originalFileName;
  private String contentType;
  private long size;
  private String url;
  private String message;
}
