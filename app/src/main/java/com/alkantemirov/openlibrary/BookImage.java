package com.alkantemirov.openlibrary;

import java.util.ArrayList;

public class BookImage {
    public static ArrayList<BookImage> bookArrayList = new ArrayList<BookImage>(); //мне слишком лень добавлять ди контейнер в проект
    public static final int thumbnailHeight = 500;
    public static final int thumbnailWidth = 300;
    public static final int coverHeight = 1280;
    public static final int coverWidth = 720;
    private int id;
    private String title;
    private int authorid;
    private String preview;
    private String location;

    public BookImage() {}
    public BookImage(String title, int authorid, String preview, String location, int id) {
        this.title = title;
        this.authorid = authorid;
        this.preview = preview;
        this.location = location;
        this.id = id;
    }

    public int getId() {return id;}
    public String getTitle() {return title;}
    public int getAuthorid() {return authorid;}
    public String getPreview() {return preview;}
    public String getLocation() {return location;}
    public void setId(int value) {id = value;}
    public void setTitle(String value) {title = value;}
    public void setAuthorid(int value) {authorid = value;}
    public void setPreview(String value) {preview = value;}
    public void setLocation(String value) {location = value;}
    public AuthorImage getAuthor() {return AuthorImage.authorArrayList.get(authorid - 1);}
    @Override
    public String toString() { //debug only
        return String.format("%s - %s [prev:%s; location: %s]", title, authorid, preview, location);
    }
}
