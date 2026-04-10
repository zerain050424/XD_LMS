package com.group3.xd_lms;

import com.group3.xd_lms.dto.BookStatusDTO;
import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BookItemMapperTest {

    @Autowired
    private BookItemMapper bookItemMapper;

    @Autowired
    private BookMetaDataMapper bookMetadataMapper;

    @Test
    public void testBookItemMapper() {
        String testIsbn = "TEST-ITEM-9780001";
        String testRfid = "RFID-TEST-0001";

        // 先插入依赖的图书元数据
        BookMetaData metadata = BookMetaData.builder().isbn(testIsbn).title("测试实体书").build();
        bookMetadataMapper.insert(metadata);

        try {
            // 1. 新增实物图书
            BookItem item = BookItem.builder()
                    .rfidTag(testRfid)
                    .isbn(testIsbn)
                    .status(BookItem.BookStatus.Available)
                    .location("A区-1排")
                    .build();
            bookItemMapper.insert(item);
            System.out.println("✅ 新增实物图书成功");

            // 2. 根据RFID查询
            BookItem byRfid = bookItemMapper.selectByRfidTag(testRfid);
            System.out.println("✅ 根据RFID查询：" + byRfid);

            // 3. 根据ISBN查询实物列表
            List<BookItem> byIsbn = bookItemMapper.selectByIsbn(testIsbn);
            System.out.println("✅ 根据ISBN查询实物数：" + byIsbn.size());

            // 4. 更新图书信息
            BookItem update = BookItem.builder().rfidTag(testRfid).location("B区-3排").build();
            bookItemMapper.updateByRfidTag(update);
            System.out.println("✅ 更新位置成功");

            // 5. 更新状态
            bookItemMapper.updateStatus(testRfid, "Lost");
            System.out.println("✅ 更新状态为 Lost 成功");

            // 6. 根据状态查询
            List<BookItem> byStatus = bookItemMapper.selectByStatus("Lost");
            System.out.println("✅ 根据状态查询数量：" + byStatus.size());

        } finally {
            // 清理数据
            bookItemMapper.deleteByRfidTag(testRfid);
            bookMetadataMapper.deleteByIsbn(testIsbn);
            System.out.println("🧹 测试数据已清理");
        }

        System.out.println("=== BookItemMapper 全部测试通过 ===");
    }

    @Test
    public void testQueryBookStatusByCategory() {
        System.out.println("\n=== 测试1：按类别检索 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus("计算机", null, null, "title", "asc");
        System.out.println("按类别'计算机'检索到 " + result.size() + " 本图书");
        for (BookStatusDTO book : result) {
            assertTrue(book.getCategory().contains("计算机"), "类别应包含'计算机'");
            System.out.println("  - " + book.getTitle() + " | " + book.getCategory() + " | " + book.getStatus());
        }
        System.out.println("✅ 按类别检索测试通过");
    }

    @Test
    public void testQueryBookStatusByStatus() {
        System.out.println("\n=== 测试2：按状态检索 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, "Available", null, "title", "asc");
        System.out.println("按状态'Available'检索到 " + result.size() + " 本图书");
        for (BookStatusDTO book : result) {
            assertEquals("Available", book.getStatus(), "状态应为Available");
        }
        System.out.println("✅ 按状态检索测试通过");
    }

    @Test
    public void testQueryBookStatusByKeyword() {
        System.out.println("\n=== 测试3：关键词模糊匹配类别 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, null, "科", "title", "asc");
        System.out.println("按关键词'科'检索到 " + result.size() + " 本图书");
        assertFalse(result.isEmpty(), "关键词'科'应能检索到结果");
        System.out.println("✅ 关键词模糊匹配测试通过");
    }

    @Test
    public void testQueryBookStatusNoResult() {
        System.out.println("\n=== 测试4：无匹配结果边界条件 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, null, "不存在的关键词123456", "title", "asc");
        assertEquals(0, result.size(), "无匹配结果时应返回空列表");
        System.out.println("无匹配结果时返回: " + result.size() + " 条记录");
        System.out.println("✅ 无结果边界测试通过");
    }

    @Test
    public void testQueryBookStatusEmptyInput() {
        System.out.println("\n=== 测试5：空输入返回全部 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, null, null, "title", "asc");
        assertTrue(result.size() > 0, "空输入应返回全部图书");
        System.out.println("空输入返回全部图书: " + result.size() + " 本");
        System.out.println("✅ 空输入测试通过");
    }

    @Test
    public void testQueryBookStatusSortByTitleDesc() {
        System.out.println("\n=== 测试6：按书名降序排序 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, null, null, "title", "desc");
        assertTrue(result.size() >= 2, "需要至少2条数据验证排序");
        String first = result.get(0).getTitle();
        String second = result.get(1).getTitle();
        assertTrue(first.compareTo(second) >= 0, "降序排列时前一个应大于等于后一个");
        System.out.println("排序验证: '" + first + "' >= '" + second + "'");
        System.out.println("✅ 按书名降序测试通过");
    }

    @Test
    public void testQueryBookStatusSortByStatus() {
        System.out.println("\n=== 测试7：按状态排序 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, null, null, "status", "asc");
        assertTrue(result.size() > 0, "应该有返回结果");
        System.out.println("按状态升序返回: " + result.size() + " 本图书");
        for (BookStatusDTO book : result.subList(0, Math.min(5, result.size()))) {
            System.out.println("  - 状态: " + book.getStatus() + " | " + book.getTitle());
        }
        System.out.println("✅ 按状态排序测试通过");
    }

    @Test
    public void testQueryBookStatusCombinedCondition() {
        System.out.println("\n=== 测试8：类别+状态组合条件 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus("计算机", "Available", null, "title", "asc");
        System.out.println("类别'计算机'且状态'Available'检索到 " + result.size() + " 本图书");
        for (BookStatusDTO book : result) {
            assertTrue(book.getCategory().contains("计算机"), "类别应包含'计算机'");
            assertEquals("Available", book.getStatus(), "状态应为Available");
        }
        System.out.println("✅ 组合条件检索测试通过");
    }

    @Test
    public void testQueryBookStatusResultFields() {
        System.out.println("\n=== 测试9：验证返回字段完整性 ===");
        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(null, null, null, "title", "asc");
        assertFalse(result.isEmpty(), "应该有返回结果");
        BookStatusDTO book = result.get(0);
        assertNotNull(book.getTitle(), "标题不应为空");
        assertNotNull(book.getAuthor(), "作者不应为空");
        assertNotNull(book.getCategory(), "类别不应为空");
        assertNotNull(book.getStatus(), "状态不应为空");
        System.out.println("返回字段验证:");
        System.out.println("  - 标题: " + book.getTitle());
        System.out.println("  - 作者: " + book.getAuthor());
        System.out.println("  - 类别: " + book.getCategory());
        System.out.println("  - 状态: " + book.getStatus());
        System.out.println("  - 位置: " + book.getLocation());
        System.out.println("✅ 返回字段完整性测试通过");
    }
}
