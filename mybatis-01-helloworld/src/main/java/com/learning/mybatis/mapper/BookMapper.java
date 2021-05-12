package com.learning.mybatis.mapper;

import com.learning.mybatis.bean.Book;

public interface BookMapper {
    Book selectBook(String isbn);

    boolean insertBook(Book book);

    boolean updateBook(Book book);

    boolean deleteBook(String isbn);
}
