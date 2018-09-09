package com.nozokada.japaneseldsquad;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;

public class SearchActivity extends AppCompatActivity implements OnQueryTextListener {
    private static final String PREFS_NAME = "JLQPrefsFile";

    private Realm realm;

    private RealmResults<Scripture> searchResultsList;

    private ArrayAdapter<Scripture> arrayAdapter;

    private SharedPreferences settings;
    private SwitchCompat englishSwitch;
    private boolean englishEnabled;

    private Toolbar toolbar;
    private ListView listView;
    private TextView textView;

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

        getSupportActionBar().setTitle(getString(R.string.search));

        textView = findViewById(R.id.text);
        textView.setText(getText(R.string.no_results_msg));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(false);
//        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(this);

//        MenuItem item = menu.findItem(R.id.switchEng);
//        item.setActionView(R.layout.switch_eng);
//
//        englishEnabled = settings.getBoolean("englishEnabled", false);
//
//        englishSwitch = (SwitchCompat) item.getActionView().findViewById(R.id.switchForActionBar);
//        englishSwitch.setChecked(englishEnabled);
//
//        englishSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                SharedPreferences.Editor editor = settings.edit();
//                editor.putBoolean("englishEnabled", isChecked);
//                editor.apply();

//                if (arrayAdapter != null)
//                    arrayAdapter.notifyDataSetChanged();
//            }
//        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (newText.equals(""))
            searchResultsList = realm.where(Scripture.class).equalTo("id", "-1").findAll();
        else
            searchResultsList = realm.where(Scripture.class).contains("scripture_eng_search", newText, Case.INSENSITIVE)
                    .or().contains("scripture_jpn_search", newText).findAll().sort("id");

        if (searchResultsList.size() > 0)
            textView.setVisibility(View.GONE);
        else
            textView.setVisibility(View.VISIBLE);

        arrayAdapter = new ArrayAdapter<Scripture>(this, R.layout.list_item, android.R.id.text1, searchResultsList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                Scripture scripture = searchResultsList.get(position);

                boolean gsCell = false;
                boolean hymnsCell = false;
                boolean contCell = false;

                if (scripture.getParent_book().getLink().startsWith("gs")) gsCell = true;
                else if (scripture.getParent_book().getLink().startsWith("hymns")) hymnsCell = true;
                else if (scripture.getParent_book().getLink().endsWith("_cont")) contCell = true;

                StringBuilder jpText = new StringBuilder();
                RealmResults<Scripture> hymnFound;
                RealmResults<Scripture> gsFound;

                if (hymnsCell) {
                    hymnFound = scripture.getParent_book().getChild_scriptures().where().equalTo("chapter", scripture.getChapter()).findAll();
                    String title = hymnFound.where().equalTo("verse", "title").findFirst().getScripture_jpn().replaceAll("<[^>]*>", "");
                    String counter = hymnFound.where().equalTo("verse", "counter").findFirst().getScripture_jpn();
                    jpText.append("賛美歌 ").append(counter).append(" ").append(title).append(" ").append(scripture.getVerse()).append("番");
                }
                else if (gsCell) {
                    gsFound = scripture.getParent_book().getChild_scriptures().where().equalTo("chapter", scripture.getChapter()).findAll();
                    String title = gsFound.where().equalTo("verse", "title").findFirst().getScripture_jpn().replaceAll("<[^>]*>", "");
                    jpText.append("聖句ガイド「").append(title).append("」").append(scripture.getVerse()).append("段落目");
                }
                else if (contCell) {
                    jpText.append(scripture.getParent_book().getParent_book().getName_jpn())
                            .append(" ").append(scripture.getParent_book().getName_jpn())
                            .append(" ").append(scripture.getVerse()).append("段落目");
                }
                else {
                    jpText.append(scripture.getParent_book().getName_jpn())
                            .append(" ").append(scripture.getChapter())
                            .append(" : ").append(scripture.getVerse());
                }

                if (settings.getBoolean("englishEnabled", false)) {
                    StringBuilder enText = new StringBuilder();

                    TextView text1 = view.findViewById(android.R.id.text1);
                    TextView text2 = view.findViewById(android.R.id.text2);
                    text2.setVisibility(View.VISIBLE);

                    if (hymnsCell) {
                        hymnFound = scripture.getParent_book().getChild_scriptures().where().equalTo("chapter", scripture.getChapter()).findAll();
                        String title = hymnFound.where().equalTo("verse", "title").findFirst().getScripture_eng().replaceAll("<[^>]*>", "");
                        String counter = hymnFound.where().equalTo("verse", "counter").findFirst().getScripture_eng();
                        enText.append("HYMN ").append(counter).append(" ").append(title).append(" Verse ").append(scripture.getVerse());
                    }
                    else if (gsCell) {
                        text2.setVisibility(View.GONE);
                    }
                    else if (contCell) {
                        enText.append(scripture.getParent_book().getParent_book().getName_eng())
                                .append(" ").append(scripture.getParent_book().getName_eng())
                                .append(" Paragraph ").append(scripture.getVerse());
                    }
                    else {
                        enText.append(scripture.getParent_book().getName_eng())
                                .append(" ").append(scripture.getChapter())
                                .append(" : ").append(scripture.getVerse());
                    }

                    text1.setText(jpText);
                    text2.setText(enText);
                }
                else {
                    TextView text1 = view.findViewById(android.R.id.text1);
                    TextView text2 = view.findViewById(android.R.id.text2);
                    text1.setText(jpText);
                    text2.setVisibility(View.GONE);
                }

                return view;
            }
        };

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Scripture scripture = searchResultsList.get(position);
                Book parentBook = scripture.getParent_book();

                int chaptersCount = parentBook.getChild_scriptures().sort("id").last().getChapter();

                Intent intent = new Intent(getApplicationContext(), ContentActivity.class);
                intent.putExtra("id", parentBook.getId());
                intent.putExtra("name", parentBook.getName_jpn());
                intent.putExtra("chapter", searchResultsList.get(position).getChapter());
                intent.putExtra("verse", searchResultsList.get(position).getVerse());
                intent.putExtra("scriptureId", searchResultsList.get(position).getId());
                intent.putExtra("count", chaptersCount);
                intent.putExtra("slided", true);
                startActivity(intent);
            }
        });

        return true;
    }
}
