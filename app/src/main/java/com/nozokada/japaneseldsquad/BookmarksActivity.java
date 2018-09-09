package com.nozokada.japaneseldsquad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class BookmarksActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "JLQPrefsFile";

    private Realm realm;

    private RealmResults<Bookmark> bookmarksList;

    private ArrayAdapter<Bookmark> arrayAdapter;

    private SharedPreferences settings;
    private SwitchCompat englishSwitch;
    private boolean englishEnabled;

    private Toolbar toolbar;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);

        settings = getSharedPreferences(PREFS_NAME, 0);
        englishEnabled = settings.getBoolean("englishEnabled", false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.list);

        realm = Realm.getDefaultInstance();

        bookmarksList = realm.where(Bookmark.class).findAll().sort("date");

        getSupportActionBar().setTitle(getString(R.string.bookmarks));

        if (bookmarksList.size() > 0) {
            arrayAdapter = new ArrayAdapter<Bookmark>(this, R.layout.list_item, android.R.id.text1, bookmarksList) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    englishEnabled = settings.getBoolean("englishEnabled", false);

                    String jpText = bookmarksList.get(position).getName_jpn();

                    if (englishEnabled) {
                        String enText = bookmarksList.get(position).getName_eng();

                        TextView text1 = view.findViewById(android.R.id.text1);
                        text1.setText(jpText);

                        TextView text2 = view.findViewById(android.R.id.text2);
                        text2.setVisibility(View.VISIBLE);
                        text2.setText(enText);
                    }
                    else {
                        TextView text1 = view.findViewById(android.R.id.text1);
                        text1.setText(jpText);
                        TextView text2 = view.findViewById(android.R.id.text2);
                        text2.setVisibility(View.GONE);
                    }

                    return view;
                }
            };

            listView.setAdapter(arrayAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Scripture scripture = bookmarksList.get(position).getScripture();
                    Book parentBook = scripture.getParent_book();
                    Book grandParentBook = parentBook.getParent_book();
                    List<Intent> intents = new ArrayList<Intent>();

                    Intent rootIntent = new Intent(getApplicationContext(), BooksActivity.class);
                    rootIntent.putExtra("id", "0");
                    rootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intents.add(rootIntent);

                    Intent grandParentIntent = new Intent(getApplicationContext(), BooksActivity.class);
                    grandParentIntent.putExtra("id", grandParentBook.getId());
                    intents.add(grandParentIntent);

                    int chaptersCount = parentBook.getChild_scriptures().sort("id").last().getChapter();
                    if (chaptersCount > 1) {
                        Intent parentIntent = new Intent(getApplicationContext(), ChaptersActivity.class);
                        parentIntent.putExtra("id", parentBook.getId());
                        intents.add(parentIntent);
                    }

                    Intent intent = new Intent(getApplicationContext(), ContentActivity.class);
                    intent.putExtra("id", parentBook.getId());
                    intent.putExtra("name", parentBook.getName_jpn());
                    intent.putExtra("chapter", bookmarksList.get(position).getScripture().getChapter());
                    intent.putExtra("verse", bookmarksList.get(position).getScripture().getVerse());
                    intent.putExtra("scriptureId", bookmarksList.get(position).getScripture().getId());
                    intent.putExtra("count", chaptersCount);
                    intents.add(intent);

                    startActivities(intents.toArray(new Intent[intents.size()]));
                    finish();
                }
            });
        }
        else {
            // No bookmarks
            TextView textView = findViewById(R.id.text);
            textView.setText(getText(R.string.no_bookmark_msg));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bookmarks, menu);
        MenuItem item = menu.findItem(R.id.switchEng);
        item.setActionView(R.layout.switch_eng);

        englishEnabled = settings.getBoolean("englishEnabled", false);

        englishSwitch = item.getActionView().findViewById(R.id.switchForActionBar);
        englishSwitch.setChecked(englishEnabled);

        englishSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("englishEnabled", isChecked);
                editor.apply();

                if (arrayAdapter != null)
                    arrayAdapter.notifyDataSetChanged();
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
