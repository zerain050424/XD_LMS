package com.group3.xd_lms;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import com.group3.xd_lms.web.BookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * BookController 测试类
 * 测试queryBookMetaDataInfos方法的各种场景
 */
public class BookControllerTest {

    @Mock
    private BookMetaDataMapper bookMetaDataMapper;

    @Mock
    private BookItemMapper bookItemMapper;

    @InjectMocks
    private BookController bookController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试1：按ISBN查询单本可借阅图书（全量查询模式）
     * 预期：返回该ISBN对应的元数据，包含基础信息（ISBN、书名、作者、分类）
     */
    @Test
    public void testQueryByIsbnFullMode() {
        String testIsbn = "978-7-1234567-8";
        BookMetaData metadata = BookMetaData.builder()
                .isbn(testIsbn)
                .title("Java编程思想")
                .author("Bruce Eckel")
                .category("计算机")
                .keywords("Java,编程")
                .build();

        BookItem availableItem = BookItem.builder()
                .rfidTag("RFID001")
                .isbn(testIsbn)
                .status(BookItem.BookStatus.Available)
                .location("A1-001")
                .build();

        when(bookMetaDataMapper.selectByIsbn(testIsbn)).thenReturn(metadata);
        when(bookItemMapper.selectAvailableByIsbn(testIsbn)).thenReturn(Arrays.asList(availableItem));

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(testIsbn, null, null, null);

        assertEquals(200, result.get("code"));
        assertEquals("查询成功", result.get("message"));
        assertEquals(1, result.get("count"));
        assertNotNull(result.get("data"));
    }

    /**
     * 测试2：按关键词模糊查询，分页模式
     * 预期：返回符合条件的第一页数据，每页2条记录
     */
    @Test
    public void testQueryByKeywordPaginationMode() {
        String keyword = "Java";
        List<BookMetaData> mockMetadata = Arrays.asList(
                BookMetaData.builder()
                        .isbn("978-7-1111111-1")
                        .title("Java核心技术")
                        .author("作者A")
                        .category("计算机")
                        .build(),
                BookMetaData.builder()
                        .isbn("978-7-2222222-2")
                        .title("Java并发编程")
                        .author("作者B")
                        .category("计算机")
                        .build()
        );

        when(bookMetaDataMapper.searchByKeyword(keyword)).thenReturn(mockMetadata);
        when(bookItemMapper.selectAvailableByIsbn("978-7-1111111-1"))
                .thenReturn(Arrays.asList(BookItem.builder().isbn("978-7-1111111-1").status(BookItem.BookStatus.Available).build()));
        when(bookItemMapper.selectAvailableByIsbn("978-7-2222222-2"))
                .thenReturn(Arrays.asList(BookItem.builder().isbn("978-7-2222222-2").status(BookItem.BookStatus.Available).build()));

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, keyword, 1, 2);

        assertEquals(200, result.get("code"));
        assertEquals(2, result.get("count")); // 总共2条
        assertEquals("查询成功", result.get("message"));

