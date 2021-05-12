package com.learning.mybatis.test;

import com.learning.mybatis.bean.Book;
import com.learning.mybatis.mapper.BookMapper;
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
        try(SqlSession sqlSession = sqlSessionFactory.openSession()){
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
}
