package com.learning.mybatis.mapper;

import com.learning.mybatis.bean.Book;
import org.apache.ibatis.annotations.Param;

public interface BookMapper {
    Book selectBook(String isbn);

    boolean insertBook(Book book);

    boolean updateBook(Book book);

    boolean deleteBook(String isbn);

    com.learning.mybatis.bean2.Book selectBookAndStock(String isbn);
    com.learning.mybatis.bean2.Book selectBookStep(String isbn);
}
