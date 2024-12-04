package com.alkantemirov.openlibrary;

import java.util.ArrayList;

public class AuthorImage {
    public static ArrayList<AuthorImage> authorArrayList = new ArrayList<>();
    private int id;
    private String name;

    public AuthorImage() {}
    public AuthorImage(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {return id;}
    public String getName() {return name;}
    public void setId(int value) {id = value;}
    public void setName(String value) {name = value;}
}
