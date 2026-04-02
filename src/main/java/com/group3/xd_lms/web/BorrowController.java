package com.group3.xd_lms.web;
import com.group3.xd_lms.entity.BorrowRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/borrow")
public class BorrowController {
    // 分页查询借阅 params: {page, limit, userid, bookid}
    @RequestMapping(value = "/queryBorrowsByPage")
    public void queryBorrowsByPage(@RequestParam Map<String, Object> params){
    }


    // 借书
    @RequestMapping(value = {"/borrowBook", "/reader/borrowBook"})
    @Transactional
    public void borrowBook(){
       //扫描RFID码进行借书
    }

    // 还书
    @RequestMapping(value = {"/returnBook", "/reader/returnBook"})
    @Transactional
    public void returnBook(){
        //通过扫描RFID码进行还书
    }

}
