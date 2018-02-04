package com.nozokada.japaneseldsquad;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

public class ContentWebView extends WebView {
    private static final String BASE_ASSET_URL = "file:///android_asset/";
    private static final String DEFAULT_FONT = "Hiragino Kaku Gothic ProN";
    private static final String DEFAULT_FONT_COLOR = "rgb(0,0,0)";
    private static final String DEFAULT_BACKGROUND_COLOR = "rgb(255,255,255)";
    private static final int NORMAL = 100;
    private static final int LARGE = 150;
    private static final int XLARGE = 200;

    private FragmentActivity fragmentActivity;
    private ProgressBar spinner;
    private GestureDetector detector;

    private int currentZoomLevel = 100;

    private String currentBookId;
    private int currentChapter;
    private String currentScriptureId;

    public ContentWebView(Context context, String bookId, int chapter, String scriptureId) {
        super(context);

        fragmentActivity = (FragmentActivity) context;
        spinner = fragmentActivity.findViewById(R.id.spinner);
        detector = new GestureDetector(context, new ContentFragmentGestureDetector());

        currentBookId = bookId;
        currentChapter = chapter;
        currentScriptureId = scriptureId;

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });
        setWebViewClient(new ContentWebViewClient());

        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);

        getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data,
                                    String mimeType, String encoding, String failUrl) {
        super.loadDataWithBaseURL(baseUrl, getCSSHeader() + data, mimeType, encoding, failUrl);
    }

    public double getRelativeVerticalScrollValue () {
         return (double) computeVerticalScrollOffset() / computeVerticalScrollRange();
    }

    public void scrollAutomaticallyAfterTextZoomChange(final double relativeVerticalScrollValue) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            spinner.setVisibility(VISIBLE);

            evaluateJavascript("document.documentElement.scrollHeight;", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    int scrollValue = (int) (relativeVerticalScrollValue * Integer.valueOf(value));
                    loadUrl("javascript:window.scrollTo(0, " + String.valueOf(scrollValue) + ");");

                    spinner.setVisibility(GONE);
                }
            });
        }
    }

    private String getCSSHeader() {
        String font = DEFAULT_FONT;
        double fontSize = 1.0;
        double paddingSize = fontSize;
        String fontColor = DEFAULT_FONT_COLOR;
        String backgroundColor = DEFAULT_BACKGROUND_COLOR;

        String bookmarkImageFileName = "bookmark";

//        let nightModeEnabled = UserDefaults.standard.bool(forKey: "nightModeEnabled")
//        if nightModeEnabled {
//            fontColor = "rgb(186,186,186)"
//            backgroundColor = "rgb(33,34,37)"
//        }

//        String image = "<img src='" + bookmarkImageFileName + ".png'/>";

        String headings =
                ".title {" +
                "text-align: center;" +
                "text-transform: uppercase;" +
                "margin-bottom: 15px;" +
                "margin-top: 10px;" +
                "}" +
                ".hymn-title {" +
                "text-align: center;" +
                "margin-bottom: 15px;" +
                "margin-top: 10px;" +
                "}" +
                ".subtitle {" +
                "text-align: center;" +
                "margin-bottom: 15px;" +
                "}";

        String body =
                "body {" +
                "margin: 0;" +
                "padding: " + paddingSize + "em;" +
                "font-family: '" + font + "';" +
                "line-height: 1.4;" +
                "font-size: " + fontSize + "em;" +
                "color: " + fontColor + ";" +
                "background-color: " + backgroundColor + ";" +
                "-webkit-text-size-adjust: none;" +
                "}";

        String verse =
                ".verse {" +
                "padding-bottom: 5px;" +
                "}";

        String verseNumber =
                ".verse-number {" +
                "color: " + fontColor +";" +
                "text-decoration: underline;" +
                "font-weight: bold;" +
                "}";

        String bookmarked =
                ".bookmarked:before {" +
                "background-image: url('" + bookmarkImageFileName + ".png');" +
                "background-size: " + paddingSize + "em " + (paddingSize / 2) + "em;" +
                "display: inline-block;" +
                "width: " + paddingSize + "em;" +
                "height: " + (paddingSize / 2) + "em;" +
                "position: absolute;" +
                "left: 0;" +
                "content: '';" +
                "}";

        String paragraph =
                ".paragraph {" +
                "margin-bottom: 15px;" +
                "}";

        String hymnVerse =
                ".hymn-verse {" +
                "padding: 5px;" +
                "}" +
                ".hymn-verse ol {" +
                "margin: 0 auto;" +
                "width: 80%;" +
                "}";

        String large = ".large {" +
                "font-size: 160%;" +
                "}";

        return "<head>" +
//                image +
                "<style type='text/css'>" +
                headings +
                body +
                verse +
                hymnVerse +
                verseNumber +
                bookmarked +
                paragraph +
                large +
                "</style></head>";
    }

    private class ContentFragmentGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            currentZoomLevel = getSettings().getTextZoom();
            double relativeVerticalScrollValue = getRelativeVerticalScrollValue();

            if (currentZoomLevel == NORMAL)
                getSettings().setTextZoom(LARGE);
            else if (currentZoomLevel == LARGE)
                getSettings().setTextZoom(XLARGE);
            else
                getSettings().setTextZoom(NORMAL);

            scrollAutomaticallyAfterTextZoomChange(relativeVerticalScrollValue);

            return false;
        }
    }

    private class ContentWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            url = url.replace(BASE_ASSET_URL, "");
            String[] path = url.split("/");
            String eventId = path[path.length - 1];

