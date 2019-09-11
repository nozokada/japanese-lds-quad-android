package com.nozokada.japaneseldsquad;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class Scripture extends RealmObject {

    @PrimaryKey
    @Required
    private String id;

    private int chapter;

    @Required
    private String verse;

    @Required
    private String scripture_primary;

    @Required
    private String scripture_primary_raw;

    @Required
    private String scripture_secondary;

    @Required
    private String scripture_secondary_raw;

    private Book parent_book;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }

    public String getVerse() {
        return verse;
    }

    public void setVerse(String verse) {
        this.verse = verse;
    }

    public String getScripture_primary() {
        return scripture_primary;
    }

    public void setScripture_primary(String scripture_primary) {
        this.scripture_primary = scripture_primary;
    }

    public String getScripture_primary_raw() {
        return scripture_primary_raw;
    }

    public void setScripture_primary_raw(String scripture_primary_raw) {
        this.scripture_primary_raw = scripture_primary_raw;
    }

    public String getScripture_secondary() {
        return scripture_secondary;
    }

    public void setScripture_secondary(String scripture_secondary) {
        this.scripture_secondary = scripture_secondary;
    }

    public String getScripture_secondary_raw() {
        return scripture_secondary_raw;
    }

    public void setScripture_secondary_raw(String scripture_secondary_raw) {
        this.scripture_secondary_raw = scripture_secondary_raw;
    }

    public Book getParent_book() {
        return parent_book;
    }

    public void setParent_book(Book parent_book) {
        this.parent_book = parent_book;
    }
}
