package com.nozokada.japaneseldsquad;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class Book extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    @Required
    private String name_jpn;

    @Required
    private String name_eng;

    @Required
    private String link;

    private Book parent_book;

    @LinkingObjects("parent_book")
    private final RealmResults<Book> child_books = null;

    @LinkingObjects("parent_book")
    private final RealmResults<Scripture> child_scriptures = null;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName_jpn() {
        return name_jpn;
    }

    public void setName_jpn(String name_jpn) {
        this.name_jpn = name_jpn;
    }

    public String getName_eng() {
        return name_eng;
    }

    public void setName_eng(String name_eng) {
        this.name_eng = name_eng;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Book getParent_book() {
        return parent_book;
    }

    public void setParent_book(Book parent_book) {
        this.parent_book = parent_book;
    }

    public RealmResults<Book> getChild_books() {
        return child_books;
    }

    public RealmResults<Scripture> getChild_scriptures() {
        return child_scriptures;
    }
}
