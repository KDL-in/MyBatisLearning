package com.learning.mybatis.test;

import com.learning.mybatis.bean.Book;
import com.learning.mybatis.mapper.BookMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class TEST {
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void init() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Test
    public void hello() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            System.out.println(sqlSession.selectOne("selectBook", "ISBN-001").toString());
        }
    }

    @Test
    public void testItf() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
            System.out.println(mapper.selectBook("ISBN-001")
            );
        }
    }

    @Test
    public void testInsert() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
            System.out.println(mapper.insertBook(new Book("ISBN-053", "B2", 586)));
//            sqlSession.commit();
        }
    }

    @Test
    public void testUpdate() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
//            System.out.println(mapper.updateBook(new Book("ISBN-055", null, 888)));
            System.out.println(mapper.deleteBook("ISBN-055"));
            sqlSession.commit();
        }
    }

    @Test
    public void testSelectBookAndStock() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BookMapper mapper = sqlSession.getMapper(BookMapper.class);
//            System.out.println(mapper.updateBook(new Book("ISBN-055", null, 888)));
            com.learning.mybatis.bean2.Book book = mapper.selectBookStep("ISBN-001");
            book.getBookStock();
//            System.out.println(mapper.selectBookStep("ISBN-001"));

        }
    }
}
