package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.BookRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookRecommendationMapper {

    // ==================== 基础CRUD ====================

    /**
     * 新增荐购记录
     */
    int insert(BookRecommendation recommendation);

    /**
     * 根据ID查询荐购记录（关联查询用户信息）
     */
    BookRecommendation selectById(@Param("id") Long id);

    /**
     * 根据ID更新荐购记录
     */
    int updateById(BookRecommendation recommendation);

    /**
     * 根据ID删除荐购记录
     */
    int deleteById(@Param("id") Long id);

    // ==================== 读者端查询 ====================

    /**
     * 查询某个用户的所有荐购记录
     */
    List<BookRecommendation> selectByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户的荐购记录
     */
    List<BookRecommendation> selectByUserIdPage(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );

    /**
     * 统计用户荐购记录总数
     */
    Integer countByUserId(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    // ==================== 管理员端查询 ====================

    /**
     * 查询所有荐购记录（管理员视图）
     */
    List<BookRecommendation> selectAll();

    /**
     * 分页查询所有荐购记录（支持状态筛选）
     */
    List<BookRecommendation> selectAllPage(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );

    /**
     * 统计荐购记录总数
     */
    Integer countAll(
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    /**
     * 根据状态查询荐购记录
     */
    List<BookRecommendation> selectByStatus(@Param("status") String status);

    // ==================== 审核操作 ====================

    /**
     * 审核荐购记录（更新状态、管理员ID和反馈）
     */
    int reviewById(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("adminId") Long adminId,
            @Param("feedback") String feedback
    );

    // ==================== 统计查询 ====================

    /**
     * 统计各状态的数量
     */
    List<StatusCount> countByStatusGroup();

    /**
     * 状态统计内部类
     */
    class StatusCount {
        private String status;
        private Integer count;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}