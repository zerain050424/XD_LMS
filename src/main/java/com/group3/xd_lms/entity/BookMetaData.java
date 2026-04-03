package com.group3.xd_lms.entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图书元数据实体类 - 对应数据库 book_metadata 表
 * 存储图书通用信息（按ISBN维度，对应"一种书"）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookMetaData {
    /**
     * ISBN号 - 主键
     */
    @NotBlank(message = "ISBN不能为空")
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

    /**
     * 书名
     */
    @NotBlank(message = "书名不能为空")
    @Size(max = 255, message = "书名长度不能超过255")
    private String title;

    /**
     * 作者
     */
    @Size(max = 255, message = "作者名称长度不能超过255")
    private String author;

    /**
     * 图书分类
     */
    @Size(max = 100, message = "分类名称长度不能超过100")
    private String category;

    /**
     * 搜索关键词
     */
    private String keywords;

    /**
     * 出版社
     */
    @Size(max = 255, message = "出版社名称长度不能超过255")
    private String publisher;

    /**
     * 信息录入时间
     */
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
