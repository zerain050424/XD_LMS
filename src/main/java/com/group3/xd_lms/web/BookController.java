package com.group3.xd_lms.web;
import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        System.out.println(list);
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

    // R1(Reader) 在数据库中搜索图书信息 - 支持分页与全量查询
    @GetMapping(value = "BookMetaData/queryInfos")
    public HashMap<String, Object> queryBookMetaDataInfos(
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        //执行查询图书逻辑
        try {
            // 参数清理 - 去除前后空格
            isbn = isbn != null ? isbn.trim() : null;
            keyword = keyword != null ? keyword.trim() : null;

            // 参数校验
            if ((isbn == null || isbn.isEmpty()) && (keyword == null || keyword.isEmpty())) {
                throw new IllegalArgumentException("请提供ISBN或关键词进行搜索");
            }

            // 获取所有符合条件的元数据
            List<BookMetaData> allMetadata;
            if (isbn != null && !isbn.isEmpty()) {
                // 按ISBN查询单本
                BookMetaData singleResult = bookMetadataMapper.selectByIsbn(isbn);
                allMetadata = singleResult != null ? new java.util.ArrayList<>(java.util.Arrays.asList(singleResult)) : new java.util.ArrayList<>();
            } else {
                // 按关键词模糊搜索
                allMetadata = bookMetadataMapper.searchByKeyword(keyword);
            }

            // 获取对应的可借阅实物图书
            List<BookMetaData> availableBooks = new java.util.ArrayList<>();
            for (BookMetaData metadata : allMetadata) {
                // 查询该ISBN下所有Available状态的图书
                List<BookItem> availableItems = bookItemMapper.selectAvailableByIsbn(metadata.getIsbn());
                if (availableItems != null && !availableItems.isEmpty()) {
                    availableBooks.add(metadata);
                }
            }

            // 处理分页
            int total = availableBooks.size();
            List<BookMetaData> result;

            if (pageNum != null && pageSize != null) {
                // 分页查询模式
                if (pageNum <= 0 || pageSize <= 0) {
                    throw new IllegalArgumentException("页码和页大小必须大于0");
                }
                int startIndex = (pageNum - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, total);

                if (startIndex >= total) {
                    result = new java.util.ArrayList<>();
                } else {
                    result = availableBooks.subList(startIndex, endIndex);
                }
            } else {
                // 全量查询模式
                result = availableBooks;
            }

            return Result.getListResultMap(200, "查询成功", total, result);
        } catch (IllegalArgumentException e) {
            return Result.getResultMap(500, e.getMessage());
        } catch (Exception e) {
            return Result.getResultMap(500, "查询图书元数据失败，请稍后重试");
        }
    }

    // TODO R1(Librarian) 在数据库中按状态搜索图书信息
    @GetMapping(value = "BookMetaData/queryStatusInfos")
    public void queryBookMetaDataInfosByStatus(){
        //执行按状态查询图书
    }
    // TODO R1(Librarian) 在数据库中按种类搜索图书信息
    @GetMapping(value = "BookMetaData/queryCategoryInfos")
    public void queryBookMetaDataInfosByCategory(){
        //执行按种类查询图书
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
    // 获取图书详情及实时借阅状态
    @GetMapping("/borrowingstatus/{isbn}")
    public HashMap<String, Object> getBorrowingStatus(@PathVariable String isbn) {
        // 1. 查询图书元数据
        BookMetaData metadata = bookMetadataMapper.selectByIsbn(isbn);
        if (metadata == null) {
            return Result.getResultMap(404, "图书不存在，ISBN: " + isbn);
        }

        // 2. 查询该 ISBN 下的所有实物书
        List<BookItem> items = bookItemMapper.selectByIsbn(isbn);
        int totalCount = items.size();

        // 3. 统计可借数量（利用 BookItem 中的 isAvailable() 方法）
        long availableCount = items.stream().filter(BookItem::isAvailable).count();
        boolean available = availableCount > 0;
        String statusMessage = available ? "可借" : "不可借";

        // 4. 组装返回数据（使用 HashMap，因为你的 Result 工具类支持直接放 data）
        HashMap<String, Object> data = new HashMap<>();
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
}



