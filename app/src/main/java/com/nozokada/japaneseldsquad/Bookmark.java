package com.nozokada.japaneseldsquad;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class Bookmark extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    @Required
    private String name_jpn;

    @Required
    private String name_eng;

    private Scripture scripture;

    private Date date;


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

    public Scripture getScripture() {
        return scripture;
    }

    public void setScripture(Scripture scripture) {
        this.scripture = scripture;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
