package com.learning.mybatis.mapper;

import com.learning.mybatis.bean.Book;

public interface BookMapper {
    Book selectBook(String isbn);
}
