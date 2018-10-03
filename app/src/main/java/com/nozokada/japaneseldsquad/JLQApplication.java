package com.nozokada.japaneseldsquad;

import android.app.Application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class JLQApplication extends Application {
    private static final String DEFAULT_REALM_FILENAME = "jlq.realm";
    private static final String NEW_DEFAULT_REALM_FILENAME = "jlq_new.realm";
    private static final int CURRENT_SCHEMA_VERSION = 1;

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

        RealmConfiguration defaultConfig = new RealmConfiguration.Builder().name(DEFAULT_REALM_FILENAME)
                .schemaVersion(CURRENT_SCHEMA_VERSION)
                .migration(new Migration())
                .build();
        Realm.setDefaultConfiguration(defaultConfig);

        if (defaultRealmFile.exists()) {
            Realm realmToCopy = Realm.getInstance(defaultConfig);
            configureDefaultRealm(defaultRealmParentPath, realmToCopy);
        }
        else {
            configureDefaultRealm(defaultRealmParentPath, null);
        }
    }

    private void configureDefaultRealm(String defaultRealmParentPath, Realm realmToCopy) {
        copyFile(getResources().openRawResource(R.raw.jlq), defaultRealmParentPath, NEW_DEFAULT_REALM_FILENAME);
        if (realmToCopy != null) {
            copyUserDataToNewDefaultRealm(realmToCopy);
            boolean success = new File(defaultRealmParentPath + "/" + DEFAULT_REALM_FILENAME).delete();
        }
        File realmFile = new File(defaultRealmParentPath + "/" + NEW_DEFAULT_REALM_FILENAME);
        boolean success = realmFile.renameTo(new File(defaultRealmParentPath + "/" + DEFAULT_REALM_FILENAME));
    }

    private void copyUserDataToNewDefaultRealm(Realm realmToCopy) {
        final RealmResults<Bookmark> bookmarksToCopy = realmToCopy.where(Bookmark.class).findAll();

        RealmConfiguration config = new RealmConfiguration.Builder().name(NEW_DEFAULT_REALM_FILENAME)
                .schemaVersion(CURRENT_SCHEMA_VERSION)
                .migration(new Migration())
                .build();
        Realm realm = Realm.getInstance(config);
        copyUserBookmarks(realm, bookmarksToCopy);

        realm.close();
        realmToCopy.close();
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

    private void copyFile(InputStream inputStream, String defaultPath, String outFileName) {
        try {
            File file = new File(defaultPath, outFileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, bytesRead);
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
