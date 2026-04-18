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
 * 图书荐购实体类 - 对应数据库 book_recommendations 表
 * 存储读者荐购图书的完整信息及审核状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRecommendation {

    /**
     * 荐购记录唯一标识 - 主键
     */
    private Long id;

    /**
     * 荐购读者ID - 外键，关联users表id
     */
    private Long userId;

    /**
     * 关联荐购读者对象 - 多对一关联
     */
    @ToString.Exclude
    private User user;

    /**
     * 荐购图书ISBN（可选）
     */
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

    /**
     * 荐购图书名称
     */
    @NotBlank(message = "图书名称不能为空")
    @Size(max = 255, message = "书名长度不能超过255")
    private String title;

    /**
     * 作者
     */
    @Size(max = 255, message = "作者名称长度不能超过255")
    private String author;

    /**
     * 出版社
     */
    @Size(max = 255, message = "出版社名称长度不能超过255")
    private String publisher;

    /**
     * 荐购理由
     */
    private String reason;

    /**
     * 审核状态
     */
    @Default
    private RecommendationStatus status = RecommendationStatus.Pending;

    /**
     * 处理管理员ID - 外键，关联users表id
     */
    private Long adminId;

    /**
     * 关联管理员对象 - 多对一关联
     */
    @ToString.Exclude
    private User admin;

    /**
     * 管理员反馈意见
     */
    private String feedback;

    /**
     * 创建时间
     */
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 荐购状态枚举
     */
    public enum RecommendationStatus {
        /** 待审核 */
        Pending,
        /** 审核通过 */
        Approved,
        /** 已拒绝 */
        Rejected,
        /** 已采购 */
        Purchased
    }

    // ==================== 业务辅助方法 ====================

    /**
     * 判断是否可编辑（只有待审核状态可编辑）
     */
    public boolean isEditable() {
        return RecommendationStatus.Pending.equals(this.status);
    }

    /**
     * 判断是否可审核（只有待审核状态可审核）
     */
    public boolean isReviewable() {
        return RecommendationStatus.Pending.equals(this.status);
    }
}
