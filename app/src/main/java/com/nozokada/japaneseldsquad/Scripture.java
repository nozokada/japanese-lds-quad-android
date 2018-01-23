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
    private String scripture_jpn;

    @Required
    private String scripture_jpn_search;

    @Required
    private String scripture_eng;

    @Required
    private String scripture_eng_search;

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

    public String getScripture_jpn() {
        return scripture_jpn;
    }

    public void setScripture_jpn(String scripture_jpn) {
        this.scripture_jpn = scripture_jpn;
    }

    public String getScripture_jpn_search() {
        return scripture_jpn_search;
    }

    public void setScripture_jpn_search(String scripture_jpn_search) {
        this.scripture_jpn_search = scripture_jpn_search;
    }

    public String getScripture_eng() {
        return scripture_eng;
    }

    public void setScripture_eng(String scripture_eng) {
        this.scripture_eng = scripture_eng;
    }

    public String getScripture_eng_search() {
        return scripture_eng_search;
    }

    public void setScripture_eng_search(String scripture_eng_search) {
        this.scripture_eng_search = scripture_eng_search;
    }

    public Book getParent_book() {
        return parent_book;
    }

    public void setParent_book(Book parent_book) {
        this.parent_book = parent_book;
    }
}
