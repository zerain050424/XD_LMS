package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BookMetaDataMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/*********** R1 Version *****************/
@RestController
@RequestMapping(value = "/book")
public class BookController {
    @Autowired
    private final BookItemMapper bookItemMapper;
    private final BookMetaDataMapper bookMetadataMapper;

    public BookController(BookItemMapper bookItemMapper, BookMetaDataMapper bookMetadataMapper) {
        this.bookItemMapper = bookItemMapper;
        this.bookMetadataMapper = bookMetadataMapper;
    }

    // ==========================================
    // 1. 管理图书元数据
    // ==========================================

    /**
     * 获取图书元数据数量
     * URL: GET /book/bookMetaData/getCount
     * 功能：查询数据库中所有图书元数据的总记录数
     *
     * @return 图书总数
     */
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

    /**
     * 查询所有图书信息
     * URL: GET /book/queryAllBookMetaDataInfos
     * 功能：获取数据库中所有的图书元数据列表
     *
     * @return 包含所有图书元数据的列表结果
     */
    @GetMapping(value = "bookMetaData/getAllBookMetaData")
    public HashMap<String, Object> queryAllBookMetaDataInfos() {
        List<BookMetaData> dataList = bookMetadataMapper.selectAll();
        return Result.getListResultMap(200, "Search Success", dataList.size(), dataList);
    }

    /**
     * 搜索图书信息（支持分页）
     * URL: GET /book/BookMetaData/queryInfos
     * 功能：根据ISBN或关键词搜索图书，并返回可借阅的图书列表（仅包含有实物的图书）
     *
     * @param isbn     图书ISBN号（精确查询）
     * @param keyword  搜索关键词（模糊查询，用于标题、作者等）
     * @param pageNum  页码（用于分页查询）
     * @param pageSize 页大小（用于分页查询）
     * @return 符合条件的图书元数据列表及总数
     */
    @GetMapping(value = "BookMetaData/queryInfos")
    public HashMap<String, Object> queryBookMetaDataInfos(
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        // 执行查询图书逻辑
        try {
            // 参数清理 - 去除前后空格
            isbn = isbn != null ? isbn.trim() : null;
            keyword = keyword != null ? keyword.trim() : null;
            System.out.println(isbn);
            // 参数校验
            if ((isbn == null || isbn.isEmpty()) && (keyword == null || keyword.isEmpty())) {
                throw new IllegalArgumentException("Please input ISBN or keyword");
            }

            // 获取所有符合条件的元数据
            List<BookMetaData> allMetadata;
            if (isbn != null && !isbn.isEmpty()) {
                // 按ISBN查询单本
                BookMetaData singleResult = bookMetadataMapper.selectByIsbn(isbn);
                if (singleResult != null) {
                    return Result.getResultMap(200, "Search Success", singleResult);
                }
                else{
                    return Result.getResultMap(500, "Search failed,Dont find this book");
                }
            } else {
                // 按关键词模糊搜索
                allMetadata = bookMetadataMapper.searchByKeyword(keyword);
                return Result.getListResultMap(200, "Search Success",allMetadata.size(), allMetadata);
            }
    } catch (IllegalArgumentException e) {
        return Result.getResultMap(500, e.getMessage());
    } catch (Exception e) {
        return Result.getResultMap(500, "Search failed");
    }
    }

    /**
     * 按关键词搜索图书
     * URL: GET /book/BookMetaData/queryStatusInfos
     * 功能：根据关键词模糊搜索图书元数据
     *
     * @param keyword 搜索关键词
     * @return 匹配的图书列表
     */
    @GetMapping(value = "BookMetaData/queryInfosByKeyword")
    public HashMap<String, Object> queryBookMetaDataInfosByKeyword(@RequestParam String keyword) {
        List<BookMetaData> bookList = bookMetadataMapper.searchByKeyword("%" + keyword + "%");
        return Result.getListResultMap(200, "Search Success", bookList.size(), bookList);
    }

