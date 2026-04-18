package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BookRecommendation;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BookRecommendationMapper;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书荐购控制器
 * 功能：读者提交荐购申请，管理员审核并反馈
 */
@RestController
@RequestMapping("/recommendation")
public class RecommendationController {

    @Autowired
    private BookRecommendationMapper recommendationMapper;

    @Autowired
    private UserMapper userMapper;

    // ==========================================
    // 1. 读者端接口
    // ==========================================

    /**
     * 读者提交荐购申请
     * URL: POST /recommendation/submit
     * 权限: Reader, Librarian, Admin
     *
     * @param recommendation 荐购信息
     * @param session 会话对象
     * @return 提交结果
     */
    @PostMapping("/submit")
    @Transactional
    public HashMap<String, Object> submitRecommendation(
            @RequestBody BookRecommendation recommendation,
            HttpSession session) {

        // 1. 获取当前登录用户
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        // 2. 参数校验
        if (recommendation.getTitle() == null || recommendation.getTitle().trim().isEmpty()) {
            return Result.getResultMap(400, "图书名称不能为空");
        }

        // 3. 设置荐购信息
        recommendation.setUserId(userId);
        recommendation.setStatus(BookRecommendation.RecommendationStatus.Pending);

        // 4. 保存荐购记录
        try {
            int rows = recommendationMapper.insert(recommendation);
            if (rows > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", recommendation.getId());
                data.put("message", "荐购申请已提交，请等待管理员审核");
                return Result.getResultMap(200, "荐购成功", data);
            }
            return Result.getResultMap(500, "荐购失败，请稍后重试");
        } catch (Exception e) {
            return Result.getResultMap(500, "荐购异常：" + e.getMessage());
        }
    }

    /**
     * 读者查询自己的荐购记录
     * URL: GET /recommendation/my-recommendations
     * 权限: Reader, Librarian, Admin
     *
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param status 状态筛选（可选）
     * @param session 会话对象
     * @return 分页结果
     */
    @GetMapping("/my-recommendations")
    public HashMap<String, Object> getMyRecommendations(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            HttpSession session) {

        // 1. 获取当前登录用户
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        // 2. 分页参数校验
        if (pageNum < 1 || pageSize < 1) {
            return Result.getResultMap(400, "页码和每页条数必须大于0");
        }

        // 3. 计算偏移量
        int offset = pageSize * (pageNum - 1);

        // 4. 查询数据
        List<BookRecommendation> list = recommendationMapper.selectByUserIdPage(
                userId, status, pageSize, offset);
        Integer total = recommendationMapper.countByUserId(userId, status);

        return Result.getListResultMap(200, "查询成功", total, list);
    }

    /**
     * 读者查看单条荐购详情
     * URL: GET /recommendation/detail/{id}
     * 权限: 荐购者本人 或 管理员
     *
     * @param id 荐购记录ID
     * @param session 会话对象
     * @return 荐购详情
     */
    @GetMapping("/detail/{id}")
    public HashMap<String, Object> getRecommendationDetail(
            @PathVariable Long id,
            HttpSession session) {

        // 1. 获取当前登录用户
        Long userId = (Long) session.getAttribute("userId");
        Integer roleId = (Integer) session.getAttribute("roleId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        // 2. 查询荐购记录
        BookRecommendation recommendation = recommendationMapper.selectById(id);
        if (recommendation == null) {
            return Result.getResultMap(404, "荐购记录不存在");
        }

        // 3. 权限校验：只有荐购者本人或管理员可以查看
        if (!userId.equals(recommendation.getUserId()) && (roleId == null || roleId == 3)) {
            return Result.getResultMap(403, "无权查看此荐购记录");
        }

        return Result.getResultMap(200, "查询成功", recommendation);
    }

    /**
     * 读者修改待审核的荐购申请
     * URL: PUT /recommendation/update/{id}
     * 权限: 荐购者本人
     *
     * @param id 荐购记录ID
     * @param recommendation 更新的内容
     * @param session 会话对象
     * @return 更新结果
     */
    @PutMapping("/update/{id}")
    public HashMap<String, Object> updateRecommendation(
            @PathVariable Long id,
            @RequestBody BookRecommendation recommendation,
            HttpSession session) {

        // 1. 获取当前登录用户
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        // 2. 查询原记录
        BookRecommendation existing = recommendationMapper.selectById(id);
        if (existing == null) {
            return Result.getResultMap(404, "荐购记录不存在");
        }

        // 3. 权限校验
        if (!userId.equals(existing.getUserId())) {
            return Result.getResultMap(403, "无权修改此荐购记录");
        }

        // 4. 状态校验：只有待审核状态可修改
        if (!existing.isEditable()) {
            return Result.getResultMap(400, "该荐购申请已进入审核流程，无法修改");
        }

        // 5. 执行更新
        recommendation.setId(id);
        int rows = recommendationMapper.updateById(recommendation);
        return rows > 0 ? Result.getResultMap(200, "修改成功")
                : Result.getResultMap(500, "修改失败");
    }

    /**
     * 读者取消/删除荐购申请
     * URL: DELETE /recommendation/cancel/{id}
     * 权限: 荐购者本人（仅限待审核状态）
     *
     * @param id 荐购记录ID
     * @param session 会话对象
     * @return 删除结果
     */
    @DeleteMapping("/cancel/{id}")
    public HashMap<String, Object> cancelRecommendation(
            @PathVariable Long id,
            HttpSession session) {

        // 1. 获取当前登录用户
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }

        // 2. 查询原记录
        BookRecommendation existing = recommendationMapper.selectById(id);
        if (existing == null) {
            return Result.getResultMap(404, "荐购记录不存在");
        }

        // 3. 权限校验
        if (!userId.equals(existing.getUserId())) {
            return Result.getResultMap(403, "无权取消此荐购记录");
        }

        // 4. 状态校验：只有待审核状态可取消
        if (!existing.isEditable()) {
            return Result.getResultMap(400, "该荐购申请已进入审核流程，无法取消");
        }

        // 5. 执行删除
        int rows = recommendationMapper.deleteById(id);
        return rows > 0 ? Result.getResultMap(200, "取消成功")
                : Result.getResultMap(500, "取消失败");
    }

