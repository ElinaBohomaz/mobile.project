package com.example.simplebrowser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simplebrowser.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.cardBrowser.setOnClickListener(v -> {
            pulse(v);
            startActivity(new Intent(this, BrowserActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        b.cardHistory.setOnClickListener(v -> {
            pulse(v);
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        b.cardExit.setOnClickListener(v -> {
            pulse(v);
            finish();
        });

        b.title.animate().alpha(1f).translationY(0f).setDuration(450).start();
        b.subtitle.animate().alpha(1f).translationY(0f).setDuration(520).start();
    }

    private void pulse(View v) {
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        ).start();
    }
}
