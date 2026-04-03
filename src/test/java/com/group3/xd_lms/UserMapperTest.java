package com.group3.xd_lms;

import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    // ==================== 新增用户 ====================
    @Test
    void testInsert1() {
        User user = User.builder()
                .user_account("test001")
                .password("123456")
                .fullName("测试用户1")
                .email("test1@qq.com")
                .roleId(3)
                .build();
        userMapper.insert(user);
        System.out.println("testInsert1 成功：" + user);
    }

    @Test
    void testInsert2() {
        User user = User.builder()
                .user_account("test002")
                .password("123456")
                .fullName("测试用户2")
                .email("test2@qq.com")
                .roleId(2)
                .build();
        userMapper.insert(user);
        System.out.println("testInsert2 成功：" + user);
    }

    @Test
    void testInsert3() {
        User user = User.builder()
                .user_account("test003")
                .password("123456")
                .fullName("测试用户3")
                .email("test3@qq.com")
                .roleId(1)
                .build();
        userMapper.insert(user);
        System.out.println("testInsert3 成功：" + user);
    }

    // ==================== 根据ID查询 ====================
    @Test
    void testSelectById1() {
        User user = userMapper.selectById(1L);
        System.out.println("testSelectById1：" + user);
    }

    @Test
    void testSelectById2() {
        User user = userMapper.selectById(2L);
        System.out.println("testSelectById2：" + user);
    }

    @Test
    void testSelectById3() {
        User user = userMapper.selectById(3L);
        System.out.println("testSelectById3：" + user);
    }

    // ==================== 根据账号查询 ====================
    @Test
    void testSelectByUserAccount1() {
        User user = userMapper.selectByUserAccount("test001");
        System.out.println("testSelectByUserAccount1：" + user);
    }

    @Test
    void testSelectByUserAccount2() {
        User user = userMapper.selectByUserAccount("test002");
        System.out.println("testSelectByUserAccount2：" + user);
    }

    @Test
    void testSelectByUserAccount3() {
        User user = userMapper.selectByUserAccount("test003");
        System.out.println("testSelectByUserAccount3：" + user);
    }

    // ==================== 更新用户 ====================
    @Test
    void testUpdateById1() {
        User user = User.builder().id(1L).fullName("更新姓名1").email("update1@qq.com").build();
        userMapper.updateById(user);
    }

    @Test
    void testUpdateById2() {
        User user = User.builder().id(2L).fullName("更新姓名2").email("update2@qq.com").build();
        userMapper.updateById(user);
    }

    @Test
    void testUpdateById3() {
        User user = User.builder().id(3L).fullName("更新姓名3").email("update3@qq.com").build();
        userMapper.updateById(user);
    }

    // ==================== 修改状态 ====================
    @Test
    void testUpdateStatus1() {
        userMapper.updateStatusById(1L, "Active");
    }

    @Test
    void testUpdateStatus2() {
        userMapper.updateStatusById(2L, "Disabled");
    }

    @Test
    void testUpdateStatus3() {
        userMapper.updateStatusById(3L, "Active");
    }

    // ==================== 修改密码 ====================
    @Test
    void testUpdatePassword1() {
        userMapper.updatePasswordById(1L, "newpwd111");
    }

    @Test
    void testUpdatePassword2() {
        userMapper.updatePasswordById(2L, "newpwd222");
    }

    @Test
    void testUpdatePassword3() {
        userMapper.updatePasswordById(3L, "newpwd333");
    }

    // ==================== 查询全部 ====================
    @Test
    void testSelectAll1() {
        List<User> list = userMapper.selectAll();
        System.out.println("总数1：" + list.size());
    }

    @Test
    void testSelectAll2() {
        List<User> list = userMapper.selectAll();
        list.forEach(System.out::println);
    }

    @Test
    void testSelectAll3() {
        userMapper.selectAll().forEach(u -> System.out.println("用户：" + u.getFullName()));
    }

    // ==================== 根据角色查询 ====================
    @Test
    void testSelectByRoleId1() {
        System.out.println("管理员：" + userMapper.selectByRoleId(1).size());
    }

    @Test
    void testSelectByRoleId2() {
        System.out.println("馆员：" + userMapper.selectByRoleId(2).size());
    }

    @Test
    void testSelectByRoleId3() {
        System.out.println("读者：" + userMapper.selectByRoleId(3).size());
    }

    // ==================== 根据状态查询 ====================
    @Test
    void testSelectByStatus1() {
        System.out.println("正常用户：" + userMapper.selectByStatus("Active").size());
    }

    @Test
    void testSelectByStatus2() {
        System.out.println("禁用用户：" + userMapper.selectByStatus("Disabled").size());
    }

    @Test
    void testSelectByStatus3() {
        userMapper.selectByStatus("Active").forEach(u -> System.out.println("活跃：" + u.getUser_account()));
    }

    // ==================== 模糊搜索 ====================
    @Test
    void testSearch1() {
        System.out.println("搜索 测试：" + userMapper.searchByKeyword("测试").size());
    }

    @Test
    void testSearch2() {
        System.out.println("搜索 001：" + userMapper.searchByKeyword("001").size());
    }

    @Test
    void testSearch3() {
        System.out.println("搜索 更新：" + userMapper.searchByKeyword("更新").size());
    }

    // ==================== 删除用户 ====================
    @Test
    void testDelete1() {
        userMapper.deleteById(99L);
    }

    @Test
    void testDelete2() {
        // userMapper.deleteById(4L);
    }

    @Test
    void testDelete3() {
        // userMapper.deleteById(5L);
    }
}