    // ==========================================
    // 2. 管理员端接口
    // ==========================================

    /**
     * 管理员分页查询所有荐购记录
     * URL: GET /recommendation/admin/list
     * 权限: Admin, Librarian
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param status 状态筛选
     * @param keyword 关键词搜索（书名/ISBN/荐购人）
     * @param session 会话对象
     * @return 分页结果
     */
    @GetMapping("/admin/list")
    public HashMap<String, Object> adminGetRecommendationList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpSession session) {

        // 1. 权限校验
        Integer roleId = (Integer) session.getAttribute("roleId");
        if (roleId == null || roleId == 3) {
            return Result.getResultMap(403, "无权限访问");
        }

        // 2. 分页参数校验
        if (pageNum < 1 || pageSize < 1) {
            return Result.getResultMap(400, "页码和每页条数必须大于0");
        }

        // 3. 计算偏移量
        int offset = pageSize * (pageNum - 1);

        // 4. 查询数据
        List<BookRecommendation> list = recommendationMapper.selectAllPage(
                status, keyword, pageSize, offset);
        Integer total = recommendationMapper.countAll(status, keyword);

        return Result.getListResultMap(200, "查询成功", total, list);
    }

    /**
     * 管理员审核荐购申请
     * URL: PUT /recommendation/admin/review/{id}
     * 权限: Admin, Librarian
     *
     * @param id 荐购记录ID
     * @param params 包含 status 和 feedback 的参数
     * @param session 会话对象
     * @return 审核结果
     */
    @PutMapping("/admin/review/{id}")
    @Transactional
    public HashMap<String, Object> reviewRecommendation(
            @PathVariable Long id,
            @RequestBody Map<String, String> params,
            HttpSession session) {

        // 1. 权限校验
        Long adminId = (Long) session.getAttribute("userId");
        Integer roleId = (Integer) session.getAttribute("roleId");
        if (roleId == null || roleId == 3) {
            return Result.getResultMap(403, "无权限执行此操作");
        }

        // 2. 参数校验
        String status = params.get("status");
        String feedback = params.get("feedback");

        if (status == null || status.trim().isEmpty()) {
            return Result.getResultMap(400, "审核状态不能为空");
        }

        // 3. 状态值校验
        BookRecommendation.RecommendationStatus targetStatus;
        try {
            targetStatus = BookRecommendation.RecommendationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Result.getResultMap(400, "无效的审核状态，支持：Approved, Rejected, Purchased");
        }

        // 4. 查询荐购记录
        BookRecommendation recommendation = recommendationMapper.selectById(id);
        if (recommendation == null) {
            return Result.getResultMap(404, "荐购记录不存在");
        }

        // 5. 状态校验
        if (!recommendation.isReviewable()) {
            return Result.getResultMap(400, "该荐购申请已被处理，无法重复审核");
        }

        // 6. 执行审核
        int rows = recommendationMapper.reviewById(id, status, adminId, feedback);
        if (rows > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("status", status);
            data.put("message", "审核完成");
            return Result.getResultMap(200, "审核成功", data);
        }
        return Result.getResultMap(500, "审核失败");
    }

    /**
     * 管理员获取荐购统计信息
     * URL: GET /recommendation/admin/statistics
     * 权限: Admin, Librarian
     *
     * @param session 会话对象
     * @return 统计数据
     */
    @GetMapping("/admin/statistics")
    public HashMap<String, Object> getStatistics(HttpSession session) {
        // 1. 权限校验
        Integer roleId = (Integer) session.getAttribute("roleId");
        if (roleId == null || roleId == 3) {
            return Result.getResultMap(403, "无权限访问");
        }

        // 2. 统计各状态数量
        List<BookRecommendationMapper.StatusCount> statusCounts =
                recommendationMapper.countByStatusGroup();

        // 3. 计算总数
        int total = statusCounts.stream().mapToInt(sc -> sc.getCount()).sum();
        int pendingCount = statusCounts.stream()
                .filter(sc -> "Pending".equals(sc.getStatus()))
                .mapToInt(sc -> sc.getCount())
                .findFirst().orElse(0);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("pendingCount", pendingCount);
        data.put("statusDetails", statusCounts);

        return Result.getResultMap(200, "查询成功", data);
    }

    /**
     * 管理员快速查看待审核列表
     * URL: GET /recommendation/admin/pending
     * 权限: Admin, Librarian
     *
     * @param session 会话对象
     * @return 待审核列表
     */
    @GetMapping("/admin/pending")
    public HashMap<String, Object> getPendingRecommendations(HttpSession session) {
        // 1. 权限校验
        Integer roleId = (Integer) session.getAttribute("roleId");
        if (roleId == null || roleId == 3) {
            return Result.getResultMap(403, "无权限访问");
        }

        // 2. 查询待审核记录
        List<BookRecommendation> list = recommendationMapper.selectByStatus("Pending");

        return Result.getListResultMap(200, "查询成功", list.size(), list);
    }
}