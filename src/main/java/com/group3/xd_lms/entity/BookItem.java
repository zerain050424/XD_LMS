package com.group3.xd_lms.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
/**
 * 图书实物实体类 - 对应数据库 book_items 表
 * 存储单本实体图书信息（按RFID标签维度，对应"一本书"）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookItem {
    /**
     * RFID标签唯一码 - 主键
     */
    @NotBlank(message = "RFID标签号不能为空")
    @Size(max = 50, message = "RFID标签号长度不能超过50")
    private String rfidTag;

    /**
     * 关联图书ISBN - 外键
     */
    @NotBlank(message = "关联ISBN不能为空")
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

//    /**
//     * 关联图书元数据 - 多对一关联
//     * 用于MyBatis关联查询，@ToString.Exclude 避免循环引用
//     */
//    @ToString.Exclude
//    private BookMetaData metadata;

    /**
     * 图书状态
     */
    @Default
    private BookStatus status = BookStatus.Available;

    /**
     * 馆内架位位置
     */
    @Size(max = 100, message = "馆内位置长度不能超过100")
    private String location;

    /**
     * 图书入库时间
     */
    @Default
    private LocalDateTime addedAt = LocalDateTime.now();

    /**
     * 图书状态枚举
     * 与数据库ENUM字段完全对应
     */
    public enum BookStatus {
        /**
         * 在馆可借
         */
        Available,
        /**
         * 已借出
         */
        Loaned,
        /**
         * 已丢失
         */
        Lost,
        /**
         * 预约中
         */
        Reserved
    }


    /**
     * 判断图书是否可借
     */
    public boolean isAvailable() {
        return BookStatus.Available.equals(this.status);
    }

    /**
     * 判断图书是否已借出
     */
    public boolean isLoaned() {
        return BookStatus.Loaned.equals(this.status);
    }
}
