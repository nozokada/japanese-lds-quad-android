package com.nozokada.japaneseldsquad;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.realm.Realm;
import io.realm.RealmResults;

public class ContentFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String PREFS_NAME = "JLQPrefsFile";
    private static final String BASE_ASSET_URL = "file:///android_asset/";

    private Realm realm;

    private ContentWebView webView;
    private String bookId;
    private Book targetBook;
    private int targetChapter;
    private String targetVerse;
    private String targetScriptureId;
    private RealmResults<Scripture> scripturesList;

    private SharedPreferences settings;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private boolean dualEnabled;
    private boolean englishExists = true;

    private boolean hymnsViewed = false;
    private boolean scriptureViewed = true;

    public static ContentFragment newInstance(String id, int chapter, String verse, String scriptureId) {
        ContentFragment contentFragment = new ContentFragment();
        Bundle args = new Bundle();

        args.putString("id", id);
        args.putInt("chapter", chapter);
        args.putString("verse", verse);
        args.putString("scriptureId", scriptureId);

        contentFragment.setArguments(args);

        return contentFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
        settings.registerOnSharedPreferenceChangeListener(this);
        dualEnabled = settings.getBoolean("dualEnabled", false);

        if (savedInstanceState == null) {
            bookId = getArguments().getString("id");
            targetChapter = getArguments().getInt("chapter");
            targetVerse = getArguments().getString("verse");
            targetScriptureId = getArguments().getString("scriptureId");
        }
        else {
            bookId = savedInstanceState.getString("id");
            targetChapter = savedInstanceState.getInt("chapter");
            targetVerse = savedInstanceState.getString("verse");
            targetScriptureId = savedInstanceState.getString("scriptureId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        webView = new ContentWebView(getActivity(), bookId, targetChapter, targetScriptureId);

        BuildContentTask task = new BuildContentTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return webView;
    }

    @Override
    public void onResume() {
        super.onResume();
        settings.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        settings.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("", "onSharedPreferenceChanged with key: " + key);

        if (dualEnabled != sharedPreferences.getBoolean(key, false)) {
            dualEnabled = sharedPreferences.getBoolean(key, false);
            reload();
        }
    }

    public void reload() {
        getFragmentManager().beginTransaction().detach(this).attach(this).commit();
    }

    private class BuildContentTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {

            realm = Realm.getDefaultInstance();

            targetBook = realm.where(Book.class).equalTo("id", bookId).findFirst();

            scripturesList = targetBook.getChild_scriptures().sort("id");

            if (scripturesList != null) {
                if (scripturesList.first().getScripture_eng().equals(""))
                    englishExists = false;
            }

            if (targetBook.getLink().startsWith("gs")) {
                scriptureViewed = false;
            }
            else if (targetBook.getLink().startsWith("hymns")) {
                hymnsViewed = true;
                scriptureViewed = false;
            }
            else if (targetBook.getLink().endsWith("_cont")) {
                scriptureViewed = false;
            }

            StringBuilder pageContents = new StringBuilder();
            RealmResults<Scripture> scriptures = scripturesList.where().equalTo("chapter", targetChapter).findAll().sort("id");

            Scripture title = scriptures.where().equalTo("verse", "title").findFirst();
            if (title != null) {
                pageContents.append("<div class='title'>").append(title.getScripture_jpn()).append("</div>");
                if (dualEnabled && englishExists) {
                    if (hymnsViewed) pageContents.append("<div class='hymn-title'>").append(title.getScripture_eng()).append("</div>");
                    else pageContents.append("<div class='title'>").append(title.getScripture_eng()).append("</div>");
                }
            }

            if (!hymnsViewed) {
                Scripture counter = scriptures.where().equalTo("verse", "counter").findFirst();
                if (counter != null) {
                    pageContents.append("<div class='subtitle'>").append(counter.getScripture_jpn()).append("</div>");
                    if (dualEnabled && englishExists)
                        pageContents.append("<div class='subtitle'>").append(counter.getScripture_eng()).append("</div>");
                }
            }

            Scripture preface = scriptures.where().equalTo("verse", "preface").findFirst();
            if (preface != null) {
                if (dualEnabled && englishExists) { pageContents.append("<hr>"); }
                pageContents.append("<div class='paragraph'>").append(preface.getScripture_jpn()).append("</div>");
                if (dualEnabled && englishExists) pageContents.append("<div class='paragraph'>").append(preface.getScripture_eng()).append("</div>");
            }

            Scripture intro = scriptures.where().equalTo("verse", "intro").findFirst();
            if (intro != null) {
                if (dualEnabled && englishExists) { pageContents.append("<hr>"); }
                pageContents.append("<div class='paragraph'>").append(intro.getScripture_jpn()).append("</div>");
                if (dualEnabled && englishExists) pageContents.append("<div class='paragraph'>").append(intro.getScripture_eng()).append("</div>");
            }

            Scripture summary = scriptures.where().equalTo("verse", "summary").findFirst();
            if (summary != null) {
                if (dualEnabled && englishExists) { pageContents.append("<hr>"); }
                pageContents.append("<div class='paragraph'><i>").append(summary.getScripture_jpn()).append("</i></div>");
                if (dualEnabled && englishExists) pageContents.append("<div class='paragraph'><i>").append(summary.getScripture_eng()).append("</i></div>");
            }

            for(Scripture scripture : scriptures) {
                String verse = "";
                if (scriptureViewed) { verse = scripture.getVerse(); }

                if (scripture.getId().length() == 6) {

                    if (scripture.getVerse().equals(targetVerse))
                        pageContents.append("<a name='anchor'></a>");

                    boolean bookmarked = false;
                    RealmResults<Bookmark> bookmarksFound = realm.where(Bookmark.class).equalTo("id", scripture.getId()).findAll();
                    if (bookmarksFound.size() > 0) bookmarked = true;

                    if (dualEnabled && englishExists) {
                        pageContents.append("<hr>");

                        pageContents.append("<div id='").append(scripture.getId()).append("' ");
                        if (bookmarked) pageContents.append(" class='bookmarked'");
                        pageContents.append(">");

                        if (hymnsViewed) {
                            pageContents.append("<div class='hymn-verse'><ol>").append(scripture.getScripture_jpn()).append("</ol></div>");
                            pageContents.append("<div class='hymn-verse'><ol>").append(scripture.getScripture_eng()).append("</ol></div>");
                        }
                        else {
                            pageContents.append("<div class='verse'><a class='verse-number' href='")
                                    .append(scripture.getId()).append("/bookmark'>").append(verse)
                                    .append("</a> ").append(scripture.getScripture_jpn()).append("</div>");
                            pageContents.append("<div class='verse'><a class='verse-number' href='")
                                    .append(scripture.getId()).append("/bookmark'>").append(verse)
                                    .append("</a> ").append(scripture.getScripture_eng()).append("</div>");
                        }
                    }

                    else {
                        pageContents.append("<div id='").append(scripture.getId()).append("' ");
                        if (bookmarked) pageContents.append(" class='bookmarked'");
                        pageContents.append(">");

                        if (hymnsViewed) {
                            pageContents.append("<div class='hymn-verse'><ol>").append(scripture.getScripture_jpn()).append("</ol></div>");
                        }
                        else {
                            pageContents.append("<div class='verse'><a class='verse-number' href='")
                                    .append(scripture.getId()).append("/bookmark'>").append(verse)
                                    .append("</a> ").append(scripture.getScripture_jpn()).append("</div>");
                        }
                    }
                    pageContents.append("</div>");
                }
            }

            realm.close();

            return pageContents.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            webView.loadDataWithBaseURL(BASE_ASSET_URL, result, "text/html", "utf-8", null);
        }
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("id", getArguments().getString("id"));
        outState.putInt("chapter", getArguments().getInt("chapter"));
        outState.putString("verse", getArguments().getString("verse"));
        outState.putString("scriptureId", getArguments().getString("scriptureId"));
    }
}
