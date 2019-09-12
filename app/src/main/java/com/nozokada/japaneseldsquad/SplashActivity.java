package com.nozokada.japaneseldsquad;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getApplicationContext(), BooksActivity.class);
        intent.putExtra("id", "0");
        startActivity(intent);

        finish();
    }
}