    /**
     * 按种类搜索图书信息
     * URL: GET /book/BookMetaData/queryCategoryInfos
     * 功能：根据图书分类查询所有相关图书
     *
     * @param category 图书分类名称
     * @return 该分类下的图书列表
     */
    @GetMapping(value = "BookMetaData/queryCategoryInfos")
    public HashMap<String, Object> queryBookMetaDataInfosByCategory(@RequestParam String category) {
        List<BookMetaData> bookList = bookMetadataMapper.selectByCategory(category);
        return Result.getListResultMap(200, "Search Success", bookList.size(), bookList);
    }
    /**
     * 按isbn搜索图书信息
     * URL: GET /book/BookMetaData/queryBookInfosByISBN
     * 功能：根据图书isbn码查询图书
     *
     * @param isbn 图书isbn码
     * @return 对应isbn号的图书
     */
    @GetMapping(value = "BookMetaData/queryBookInfosByISBN")
    public HashMap<String, Object> queryBookMetaDataInfosByISBN(@RequestParam String isbn) {
        BookMetaData bookMeta = bookMetadataMapper.selectByIsbn(isbn);
        return Result.getResultMap(200, "Search Success", bookMeta);
    }

    /**
     * 添加图书元数据
     * URL: POST /book/BookMeTaData/addInfos
     * 功能：向数据库中插入新的图书元数据记录
     *
     * @param isbn       图书ISBN (必填)
     * @param title      书名 (必填)
     * @param author     作者
     * @param category   分类
     * @param keywords   关键词
     * @param publisher  出版社
     * @return 操作结果及新插入的图书对象
     */
    @PostMapping(value = "BookMeTaData/addInfos")
    public HashMap<String, Object> addBookMetaDataInfo(
            @RequestParam String isbn,
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String publisher) {
        // 1. 参数校验（去除首尾空格）
        isbn = isbn.trim();
        title = title.trim();
        if (isbn.isEmpty() || title.isEmpty()) {
            return Result.getResultMap(400, "isbn/title cant be empty");
        }

        // 2. 查重
        if (bookMetadataMapper.selectByIsbn(isbn) != null) {
            return Result.getResultMap(409, "The ISBN already exist");
        }

        // 3. 封装对象
        BookMetaData bookMetaData = new BookMetaData();
        bookMetaData.setIsbn(isbn);
        bookMetaData.setTitle(title);
        bookMetaData.setAuthor(author);
        bookMetaData.setCategory(category);
        bookMetaData.setKeywords(keywords);
        bookMetaData.setPublisher(publisher);

        // 4. 执行插入
        int rows = bookMetadataMapper.insert(bookMetaData);
        if (rows > 0) {
            return Result.getResultMap(200, "Add Success", bookMetaData);
        }
        return Result.getResultMap(500, "Add failed");
    }

    /**
     * 删除图书元数据
     * URL: DELETE /book/BookMetaData/deleteInfos
     * 功能：根据ISBN从数据库中删除对应的图书元数据
     *
     * @param isbn 图书ISBN
     * @return 删除结果
     */
    @DeleteMapping(value = "BookMetaData/deleteInfos")
    public HashMap<String, Object> deleteBookMetaDataInfo(@RequestParam String isbn) {
        int rows = bookMetadataMapper.deleteByIsbn(isbn);
        if (rows > 0) {
            return Result.getResultMap(200, "Delete Success");
        } else {
            return Result.getResultMap(500, "Delete Failed,No BookMetaData");
        }
    }

    /**
     * 更新图书元数据
     * URL: PUT /book/BookMetaData/updateInfos
     * 功能：根据ISBN更新图书的详细信息
     *
     * @param isbn       图书ISBN (必填)
     * @param title      书名 (必填)
     * @param author     作者
     * @param category   分类
     * @param keywords   关键词
     * @param publisher  出版社
     * @return 更新结果及更新后的图书对象
     */
    @PutMapping(value = "BookMetaData/updateInfos")
    public HashMap<String, Object> updateBookMetaDataInfo(
            @RequestParam String isbn,
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String publisher) {
        // 1. 封装对象
        BookMetaData bookMetaData = new BookMetaData();
        bookMetaData.setIsbn(isbn);
        bookMetaData.setTitle(title);
        bookMetaData.setAuthor(author);
        bookMetaData.setCategory(category);
        bookMetaData.setKeywords(keywords);
        bookMetaData.setPublisher(publisher);

        // 2. 执行更新
        int rows = bookMetadataMapper.updateByIsbn(bookMetaData);
        if (rows > 0) {
            return Result.getResultMap(200, "Update Success", bookMetaData);
        } else {
            return Result.getResultMap(500, "Update Failed,No BookMetaData");
        }
    }

    // ==========================================
    // 2. 管理图书实体
    // ==========================================

