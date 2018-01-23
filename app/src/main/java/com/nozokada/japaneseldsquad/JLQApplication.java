package com.nozokada.japaneseldsquad;

import android.app.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class JLQApplication extends Application {
    String DEFAULT_REALM_FILENAME = "jlq.realm";
    String TEMPORARY_REALM_FILENAME = "tmp.realm";
    int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        setUpRealm();
    }

    private void setUpRealm() {
        Realm.init(this);
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build());

        String defaultRealmPath = Realm.getDefaultInstance().getPath();
        String defaultRealmParentPath = defaultRealmPath.substring(0, defaultRealmPath.lastIndexOf("/"));
        File defaultRealmFile = new File(defaultRealmParentPath + "/" + DEFAULT_REALM_FILENAME);

        String tmpRealmPath = defaultRealmParentPath + "/" + TEMPORARY_REALM_FILENAME;

        if (defaultRealmFile.exists()) {
            createTemporaryRealmFile(defaultRealmFile, defaultRealmParentPath);

            RealmConfiguration tmpConfig = new RealmConfiguration.Builder().name(TEMPORARY_REALM_FILENAME)
                    .schemaVersion(CURRENT_SCHEMA_VERSION)
                    .migration(new Migration())
                    .build();
            Realm realmToCopy = Realm.getInstance(tmpConfig);

            createNewRealmFromBundleRealmFile(defaultRealmParentPath);
            copyUserDataToDefaultRealm(realmToCopy);
            removeTemporaryRealmFile(tmpRealmPath);
        }
        else {
            createNewRealmFromBundleRealmFile(defaultRealmParentPath);
        }
    }

    private void createTemporaryRealmFile(File defaultRealmFile, String defaultRealmParentPath) {
        try {
            InputStream defaultRealmInputStream = new FileInputStream(defaultRealmFile);
            copyBundledRealmFile(defaultRealmInputStream, defaultRealmParentPath, TEMPORARY_REALM_FILENAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void removeTemporaryRealmFile(String tmpRealmPath) {
        new File(tmpRealmPath).delete();
    }

    private void copyUserDataToDefaultRealm(Realm realmToCopy) {
        final RealmResults<Bookmark> bookmarksToCopy = realmToCopy.where(Bookmark.class).findAll();

        Realm realm = Realm.getDefaultInstance();
        copyUserBookmarks(realm, bookmarksToCopy);

        realm.close();
        realmToCopy.close();
    }

    private void createNewRealmFromBundleRealmFile(String defaultRealmParentPath) {
        copyBundledRealmFile(getResources().openRawResource(R.raw.jlq), defaultRealmParentPath, DEFAULT_REALM_FILENAME);
        RealmConfiguration config = new RealmConfiguration.Builder().name(DEFAULT_REALM_FILENAME)
                .schemaVersion(CURRENT_SCHEMA_VERSION)
                .migration(new Migration())
                .build();
        Realm.setDefaultConfiguration(config);
    }

    private void copyUserBookmarks(Realm realm, final RealmResults<Bookmark> bookmarksToCopy) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (Bookmark bookmarkToCopy : bookmarksToCopy) {
                    Bookmark bookmark = realm.createObject(Bookmark.class, bookmarkToCopy.getId());
                    bookmark.setName_jpn(bookmarkToCopy.getName_jpn());
                    bookmark.setName_eng(bookmarkToCopy.getName_eng());
                    bookmark.setScripture(realm.where(Scripture.class).equalTo("id", bookmark.getId()).findFirst());
                    bookmark.setDate(bookmarkToCopy.getDate());
                }
            }
        });
    }

    private void copyBundledRealmFile(InputStream inputStream, String defaultPath, String outFileName) {
        try {
            File file = new File(defaultPath, outFileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, bytesRead);
            }
            outputStream.close();
//            file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
