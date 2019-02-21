package com.nozokada.japaneseldsquad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;

import io.realm.Realm;
import io.realm.RealmResults;

public class ContentActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "JLQPrefsFile";

    private Realm realm;

    private String bookId;
    private String targetBookName;
    private Book targetBook;
    private int targetChapter;
    private String targetVerse;
    private String targetScriptureId;
    private int chaptersCount;
    private boolean slided;

    private int currentIndex;
    private RealmResults<Scripture> scripturesList;

    ViewPager viewPager;
    private ContentFragmentStatePagerAdapter fragmentStatePagerAdapter;

    private SharedPreferences settings;
    private SwitchCompat dualSwitch;
    private boolean dualEnabled;
    private boolean secondaryExists = true;

    private boolean gsViewed = false;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        settings = getSharedPreferences(PREFS_NAME, 0);
        dualEnabled = settings.getBoolean("dualEnabled", false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        realm = Realm.getDefaultInstance();

        if (savedInstanceState == null) {
            bookId = getIntent().getExtras().getString("id");
            targetBookName = getIntent().getExtras().getString("name");
            targetChapter = getIntent().getExtras().getInt("chapter");
            targetVerse = getIntent().getExtras().getString("verse");
            targetScriptureId = getIntent().getExtras().getString("scriptureId", null);
            chaptersCount = getIntent().getExtras().getInt("count");
            slided = getIntent().getExtras().getBoolean("slided", false);

            if (slided) overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            else overridePendingTransition(0, 0);
        }
        else {
            bookId = savedInstanceState.getString("id");
            targetBookName = savedInstanceState.getString("name");
            targetChapter = savedInstanceState.getInt("chapter");
            targetVerse = savedInstanceState.getString("verse");
            targetScriptureId = savedInstanceState.getString("scriptureId");
            chaptersCount = savedInstanceState.getInt("count");
        }

        currentIndex = targetChapter - 1;

        targetBook = realm.where(Book.class).equalTo("id", bookId).findFirst();

        scripturesList = targetBook.getChild_scriptures().sort("id");

        if (scripturesList != null) {
            if (scripturesList.first().getScripture_eng().equals(""))
                secondaryExists = false;
        }

        if (targetBook.getLink().startsWith("gs"))
            gsViewed = true;

        String counter = scripturesList.where().equalTo("verse", "counter").equalTo("chapter", targetChapter).findFirst().getScripture_jpn();
        if (counter.equals("")) {
            if (gsViewed) {
                getSupportActionBar().setTitle(scripturesList.where().equalTo("verse", "title")
                        .equalTo("chapter", targetChapter).findFirst().getScripture_jpn().replaceAll("<[^>]*>", ""));
            }
            else {
                getSupportActionBar().setTitle(scripturesList.first().getParent_book().getName_jpn());
            }
        }
        else {
            getSupportActionBar().setTitle(targetBookName + " " + counter);
        }

        viewPager = findViewById(R.id.pager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                String counter = scripturesList.where().equalTo("verse", "counter").equalTo("chapter", position + 1).findFirst().getScripture_jpn();
                if (counter.equals("")) {
                    if (gsViewed) {
                        getSupportActionBar().setTitle(scripturesList.where().equalTo("verse", "title")
                                .equalTo("chapter", position + 1).findFirst().getScripture_jpn().replaceAll("<[^>]*>", ""));
                    }
                    else {
                        getSupportActionBar().setTitle(scripturesList.first().getParent_book().getName_jpn());
                    }
                }
                else {
                    getSupportActionBar().setTitle(targetBookName + " " + counter);
                }

                currentIndex = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        fragmentStatePagerAdapter = new ContentFragmentStatePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentStatePagerAdapter);
        viewPager.setCurrentItem(currentIndex);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (dualEnabled != settings.getBoolean("dualEnabled", false)) {
            viewPager.setAdapter(fragmentStatePagerAdapter);
            viewPager.setCurrentItem(currentIndex);
        }

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.switchEng);
        item.setActionView(R.layout.switch_eng);

        dualEnabled = settings.getBoolean("dualEnabled", false);

        dualSwitch = item.getActionView().findViewById(R.id.switchForActionBar);
        dualSwitch.setChecked(dualEnabled);

        dualSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("dualEnabled", isChecked);
                editor.apply();

//                fragmentStatePagerAdapter.notifyDataSetChanged();
//                viewPager.setAdapter(fragmentStatePagerAdapter);
//                viewPager.setCurrentItem(currentIndex);

            }
        });

        if (secondaryExists) dualSwitch.setEnabled(true);
        else dualSwitch.setEnabled(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bookmarks) {
            Intent intent = new Intent(getApplicationContext(), BookmarksActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.search) {
            Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.home) {
            Intent intent = new Intent(getApplicationContext(), BooksActivity.class);
            intent.putExtra("id", "0");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("id", bookId);
        outState.putString("name", targetBookName);
        outState.putInt("chapter", targetChapter);
        outState.putString("verse", targetVerse);
        outState.putString("scriptureId", targetScriptureId);
        outState.putInt("count", chaptersCount);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private class ContentFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        ContentFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (targetChapter != position + 1) targetVerse = "";
            return ContentFragment.newInstance(bookId, position + 1, targetVerse, targetScriptureId);
        }

        @Override
        public int getCount() {
            return chaptersCount;
        }
    }
}
