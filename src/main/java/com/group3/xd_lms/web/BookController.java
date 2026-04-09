package com.group3.xd_lms.web;
import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@RestController
@RequestMapping(value = "/book")
public class BookController {
    private final BookItemMapper bookItemMapper;
    private final BookMetaDataMapper bookMetadataMapper;
    public BookController(BookItemMapper bookItemMapper,BookMetaDataMapper bookMetadataMapper) {
        this.bookItemMapper = bookItemMapper;
        this.bookMetadataMapper = bookMetadataMapper;
    }

    // 获取图书元数据数量
    @GetMapping(value = "bookMetaData/getCount")
    public HashMap<String, Object> getCount() {
        // 查询所有图书列表 → 取 size 作为总数
        List<BookMetaData> list = bookMetadataMapper.selectAll();

        if (list == null) {
            return Result.getResultMap(500, "查询失败");
        }

        int count = list.size();
        return Result.getResultMap(200, "查询成功", count);
    }

    // ==========================================
    // 1. 管理图书元数据
    // ==========================================

    // ==========================================
    // Librarian:
    // R1 Target Function:
    // 录入新书基本信息到数据库
    // 修改已有图书信息信息或移除陈旧书籍
    // 检索图书
    // ==========================================

    // 查询所有图书信息
    @GetMapping(value = "BookMetaData/queryAll")
    public void queryAllBookMetaDataInfos(){
    }

    // TODO R1(Reader) 在数据库中搜索图书信息
    @GetMapping(value = "BookMetaData/queryInfos")
    public void queryBookMetaDataInfos(){
        //执行查询图书逻辑
    }

    // TODO R1(Librarian) 在数据库中按状态搜索图书信息
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
    // TODO R1(Librarian) 在数据库中按种类搜索图书信息
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

    // TODO R1(Librarian) 向数据库中添加图书信息
    @PostMapping(value = "BookMeTaData/addInfos")
    public void addBookMetaDataInfo(){
        //执行添加新图书逻辑
    }

    //TODO R1(Librarian) 从数据库中删除图书
    @DeleteMapping(value = "BookMetaData/deleteInfos")
    public void deleteBookMetaDataInfo(){
        // 执行删除图书信息
    }

    //TODO R1(Librarian) 修改已有图书信息
    @PutMapping(value = "BookMetaData/updateInfos")
    public void updateBookMetaDataInfo(){
        // 执行修改图书信息逻辑
    }

    // ==========================================
    // 2. 管理图书实体
    // ==========================================

    // 添加图书实物
    @PostMapping("BookItem/addInfos")
    public HashMap<String, Object> addBookItemInfo(@RequestBody BookItem bookItem) {
        if (bookItem == null || bookItem.getRfidTag() == null) {
            return Result.getResultMap(400, "参数不能为空");
        }
        int rows = bookItemMapper.insert(bookItem);
        return rows > 0 ? Result.getResultMap(200, "添加成功") : Result.getResultMap(500, "添加失败");
    }

    // 删除图书实物
    @DeleteMapping("BookItem/deleteInfos")
    public HashMap<String, Object> deleteBookItemInfo(@RequestParam String rfidTag) {
        if (rfidTag == null || rfidTag.isEmpty()) {
            return Result.getResultMap(400, "rfidTag 不能为空");
        }
        int rows = bookItemMapper.deleteByRfidTag(rfidTag);
        return rows > 0 ? Result.getResultMap(200, "删除成功") : Result.getResultMap(500, "删除失败");
    }

    // 修改图书实物
    @PutMapping("BookItem/updateInfos")
    public HashMap<String, Object> updateBookItemInfo(@RequestBody BookItem bookItem) {
        if (bookItem == null || bookItem.getRfidTag() == null) {
            return Result.getResultMap(400, "参数不能为空");
        }
        int rows = bookItemMapper.updateByRfidTag(bookItem);
        return rows > 0 ? Result.getResultMap(200, "修改成功") : Result.getResultMap(500, "修改失败");
    }

    // 根据RFID查询（补充接口）
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

    // 根据ISBN查询（补充接口）
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
}

