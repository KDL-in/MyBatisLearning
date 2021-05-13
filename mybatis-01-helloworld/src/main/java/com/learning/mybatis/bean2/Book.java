package com.learning.mybatis.bean2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class Book {
    private String isbn;
    private BookStock bookStock;
    private String bookName;
    private double price;
}
