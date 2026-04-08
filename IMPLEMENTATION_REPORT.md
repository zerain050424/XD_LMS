# BookController.queryBookMetaDataInfos 接口实现文档

## 项目概览

完成了 Reader 端图书查询接口 `BookMetaData/queryInfos` 的开发，支持：
- 按ISBN或关键词查询
- 分页和全量查询两种模式
- 仅返回可借阅的图书
- 完整的参数校验和异常处理

---

## 接口设计

### 请求信息

**端点：** `GET /book/BookMetaData/queryInfos`

**请求参数：**

| 参数名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| isbn | String | 否 | 按ISBN查询，与keyword二选一 |
| keyword | String | 否 | 按关键词查询，与isbn二选一 |
| pageNum | Integer | 否 | 页码（从1开始），与pageSize配对使用 |
| pageSize | Integer | 否 | 页大小，与pageNum配对使用 |

**示例请求：**
```
# 全量查询 - 按ISBN
GET /book/BookMetaData/queryInfos?isbn=978-7-1234567-8

# 全量查询 - 按关键词
GET /book/BookMetaData/queryInfos?keyword=Java

# 分页查询 - 第1页，每页10条
GET /book/BookMetaData/queryInfos?keyword=Java&pageNum=1&pageSize=10

# 分页查询 - 第2页，每页5条
GET /book/BookMetaData/queryInfos?isbn=978-7-1111111-1&pageNum=2&pageSize=5
```

---

## 响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "查询成功",
  "count": 5,
  "data": [
    {
      "isbn": "978-7-1111111-1",
      "title": "Java核心技术",
      "author": "作者A",
      "category": "计算机",
      "keywords": "Java,编程",
      "publisher": "出版社"
    },
    {
      "isbn": "978-7-2222222-2",
      "title": "Java并发编程",
      "author": "作者B",
      "category": "计算机",
      "keywords": "并发",
      "publisher": "出版社"
    }
  ]
}
```

### 错误响应

**缺少必要参数：**
```json
{
  "status": 500,
  "message": "请提供ISBN或关键词进行搜索",
  "timestamp": 1712233533000
}
```

**分页参数非法：**
```json
{
  "status": 500,
  "message": "页码和页大小必须大于0",
  "timestamp": 1712233533000
}
```

**系统异常：**
```json
{
  "status": 500,
  "message": "查询图书元数据失败，请稍后重试",
  "timestamp": 1712233533000
}
```

---

## 功能特性

### 1. 双模式查询

**全量查询模式：**
- 不提供 pageNum 和 pageSize 参数
- 返回所有符合条件的可借阅图书

**分页查询模式：**
- 提供 pageNum 和 pageSize 参数
- 按指定的页码和页大小返回数据
- count 表示符合条件的总数，不是当前页数量

### 2. 搜索方式

**按ISBN查询：**
- 精确查询单本图书
- 如果图书存在且有可借阅副本，返回该图书信息

**按关键词查询：**
- 模糊搜索：书名、作者、关键词、出版社等字段
- 返回所有符合条件且有可借阅副本的图书

### 3. 可借阅过滤

- **核心逻辑：** 只返回 BookItem.status = 'Available' 的图书元数据
- 即使某个ISBN有多本实物图书，只要其中有一本可借阅，就返回该元数据

### 4. 参数校验

- ISBN 和 keyword 二选一（都提供时 ISBN 优先）
- 参数自动 trim() 处理空格
- 分页参数必须 > 0
- 出界分页返回空列表

---

## 技术实现

### 核心类和方法

**BookController.java**
```java
public HashMap<String, Object> queryBookMetaDataInfos(
    @RequestParam(required = false) String isbn,
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Integer pageNum,
    @RequestParam(required = false) Integer pageSize)
