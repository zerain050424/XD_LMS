package com.group3.xd_lms;

import com.group3.xd_lms.entity.BookRecommendation;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BookRecommendationMapper;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.web.RecommendationController;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RecommendationController 单元测试
 */
public class RecommendationControllerTest {

    @Mock
    private BookRecommendationMapper recommendationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpSession session;

    @InjectMocks
    private RecommendationController controller;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==================== 读者端测试 ====================

    /**
     * 测试1：成功提交荐购申请
     */
    @Test
    public void testSubmitRecommendation_Success() {
        // 1. 准备测试数据
        when(session.getAttribute("userId")).thenReturn(1L);

        BookRecommendation recommendation = BookRecommendation.builder()
                .title("测试图书")
                .isbn("978-7-123-45678-9")
                .author("测试作者")
                .reason("这是一本好书")
                .build();

        when(recommendationMapper.insert(any(BookRecommendation.class))).thenReturn(1);

        // 2. 执行测试
        HashMap<String, Object> result = controller.submitRecommendation(recommendation, session);

        // 3. 验证结果
        assertEquals(200, result.get("status"));
        assertEquals("荐购成功", result.get("message"));
        verify(recommendationMapper, times(1)).insert(any(BookRecommendation.class));
        System.out.println("✅ 测试1通过：提交荐购成功");
    }

    /**
     * 测试2：未登录时提交荐购
     */
    @Test
    public void testSubmitRecommendation_Unauthorized() {
        // 1. 准备测试数据
        when(session.getAttribute("userId")).thenReturn(null);

        BookRecommendation recommendation = BookRecommendation.builder()
                .title("测试图书")
                .build();

        // 2. 执行测试
        HashMap<String, Object> result = controller.submitRecommendation(recommendation, session);

        // 3. 验证结果
        assertEquals(401, result.get("status"));
        assertEquals("请先登录", result.get("message"));
        System.out.println("✅ 测试2通过：未登录拦截");
    }

    /**
     * 测试3：提交时图书名称为空
     */
    @Test
    public void testSubmitRecommendation_EmptyTitle() {
        // 1. 准备测试数据
        when(session.getAttribute("userId")).thenReturn(1L);

        BookRecommendation recommendation = BookRecommendation.builder()
                .title("")  // 空书名
                .build();

        // 2. 执行测试
        HashMap<String, Object> result = controller.submitRecommendation(recommendation, session);

        // 3. 验证结果
        assertEquals(400, result.get("status"));
        assertEquals("图书名称不能为空", result.get("message"));
        System.out.println("✅ 测试3通过：空书名校验");
    }

    /**
     * 测试4：读者查询自己的荐购记录
     */
    @Test
    public void testGetMyRecommendations_Success() {
        // 1. 准备测试数据
        when(session.getAttribute("userId")).thenReturn(1L);

        List<BookRecommendation> mockList = Arrays.asList(
                BookRecommendation.builder().id(1L).title("图书1").status(BookRecommendation.RecommendationStatus.Pending).build(),
                BookRecommendation.builder().id(2L).title("图书2").status(BookRecommendation.RecommendationStatus.Approved).build()
        );

        when(recommendationMapper.selectByUserIdPage(eq(1L), isNull(), eq(10), eq(0)))
                .thenReturn(mockList);
        when(recommendationMapper.countByUserId(eq(1L), isNull())).thenReturn(2);

        // 2. 执行测试
        HashMap<String, Object> result = controller.getMyRecommendations(1, 10, null, session);

        // 3. 验证结果
        assertEquals(200, result.get("status"));
        assertEquals(2, result.get("count"));
        System.out.println("✅ 测试4通过：查询我的荐购成功");
    }

    // ==================== 管理员端测试 ====================

    /**
     * 测试5：管理员审核荐购（通过）
     */
    @Test
    public void testReviewRecommendation_Approve() {
        // 1. 准备测试数据
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("roleId")).thenReturn(1);  // Admin

        BookRecommendation existing = BookRecommendation.builder()
                .id(1L)
                .title("待审核图书")
                .status(BookRecommendation.RecommendationStatus.Pending)
                .build();

        when(recommendationMapper.selectById(1L)).thenReturn(existing);
        when(recommendationMapper.reviewById(eq(1L), eq("Approved"), eq(1L), anyString()))
                .thenReturn(1);

        Map<String, String> params = new HashMap<>();
        params.put("status", "Approved");
        params.put("feedback", "同意采购");

        // 2. 执行测试
        HashMap<String, Object> result = controller.reviewRecommendation(1L, params, session);

        // 3. 验证结果
        assertEquals(200, result.get("status"));
        assertEquals("审核成功", result.get("message"));
        System.out.println("✅ 测试5通过：管理员审核通过");
    }

    /**
     * 测试6：普通读者尝试审核（应被拦截）
     */
    @Test
    public void testReviewRecommendation_ReaderForbidden() {
        // 1. 准备测试数据
        when(session.getAttribute("roleId")).thenReturn(3);  // Reader

        Map<String, String> params = new HashMap<>();
        params.put("status", "Approved");

        // 2. 执行测试
        HashMap<String, Object> result = controller.reviewRecommendation(1L, params, session);

        // 3. 验证结果
        assertEquals(403, result.get("status"));
        assertEquals("无权限执行此操作", result.get("message"));
        System.out.println("✅ 测试6通过：读者无权审核");
    }

    /**
     * 测试7：审核已处理过的荐购
     */
    @Test
    public void testReviewRecommendation_AlreadyReviewed() {
        // 1. 准备测试数据
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("roleId")).thenReturn(1);

        BookRecommendation existing = BookRecommendation.builder()
                .id(1L)
                .status(BookRecommendation.RecommendationStatus.Approved)  // 已审核
                .build();

        when(recommendationMapper.selectById(1L)).thenReturn(existing);

        Map<String, String> params = new HashMap<>();
        params.put("status", "Rejected");

        // 2. 执行测试
        HashMap<String, Object> result = controller.reviewRecommendation(1L, params, session);

        // 3. 验证结果
        assertEquals(400, result.get("status"));
        assertEquals("该荐购申请已被处理，无法重复审核", result.get("message"));
        System.out.println("✅ 测试7通过：防重复审核");
    }

    /**
     * 测试8：获取统计数据
     */
    @Test
    public void testGetStatistics_Success() {
        // 1. 准备测试数据
        when(session.getAttribute("roleId")).thenReturn(1);

        // 2. 执行测试
        HashMap<String, Object> result = controller.getStatistics(session);

        // 3. 验证结果
        assertEquals(200, result.get("status"));
        assertNotNull(result.get("data"));
        System.out.println("✅ 测试8通过：获取统计数据");
    }
}
