package com.nozokada.japaneseldsquad;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.exceptions.RealmFileException;

public class JLQApplication extends Application {
    private static final String DEFAULT_REALM_FILENAME = "jlq.realm";
    private static final String NEW_DEFAULT_REALM_FILENAME = "jlq_new.realm";
    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String PROPERTY_ID = "UA-113517187-1";
    HashMap<TrackerName, Tracker> trackers = new HashMap<>();

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
            try {
                Realm realmToCopy = Realm.getInstance(defaultConfig);
                configureBundleRealmFile(defaultRealmParentPath, realmToCopy);
            } catch (RealmFileException e) {
                sendAnalytics(e);
                configureBundleRealmFile(defaultRealmParentPath, null);
            }
        }
        else {
            configureBundleRealmFile(defaultRealmParentPath, null);
        }
    }

    public enum TrackerName {
        APP_TRACKER
    }

    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!trackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker tracker = analytics.newTracker(PROPERTY_ID);
            trackers.put(trackerId, tracker);
        }
        return trackers.get(trackerId);
    }

    private void sendAnalytics(Exception e) {
        Tracker tracker = getTracker(TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(new StandardExceptionParser(this, null)
                        .getDescription(Thread.currentThread().getName(), e))
                .setFatal(false)
                .build()
        );
    }

    private void configureBundleRealmFile(String defaultRealmParentPath, Realm realmToCopy) {
        copyBundledRealmFile(getResources().openRawResource(R.raw.jlq), defaultRealmParentPath, NEW_DEFAULT_REALM_FILENAME);
        if (realmToCopy != null) {
            copyUserDataToDefaultRealm(realmToCopy);
            boolean success = new File(defaultRealmParentPath + "/" + DEFAULT_REALM_FILENAME).delete();
        }
        File realmFile = new File(defaultRealmParentPath + "/" + NEW_DEFAULT_REALM_FILENAME);
        boolean success = realmFile.renameTo(new File(defaultRealmParentPath + "/" + DEFAULT_REALM_FILENAME));
    }

    private void copyUserDataToDefaultRealm(Realm realmToCopy) {
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