```

**BookMetaDataMapper.java** (已有)
- `selectByIsbn(String isbn)` - 按ISBN查询单本
- `searchByKeyword(String keyword)` - 按关键词模糊搜索

**BookItemMapper.java** (新增)
- `selectAvailableByIsbn(String isbn)` - 查询ISBN下所有Available的实物

### 执行流程

1. **参数清理** - trim() 去除前后空格
2. **参数校验** - 检查是否提供了isbn或keyword
3. **元数据查询** - 调用mapper查询符合条件的图书元数据
4. **可借阅过滤** - 遍历元数据，检查是否有Available的实物图书
5. **分页处理** - 按pageNum和pageSize分页或全量返回
6. **响应封装** - 使用Result.getListResultMap()返回统一格式

---

## 修改的文件清单

### 1. 新增/修改的类

| 文件路径 | 修改说明 |
|---------|---------|
| `src/main/java/com/group3/xd_lms/web/BookController.java` | 实现queryBookMetaDataInfos方法 |
| `src/main/java/com/group3/xd_lms/mapper/BookItemMapper.java` | 新增selectAvailableByIsbn方法 |
| `src/main/resources/mapper/BookItemMapper.xml` | 新增selectAvailableByIsbn SQL映射 |
| `src/test/java/com/group3/xd_lms/BookControllerTest.java` | 新增10个单元测试 |

### 2. 无需修改的已有类

- `BookMetaData.java` - 已有必要字段(isbn, title, author, category)
- `BookItem.java` - 已有Available状态
- `Result.java` - 已有getListResultMap()方法
- `BookMetaDataMapper.java` - 已有selectByIsbn()和searchByKeyword()

---

## 测试覆盖

### 已实现的测试用例（10个）

| 用例 | 测试场景 | 期望结果 |
|------|---------|--------|
| Test 1 | 按ISBN查询全量模式 | 返回单本元数据 |
| Test 2 | 按关键词查询分页模式 | 返回第1页数据 |
| Test 3 | 分页查询第2页 | 返回部分数据 |
| Test 4 | 缺少查询参数 | 返回500错误 |
| Test 5 | pageNum为0 | 返回500错误 |
| Test 6 | pageSize为负 | 返回500错误 |
| Test 7 | 没有可借阅图书 | 返回空列表 |
| Test 8 | 返回字段完整性 | isbn, title, author, category都有 |
| Test 9 | 全量查询模式 | 返回所有符合条件图书 |
| Test 10 | 空格字符串参数 | 返回500错误 |

### 测试运行结果

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 自测验证清单

- [x] 接口代码无语法错误，编译通过
- [x] 参数校验功能正常
- [x] 按ISBN精确查询能正确返回数据
- [x] 按关键词模糊搜索能正确返回数据
- [x] 全量查询模式正确（不提供分页参数）
- [x] 分页查询模式正确（提供分页参数）
- [x] 分页边界情况处理正确（超出范围返回空）
- [x] 可借阅过滤逻辑正确（只返回Available的图书）
- [x] 返回结果包含必要字段（ISBN、书名、作者、分类）
- [x] 异常处理完整（参数异常、系统异常）
- [x] 返回格式符合Result统一规范
- [x] 单元测试全部通过（10/10）

---

## 数据库查询优化

### 新增的Mapper方法

**BookItemMapper.java:**
```java
// 查询某ISBN下所有Available状态的实物
List<BookItem> selectAvailableByIsbn(@Param("isbn") String isbn);
```

**BookItemMapper.xml:**
```xml
<select id="selectAvailableByIsbn" resultMap="BaseResultMap">
    SELECT * FROM book_items WHERE isbn = #{isbn} AND status = 'Available'
</select>
```

### 查询性能

- 使用数据库层面的状态过滤，避免应用层全表扫描
- 索引优化：book_items表的isbn和status字段应建立复合索引

---

## 使用示例

### cURL请求示例

```bash
# 全量查询 - 按ISBN
curl "http://localhost:8080/book/BookMetaData/queryInfos?isbn=978-7-1234567-8"

# 全量查询 - 按关键词
curl "http://localhost:8080/book/BookMetaData/queryInfos?keyword=Java编程"

# 分页查询 - 每页10条
curl "http://localhost:8080/book/BookMetaData/queryInfos?keyword=Java&pageNum=1&pageSize=10"
```

### 前端调用示例

```javascript
// 全量查询
fetch('/book/BookMetaData/queryInfos?keyword=Java')
  .then(r => r.json())
  .then(data => {
    console.log('总数:', data.count);
    console.log('数据:', data.data);
  });

// 分页查询
fetch('/book/BookMetaData/queryInfos?keyword=Java&pageNum=1&pageSize=10')
  .then(r => r.json())
  .then(data => {
    const totalPages = Math.ceil(data.count / 10);
    console.log('第1页, 共' + totalPages + '页');
    console.log('当前返回' + data.data.length + '条记录');
  });
```

---

## 需要的后续工作（可选）

1. **性能优化**
   - 在book_items表添加(isbn, status)复合索引
   - 考虑缓存热点数据

2. **功能扩展**
   - 支持按分类、作者等字段单独筛选
   - 支持排序（如按新书优先）
   - 支持结合多个搜索条件的高级搜索

3. **API文档**
   - 集成Swagger/Knife4j自动生成API文档

---

## 技术栈

- Spring Boot 3.4.2
- MyBatis 3.0.4
- MySQL 8.0
- JUnit 5 + Mockito
- Lombok

---

## 注意事项

1. **分页的total含义**：total 是所有符合条件的图书总数，**不是当前页的条数**
2. **参数优先级**：当同时提供isbn和keyword时，isbn优先
3. **状态过滤**：接口返回的图书只包含`BookItem.status = 'Available'`的元数据
4. **容错处理**：所有参数都是可选的，但至少需要提供isbn或keyword之一