//            Log.d("overrideUrl", Arrays.toString(path));

            if (eventId.equals("bookmark")) {
                addOrRemoveBookmark(path);
            }
            else {
                if (path.length >= 1)
                    jumpToAnotherContent(path);
            }

            return true;
        }

        private void addOrRemoveBookmark(String[] path) {
            String verseId = path[path.length - 2];
            Realm realm = Realm.getDefaultInstance();

            RealmResults<Scripture> scripturesFound = realm.where(Scripture.class).equalTo("id", verseId).findAllSorted("id");
            if (scripturesFound.size() == 0)
                return;

            final Scripture scripture = scripturesFound.first();
            final Bookmark existingBookmark = realm.where(Bookmark.class).equalTo("id", scripture.getId()).findFirst();

            if (existingBookmark != null) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        existingBookmark.deleteFromRealm();
                    }
                });
            }
            else {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Bookmark newBookmark = realm.createObject(Bookmark.class, scripture.getId());
                        newBookmark.setName_jpn(scripture.getParent_book().getName_jpn() + " " + scripture.getChapter() + " : " + scripture.getVerse());
                        newBookmark.setName_eng(scripture.getParent_book().getName_eng() + " " + scripture.getChapter() + " : " + scripture.getVerse());
                        newBookmark.setScripture(scripture);
                        newBookmark.setDate(new Date());
                    }
                });
            }
            updateBookmarkedVerse(scripture.getId());

            realm.close();
        }

        private void jumpToAnotherContent(String[] path) {
            if (path.length == 1) {
                String linkName = path[0];
                Realm realm = Realm.getDefaultInstance();

                RealmResults<Book> booksFound = realm.where(Book.class).equalTo("link", linkName).findAllSorted("id");

                if (booksFound.size() > 0) {
                    Book nextBook = booksFound.last();
                    Intent intent = new Intent(fragmentActivity, ContentActivity.class);
                    intent.putExtra("id", nextBook.getId());
                    intent.putExtra("name", nextBook.getName_jpn());
                    intent.putExtra("chapter", 1);
                    intent.putExtra("verse", "");
                    intent.putExtra("count", 1);

                    fragmentActivity.startActivity(intent);
                }
                realm.close();
            }

            else if (path.length > 1) {
                String linkName = path[0];
                String chapter = path[1];
                String verse = "";

                if (path.length > 2)
                    verse = path[2].substring(path[2].indexOf("#") + 1);

                if (path[0].equals("gs")) {
                    linkName = linkName + "_" + chapter;
                    chapter = verse;
                }

                Realm realm = Realm.getDefaultInstance();
                Book nextBook = realm.where(Book.class).equalTo("link", linkName).findAllSorted("id").last();
                int chaptersCount = nextBook.getChild_scriptures().where().equalTo("verse", "counter").findAllSorted("id").last().getChapter();

                Intent intent = new Intent(fragmentActivity, ContentActivity.class);
                intent.putExtra("id", nextBook.getId());
                intent.putExtra("name", nextBook.getName_jpn());
                intent.putExtra("chapter", Integer.valueOf(chapter));
                intent.putExtra("verse", verse);
                intent.putExtra("count", chaptersCount);

                if (nextBook.getId().equals(currentBookId) && Integer.valueOf(chapter) == currentChapter) {
                    fragmentActivity.finish();
                }

                fragmentActivity.startActivity(intent);

                realm.close();
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
//            Log.d("onPageStarted", currentBookId + " " + currentChapter + " is called");
            spinner.setVisibility(VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            Log.d("onPageFinished", currentBookId + " " + currentChapter + " is called");
            executeJavaScript("window.location.hash = '#anchor';");

            if (currentScriptureId != null)
                highlightFoundVerse(currentScriptureId);

            spinner.setVisibility(GONE);
        }

        private void updateBookmarkedVerse(String verseId) {

            String script = "javascript:" +
                    "var verse = document.getElementById('" + verseId + "');" +
                    "if (verse.className == 'bookmarked') {" +
                    "   verse.removeAttribute('class');" +
                    "}" +
                    "else {" +
                    "   verse.setAttribute('class', 'bookmarked');" +
                    "}";

            executeJavaScript(script);
        }

        private void highlightFoundVerse(String verseId) {

            String script = "javascript:" +
                    "var verse = document.getElementById('" + verseId + "');" +
                    "verse.style.backgroundColor = '#ffff66';" +
                    "verse.style.transition = 'background-color 1s linear';" +
                    "setTimeout(function() {" +
                    "verse.style.background = 'transparent';" +
                    "}, 600);";

            executeJavaScript(script);
        }


        private void executeJavaScript(String script) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                evaluateJavascript(script, null);
            else
                loadUrl(script);
        }
    }
}
