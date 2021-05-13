package com.learning.mybatis.mapper;

import com.learning.mybatis.bean2.BookStock;

public interface BookStockMapper {
    BookStock selectBookStock(String isbn);
}
