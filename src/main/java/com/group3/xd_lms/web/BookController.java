package com.group3.xd_lms.web;
import com.group3.xd_lms.dto.BookStatusDTO;
import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping(value = "/book")
public class BookController {
    private final BookItemMapper bookItemMapper;
    private final BookMetaDataMapper bookMetadataMapper;

    public BookController(BookItemMapper bookItemMapper, BookMetaDataMapper bookMetadataMapper) {
        this.bookItemMapper = bookItemMapper;
        this.bookMetadataMapper = bookMetadataMapper;
    }

    // 获取图书元数据数量
    @GetMapping(value = "bookMetaData/getCount")
    public HashMap<String, Object> getCount() {
        List<BookMetaData> list = bookMetadataMapper.selectAll();

        if (list == null) {
            return Result.getResultMap(500, "查询失败");
        }

        int count = list.size();
        return Result.getResultMap(200, "查询成功", count);
    }

    @GetMapping(value = "BookMetaData/queryAll")
    public void queryAllBookMetaDataInfos(){
    }

    @GetMapping(value = "BookMetaData/queryInfos")
    public void queryBookMetaDataInfos(){
    }

    @GetMapping(value = "BookMetaData/queryStatusInfos")
    public HashMap<String, Object> queryBookMetaDataInfosByStatus(@RequestParam String status){
        if (status == null || status.trim().isEmpty()) {
            return Result.getResultMap(400, "status 不能为空");
        }

        String normalizedStatus = normalizeBookStatus(status);
        if (normalizedStatus == null) {
            return Result.getResultMap(400, "不支持的状态，请使用：Available/Loaned/Lost/Reserved 或 对应中文状态");
        }

        List<BookItem> items = bookItemMapper.selectByStatus(normalizedStatus);
        if (items == null || items.isEmpty()) {
            return Result.getListResultMap(404, "未找到图书", 0, new ArrayList<>());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (BookItem item : items) {
            BookMetaData metadata = bookMetadataMapper.selectByIsbn(item.getIsbn());
            Map<String, Object> row = new HashMap<>();
            row.put("rfidTag", item.getRfidTag());
            row.put("isbn", item.getIsbn());
            row.put("status", item.getStatus());
            row.put("location", item.getLocation());
            if (metadata != null) {
                row.put("title", metadata.getTitle());
                row.put("author", metadata.getAuthor());
                row.put("category", metadata.getCategory());
            }
            result.add(row);
        }
        return Result.getListResultMap(200, "查询成功", result.size(), result);
    }

    @GetMapping(value = "BookMetaData/queryCategoryInfos")
    public HashMap<String, Object> queryBookMetaDataInfosByCategory(@RequestParam String category){
        if (category == null || category.trim().isEmpty()) {
            return Result.getResultMap(400, "category 不能为空");
        }

        List<BookMetaData> list = bookMetadataMapper.selectByCategory(category.trim());
        if (list == null || list.isEmpty()) {
            return Result.getListResultMap(404, "未找到图书", 0, new ArrayList<>());
        }

        return Result.getListResultMap(200, "查询成功", list.size(), list);
    }

    @GetMapping("/BookMetaData/queryByIsbn")
    public HashMap<String, Object> queryByIsbn(@RequestParam String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return Result.getResultMap(400, "ISBN不能为空");
        }

        BookMetaData metadata = bookMetadataMapper.selectByIsbn(isbn);
        if (metadata == null) {
            return Result.getResultMap(404, "未找到该ISBN的图书信息");
        }

        List<BookItem> allItems = bookItemMapper.selectByIsbn(isbn);
        List<BookItem> availableItems = bookItemMapper.selectAvailableByIsbn(isbn);

        int totalCount = allItems != null ? allItems.size() : 0;
        int availableCount = availableItems != null ? availableItems.size() : 0;
        boolean available = availableCount > 0;
        String statusMessage = available ? "可借" : "无库存";

        Map<String, Object> data = new HashMap<>();
        data.put("isbn", metadata.getIsbn());
        data.put("title", metadata.getTitle());
        data.put("author", metadata.getAuthor());
        data.put("category", metadata.getCategory());
        data.put("keywords", metadata.getKeywords());
        data.put("publisher", metadata.getPublisher());
        data.put("available", available);
        data.put("availableCount", availableCount);
        data.put("totalCount", totalCount);
        data.put("statusMessage", statusMessage);

        return Result.getResultMap(200, "查询成功", data);
    }

