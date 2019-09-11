package com.nozokada.japaneseldsquad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import io.realm.Realm;
import io.realm.RealmResults;

public class BooksActivity extends AppCompatActivity {
    private Realm realm;

    private String bookId;
    private boolean slided;
    private String targetBookName;
    private Book targetBook;
    private RealmResults<Book> booksList;

    private ArrayAdapter<Book> arrayAdapter;

    private SharedPreferences settings;
    private SwitchCompat dualSwitch;
    private boolean dualEnabled;

    private Toolbar toolbar;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        settings = getSharedPreferences(Constant.PREFS_NAME, 0);
        dualEnabled = settings.getBoolean(Constant.DUAL, false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.list);

        realm = Realm.getDefaultInstance();

        if (savedInstanceState == null) {
            bookId = getIntent().getExtras().getString("id");
            slided = getIntent().getExtras().getBoolean("slided", false);
            targetBook = realm.where(Book.class).equalTo("id", bookId).findFirst();

            if (slided) overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        else {
            bookId = savedInstanceState.getString("id");
            targetBook = realm.where(Book.class).equalTo("id", bookId).findFirst();
        }

        if (bookId.equals("0")) targetBookName = getString(R.string.top_name);
        else targetBookName = targetBook.getName_primary();

        booksList = targetBook.getChild_books().sort("id");

        getSupportActionBar().setTitle(targetBookName);

        arrayAdapter = new ArrayAdapter<Book>(this, R.layout.list_item, android.R.id.text1, booksList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                dualEnabled = settings.getBoolean("dualEnabled", false);

                if (dualEnabled) {
                    TextView text1 = view.findViewById(android.R.id.text1);
                    text1.setText(booksList.get(position).getName_primary());
                    TextView text2 = view.findViewById(android.R.id.text2);
                    text2.setVisibility(View.VISIBLE);
                    text2.setText(booksList.get(position).getName_secondary());
                }
                else {
                    TextView text1 = view.findViewById(android.R.id.text1);
                    text1.setText(booksList.get(position).getName_primary());
                    TextView text2 = view.findViewById(android.R.id.text2);
                    text2.setVisibility(View.GONE);
                }

                return view;
            }
        };

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Book nextBook = arrayAdapter.getItem(position);
                Intent intent;

                assert nextBook != null;
                if (nextBook.getChild_books().size() > 0) {
                    intent = new Intent(getApplicationContext(), BooksActivity.class);
                }
                else if (nextBook.getChild_scriptures().sort("id").last().getChapter() == 1) {
                    intent = new Intent(getApplicationContext(), ContentActivity.class);
                    intent.putExtra("name", nextBook.getName_primary());
                    intent.putExtra("chapter", 1);
                    intent.putExtra("verse", "");
                    intent.putExtra("count", 1);
                }
                else {
                    intent = new Intent(getApplicationContext(), ChaptersActivity.class);
                }
                intent.putExtra("id", nextBook.getId());
                intent.putExtra("slided", true);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        invalidateOptionsMenu();
        arrayAdapter.notifyDataSetChanged();
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

                arrayAdapter.notifyDataSetChanged();
            }
        });

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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!bookId.equals("0"))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
