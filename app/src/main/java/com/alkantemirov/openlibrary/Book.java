package com.alkantemirov.openlibrary;

import android.graphics.Bitmap;
import java.io.File;

public class Book {
    private BookImage image;
    private Bitmap cover;
    private Bitmap thumbnail;
    private File txtFile;

    public Book() {}
    public Book(BookImage image, Bitmap cover, Bitmap thumbnail, File txtFile) {
        this.image = image;
        this.cover = cover;
        this.thumbnail = thumbnail;
        this.txtFile = txtFile;
    }

    public BookImage getImage() {return image;}
    public Bitmap getCover() {return cover;}
    public Bitmap getThumbnail() {return thumbnail;}
    public File getTxtfile() {return txtFile;}
    public void setImage(BookImage value) {image = value;}
    public void setCover(Bitmap value) {cover = value;}
    public void setThumbnail(Bitmap value) {thumbnail = value;}
    public void setTxtFile(File value) {txtFile = value;}
}
