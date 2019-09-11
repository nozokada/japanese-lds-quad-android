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
    private String name_primary;

    @Required
    private String name_secondary;

    private Scripture scripture;

    private Date date;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName_primary() {
        return name_primary;
    }

    public void setName_primary(String name_primary) {
        this.name_primary = name_primary;
    }

    public String getName_secondary() {
        return name_secondary;
    }

    public void setName_secondary(String name_secondary) {
        this.name_secondary = name_secondary;
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