        @SuppressWarnings("unchecked")
        List<BookMetaData> data = (List<BookMetaData>) result.get("data");
        assertEquals(2, data.size()); // 第一页返回2条
    }

    /**
     * 测试3：分页查询第二页，每页2条
     * 预期：返回第二页（可能为空或部分数据）
     */
    @Test
    public void testQueryPaginationPage2() {
        String keyword = "Java";
        List<BookMetaData> mockMetadata = Arrays.asList(
                BookMetaData.builder()
                        .isbn("978-7-1111111-1")
                        .title("Java核心技术")
                        .author("作者A")
                        .category("计算机")
                        .build(),
                BookMetaData.builder()
                        .isbn("978-7-2222222-2")
                        .title("Java并发编程")
                        .author("作者B")
                        .category("计算机")
                        .build(),
                BookMetaData.builder()
                        .isbn("978-7-3333333-3")
                        .title("Java虚拟机")
                        .author("作者C")
                        .category("计算机")
                        .build()
        );

        when(bookMetaDataMapper.searchByKeyword(keyword)).thenReturn(mockMetadata);
        for (BookMetaData meta : mockMetadata) {
            when(bookItemMapper.selectAvailableByIsbn(meta.getIsbn()))
                    .thenReturn(Arrays.asList(BookItem.builder().isbn(meta.getIsbn()).status(BookItem.BookStatus.Available).build()));
        }

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, keyword, 2, 2);

        assertEquals(200, result.get("code"));
        assertEquals(3, result.get("count")); // 总共3条

        @SuppressWarnings("unchecked")
        List<BookMetaData> data = (List<BookMetaData>) result.get("data");
        assertEquals(1, data.size()); // 第二页返回1条
    }

    /**
     * 测试4：没有提供查询参数
     * 预期：返回500错误，提示"请提供ISBN或关键词进行搜索"
     */
    @Test
    public void testMissingQueryParameter() {
        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, null, null, null);

        assertEquals(500, result.get("status"));
        assertTrue(result.get("message").toString().contains("请提供ISBN或关键词进行搜索"));
    }

    /**
     * 测试5：分页参数非法（页码为0）
     * 预期：返回500错误，提示"页码和页大小必须大于0"
     */
    @Test
    public void testInvalidPageNum() {
        String keyword = "Java";
        when(bookMetaDataMapper.searchByKeyword(keyword))
                .thenReturn(Arrays.asList(BookMetaData.builder().isbn("978-7-1111111-1").build()));
        when(bookItemMapper.selectAvailableByIsbn("978-7-1111111-1"))
                .thenReturn(Arrays.asList(BookItem.builder().isbn("978-7-1111111-1").status(BookItem.BookStatus.Available).build()));

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, keyword, 0, 10);

        assertEquals(500, result.get("status"));
        assertTrue(result.get("message").toString().contains("页码和页大小必须大于0"));
    }

    /**
     * 测试6：分页参数非法（页大小为负数）
     * 预期：返回500错误，提示"页码和页大小必须大于0"
     */
    @Test
    public void testInvalidPageSize() {
        String keyword = "Java";
        when(bookMetaDataMapper.searchByKeyword(keyword))
                .thenReturn(Arrays.asList(BookMetaData.builder().isbn("978-7-1111111-1").build()));
        when(bookItemMapper.selectAvailableByIsbn("978-7-1111111-1"))
                .thenReturn(Arrays.asList(BookItem.builder().isbn("978-7-1111111-1").status(BookItem.BookStatus.Available).build()));

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, keyword, 1, -5);

        assertEquals(500, result.get("status"));
        assertTrue(result.get("message").toString().contains("页码和页大小必须大于0"));
    }

    /**
     * 测试7：查询结果中没有可借阅的图书
     * 预期：返回空列表，total为0
     */
    @Test
    public void testNoAvailableBooks() {
        String keyword = "Java";
        List<BookMetaData> mockMetadata = Arrays.asList(
                BookMetaData.builder()
                        .isbn("978-7-1111111-1")
                        .title("Java核心技术")
                        .build()
        );

        when(bookMetaDataMapper.searchByKeyword(keyword)).thenReturn(mockMetadata);
        when(bookItemMapper.selectAvailableByIsbn("978-7-1111111-1")).thenReturn(new ArrayList<>()); // 没有可借阅的图书

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, keyword, null, null);

        assertEquals(200, result.get("code"));
        assertEquals(0, result.get("count"));
        @SuppressWarnings("unchecked")
        List<BookMetaData> data = (List<BookMetaData>) result.get("data");
        assertTrue(data.isEmpty());
    }

    /**
     * 测试8：返回的元数据包含必要字段
     * 预期：ISBN、书名、作者、分类字段不为空
     */
    @Test
    public void testReturnedFieldsComplete() {
        String testIsbn = "978-7-1234567-8";
        BookMetaData metadata = BookMetaData.builder()
                .isbn(testIsbn)
                .title("Java编程思想")
                .author("Bruce Eckel")
                .category("计算机")
                .keywords("Java,编程")
                .build();

        when(bookMetaDataMapper.selectByIsbn(testIsbn)).thenReturn(metadata);
        when(bookItemMapper.selectAvailableByIsbn(testIsbn))
                .thenReturn(Arrays.asList(BookItem.builder().isbn(testIsbn).status(BookItem.BookStatus.Available).build()));

        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(testIsbn, null, null, null);

        @SuppressWarnings("unchecked")
        List<BookMetaData> data = (List<BookMetaData>) result.get("data");
        BookMetaData returnedData = data.get(0);

        assertNotNull(returnedData.getIsbn());
        assertNotNull(returnedData.getTitle());
        assertNotNull(returnedData.getAuthor());
        assertNotNull(returnedData.getCategory());
    }

    /**
     * 测试9：全量查询模式（不分页）
     * 预期：返回所有符合条件的图书
     */
    @Test
    public void testFullQueryMode() {
        String keyword = "Java";
        List<BookMetaData> mockMetadata = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            mockMetadata.add(BookMetaData.builder()
                    .isbn("978-7-" + i + "111111-" + i)
                    .title("Java图书" + i)
                    .build());
        }

        when(bookMetaDataMapper.searchByKeyword(keyword)).thenReturn(mockMetadata);
        for (BookMetaData meta : mockMetadata) {
            when(bookItemMapper.selectAvailableByIsbn(meta.getIsbn()))
                    .thenReturn(Arrays.asList(BookItem.builder().isbn(meta.getIsbn()).status(BookItem.BookStatus.Available).build()));
        }

        // 不提供分页参数，应返回全部数据
        HashMap<String, Object> result = bookController.queryBookMetaDataInfos(null, keyword, null, null);

        assertEquals(200, result.get("code"));
        assertEquals(5, result.get("count"));

        @SuppressWarnings("unchecked")
        List<BookMetaData> data = (List<BookMetaData>) result.get("data");
        assertEquals(5, data.size()); // 全部返回
    }

    /**
     * 测试10：空的ISBN或keyword字符串（仅包含空格）
     * 预期：应被视为无效参数
     */
    @Test
    public void testEmptyStringParameter() {
        HashMap<String, Object> result = bookController.queryBookMetaDataInfos("  ", "  ", null, null);

        assertEquals(500, result.get("status"));
        assertTrue(result.get("message").toString().contains("请提供ISBN或关键词进行搜索"));
    }
}
