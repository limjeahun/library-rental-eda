package com.example.library.book.application.dto;

import com.example.library.book.domain.model.BookDesc;
import com.example.library.book.domain.model.Classfication;
import com.example.library.book.domain.model.Location;

public record AddBookCommand(String title, BookDesc desc, Classfication classfication, Location location) {
}
