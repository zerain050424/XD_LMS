package com.group3.xd_lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookStatusDTO {
    private String rfidTag;
    private String isbn;
    private String title;
    private String author;
    private String category;
    private String status;
    private String location;
}