    /**
     * 添加图书实物
     * URL: POST /book/BookItem/addInfos
     * 功能：添加一本具体的图书实物（绑定RFID）
     *
     * @param bookItem 图书实体对象
     * @return 添加结果
     */
    @PostMapping("BookItem/addInfos")
    public HashMap<String, Object> addBookItemInfo(@RequestBody BookItem bookItem) {
        if (bookItem == null || bookItem.getRfidTag() == null) {
            return Result.getResultMap(400, "Params can't be null");
        }
        int rows = bookItemMapper.insert(bookItem);
        return rows > 0 ? Result.getResultMap(200, "Add BookItem Info success") : Result.getResultMap(500, "Add BookItem Info failed");
    }

    /**
     * 删除图书实物
     * URL: DELETE /book/BookItem/deleteInfos
     * 功能：根据RFID标签删除特定的图书实物记录
     *
     * @param rfidTag 图书RFID标签
     * @return 删除结果
     */
    @DeleteMapping("BookItem/deleteInfos")
    public HashMap<String, Object> deleteBookItemInfo(@RequestParam String rfidTag) {
        if (rfidTag == null || rfidTag.isEmpty()) {
            return Result.getResultMap(500, "rfidTag no empty");
        }
         BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        if(Objects.equals(bookItem.getStatus().toString(), "Loaned") || Objects.equals(bookItem.getStatus().toString(), "Reserved")) {
            return Result.getResultMap(500, "The BookItem is already loaned/reserved");
        }
        int rows = bookItemMapper.deleteByRfidTag(rfidTag);
        return rows > 0 ? Result.getResultMap(200, "delete success") : Result.getResultMap(500, "delete failed");
    }

    /**
     * 更新图书实物信息
     * URL: PUT /book/BookItem/updateInfos
     * 功能：更新图书实物的状态或其他属性
     *
     * @param bookItem 图书实体对象
     * @return 更新结果
     */
    @PutMapping("BookItem/updateInfos")
    public HashMap<String, Object> updateBookItemInfo(@RequestBody BookItem bookItem) {
        if (bookItem == null || bookItem.getRfidTag() == null) {
            return Result.getResultMap(400, "Empty parameter");
        }
        int rows = bookItemMapper.updateByRfidTag(bookItem);
        return rows > 0 ? Result.getResultMap(200, "update success") : Result.getResultMap(500, "upate failed");
    }

    /**
     * 根据RFID查询图书实物
     * URL: GET /book/BookItem/getByRfid
     * 功能：通过RFID标签查询特定的图书实物详情
     *
     * @param rfidTag 图书RFID标签
     * @return 图书实物详情
     */
    @GetMapping("BookItem/getByRfid")
    public HashMap<String, Object> getByRfid(@RequestParam String rfidTag) {
        if (rfidTag == null || rfidTag.isEmpty()) {
            return Result.getResultMap(400, "rfidTag Cant be empty");
        }
        BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        return bookItem != null ? Result.getResultMap(200, "Search Success", bookItem) : Result.getResultMap(404, "未找到");
    }

    /**
     * 根据ISBN查询图书实物
     * URL: GET /book/BookItem/getByIsbn
     * 功能：查询某本图书（根据ISBN）的所有实体副本
     *
     * @param isbn 图书ISBN
     * @return 该ISBN对应的所有实物列表
     */
    @GetMapping("BookItem/getByIsbn")
    public HashMap<String, Object> getByIsbn(@RequestParam String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            return Result.getResultMap(400, "isbn Cant be empty");
        }
        List<BookItem> list = bookItemMapper.selectByIsbn(isbn);
        return Result.getListResultMap(200, "Search Success", list.size(), list);
    }

    /**
     * 获取图书详情及实时借阅状态
     * URL: GET /book/borrowingstatus/{isbn}
     * 功能：查询图书的详细信息，并统计当前可借阅的数量和状态
     *
     * @param isbn 图书ISBN路径变量
     * @return 图书详情、总数量、可借数量及状态信息
     */
    @GetMapping("/borrowingstatus/{isbn}")
    public HashMap<String, Object> getBorrowingStatus(@PathVariable String isbn) {
        // 1. 查询图书元数据
        BookMetaData metadata = bookMetadataMapper.selectByIsbn(isbn);
        if (metadata == null) {
            return Result.getResultMap(404, "The BookItem dont exist: " + isbn);
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

        return Result.getResultMap(200, "Search Success", data);
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



