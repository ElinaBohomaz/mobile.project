package com.example.simplebrowser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.simplebrowser.databinding.ActivityHistoryBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding b;
    private final ExecutorService dbExec = Executors.newSingleThreadExecutor();
    private AppDb db;
    private RowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        db = AppDb.get(this);

        b.btnBack.setOnClickListener(v -> {
            pulse(v);
            finish();
        });

        b.btnClear.setOnClickListener(v -> {
            pulse(v);
            confirmClear();
        });

        b.list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RowAdapter(item -> {
            Intent it = new Intent(this, BrowserActivity.class);
            it.putExtra("openUrl", item.url);
            startActivity(it);
        }, item -> {
            dbExec.execute(() -> {
                db.dao().deleteHistory(item.url);
                runOnUiThread(this::load);
            });
        });

        b.list.setAdapter(adapter);

        load();

        b.root.setAlpha(0f);
        b.root.animate().alpha(1f).setDuration(220).start();
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Очистити історію?")
                .setMessage("Усі записи буде видалено без можливості відновлення.")
                .setNegativeButton("Скасувати", null)
                .setPositiveButton("Очистити", (d, w) -> clearHistory())
                .show();
    }

    private void clearHistory() {
        dbExec.execute(() -> {
            db.dao().clearHistory();
            runOnUiThread(this::load);
        });
    }

    private void load() {
        dbExec.execute(() -> {
            int count = db.dao().historyCount();
            List<AppDb.Row> rows = new ArrayList<>();
            for (AppDb.History h : db.dao().getHistory(200)) {
                rows.add(new AppDb.Row(h.url, h.title, h.time, false));
            }
            runOnUiThread(() -> {
                adapter.set(rows);
                b.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                b.historyCount.setText(getString(R.string.history_count_fmt, count));
            });
        });
    }

    private void pulse(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
        ).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExec.shutdown();
    }
}
