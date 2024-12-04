package com.alkantemirov.openlibrary;

import androidx.lifecycle.ViewModel;

public class BookViewModel extends ViewModel {
    private BookImage book;

    public BookImage getBook() {return book;}
    public void setBook(BookImage value) {book = value;}
}