    @PostMapping(value = "BookMeTaData/addInfos")
    public void addBookMetaDataInfo(){
    }

    @DeleteMapping(value = "BookMetaData/deleteInfos")
    public void deleteBookMetaDataInfo(){
    }

    @PutMapping(value = "BookMetaData/updateInfos")
    public void updateBookMetaDataInfo(){
    }

    @PostMapping("BookItem/addInfos")
    public HashMap<String, Object> addBookItemInfo(@RequestBody BookItem bookItem) {
        if (bookItem == null || bookItem.getRfidTag() == null) {
            return Result.getResultMap(400, "参数不能为空");
        }
        int rows = bookItemMapper.insert(bookItem);
        return rows > 0 ? Result.getResultMap(200, "添加成功") : Result.getResultMap(500, "添加失败");
    }

    @DeleteMapping("BookItem/deleteInfos")
    public HashMap<String, Object> deleteBookItemInfo(@RequestParam String rfidTag) {
        if (rfidTag == null || rfidTag.isEmpty()) {
            return Result.getResultMap(400, "rfidTag 不能为空");
        }
        int rows = bookItemMapper.deleteByRfidTag(rfidTag);
        return rows > 0 ? Result.getResultMap(200, "删除成功") : Result.getResultMap(500, "删除失败");
    }

    @PutMapping("BookItem/updateInfos")
    public HashMap<String, Object> updateBookItemInfo(@RequestBody BookItem bookItem) {
        if (bookItem == null || bookItem.getRfidTag() == null) {
            return Result.getResultMap(400, "参数不能为空");
        }
        int rows = bookItemMapper.updateByRfidTag(bookItem);
        return rows > 0 ? Result.getResultMap(200, "修改成功") : Result.getResultMap(500, "修改失败");
    }

    @GetMapping("BookItem/getByRfid")
    public HashMap<String, Object> getByRfid(@RequestParam String rfidTag) {
        if (rfidTag == null || rfidTag.isEmpty()) {
            return Result.getResultMap(400, "rfidTag 不能为空");
        }
        BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        return bookItem != null ?
                Result.getResultMap(200, "查询成功", bookItem) :
                Result.getResultMap(404, "未找到");
    }

    @GetMapping("BookItem/getByIsbn")
    public HashMap<String, Object> getByIsbn(@RequestParam String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            return Result.getResultMap(400, "isbn 不能为空");
        }
        List<BookItem> list = bookItemMapper.selectByIsbn(isbn);
        return Result.getListResultMap(200, "查询成功", list.size(), list);
    }

    private String normalizeBookStatus(String inputStatus) {
        String value = inputStatus.trim().toLowerCase(Locale.ROOT);
        if ("在馆".equals(value) || "available".equals(value)) {
            return "Available";
        }
        if ("已借出".equals(value) || "checked out".equals(value) || "loaned".equals(value)) {
            return "Loaned";
        }
        if ("遗失".equals(value) || "丢失".equals(value) || "lost".equals(value)) {
            return "Lost";
        }
        if ("预约".equals(value) || "预约中".equals(value) || "reserved".equals(value)) {
            return "Reserved";
        }
        return null;
    }

    @GetMapping("/queryBookStatus")
    public HashMap<String, Object> queryBookStatus(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "title") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder) {

        String normalizedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            normalizedStatus = normalizeBookStatus(status);
            if (normalizedStatus == null) {
                return Result.getResultMap(400, "不支持的状态，请使用：在馆/已借出/遗失/预约 或 Available/Loaned/Lost/Reserved");
            }
        }

        List<BookStatusDTO> result = bookItemMapper.queryBookStatus(
                category,
                normalizedStatus,
                keyword,
                sortBy,
                sortOrder
        );

        if (result == null || result.isEmpty()) {
            return Result.getListResultMap(404, "未找到符合条件的图书", 0, new ArrayList<>());
        }

        return Result.getListResultMap(200, "查询成功", result.size(), result);
    }
}
