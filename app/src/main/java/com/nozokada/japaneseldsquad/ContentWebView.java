package com.nozokada.japaneseldsquad;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.fragment.app.FragmentActivity;
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

import static java.lang.Math.sqrt;

public class ContentWebView extends WebView {
    private FragmentActivity fragmentActivity;
    private ProgressBar spinner;
    private GestureDetector detector;

    private int currentZoomLevel = Constant.NORMAL;

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
        String font = Constant.DEFAULT_FONT;
        double fontSize = (double) currentZoomLevel / (double) Constant.NORMAL;
        double paddingSize = sqrt(sqrt(fontSize));
        String fontColor = Constant.DEFAULT_FONT_COLOR;
        String backgroundColor = Constant.DEFAULT_BACKGROUND_COLOR;

        String bookmarkImageFileName = "bookmark";

//        let nightModeEnabled = UserDefaults.standard.bool(forKey: "nightModeEnabled")
//        if nightModeEnabled {
//            fontColor = "rgb(186,186,186)"
//            backgroundColor = "rgb(33,34,37)"
//        }

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
                "body,tr {" +
                "margin: 0;" +
                "padding: " + paddingSize / 2 + "em " + paddingSize + "em;" +
                "font-family: '" + font + "';" +
                "line-height: 1.4;" +
                "font-size: " + fontSize + "em;" +
                "color: " + fontColor + ";" +
                "background-color: " + backgroundColor + ";" +
                "-webkit-text-size-adjust: none;" +
                "}";

        String verse =
                ".verse {" +
                "padding: " + paddingSize / 2 + "em 0;" +
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
                "padding: " + paddingSize / 2 + "em 0;" +
                "}";

        String hymnVerse =
                ".hymn-verse {" +
                "padding: " + paddingSize / 2 + "em 0;" +
                "}" +
                ".hymn-verse ol {" +
                "margin: 0 auto;" +
                "width: 80%;" +
                "}";

        String large = ".large {" +
                "font-size: 160%;" +
                "}";

        return "<head>" +
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

            if (currentZoomLevel == Constant.NORMAL)
                getSettings().setTextZoom(Constant.LARGE);
            else if (currentZoomLevel == Constant.LARGE)
                getSettings().setTextZoom(Constant.XLARGE);
            else
                getSettings().setTextZoom(Constant.NORMAL);

            scrollAutomaticallyAfterTextZoomChange(relativeVerticalScrollValue);

            return false;
        }
    }

    private class ContentWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            url = url.replace(Constant.BASE_ASSET_URL, "");
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

            RealmResults<Scripture> scripturesFound = realm.where(Scripture.class).equalTo("id", verseId).findAll().sort("id");
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
                        newBookmark.setName_primary(scripture.getParent_book().getName_primary() + " " + scripture.getChapter() + " : " + scripture.getVerse());
                        newBookmark.setName_secondary(scripture.getParent_book().getName_secondary() + " " + scripture.getChapter() + " : " + scripture.getVerse());
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

                RealmResults<Book> booksFound = realm.where(Book.class).equalTo("link", linkName).findAll().sort("id");

                if (booksFound.size() > 0) {
                    Book nextBook = booksFound.last();
                    Intent intent = new Intent(fragmentActivity, ContentActivity.class);
                    intent.putExtra("id", nextBook.getId());
                    intent.putExtra("name", nextBook.getName_primary());
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
                Book nextBook = realm.where(Book.class).equalTo("link", linkName).findAll().sort("id").last();
                int chaptersCount = nextBook.getChild_scriptures().where().equalTo("verse", "counter").findAll().sort("id").last().getChapter();

                Intent intent = new Intent(fragmentActivity, ContentActivity.class);
                intent.putExtra("id", nextBook.getId());
                intent.putExtra("name", nextBook.getName_primary());
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
