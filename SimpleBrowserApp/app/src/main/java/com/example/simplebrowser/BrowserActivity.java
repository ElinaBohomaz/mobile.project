package com.example.simplebrowser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.simplebrowser.databinding.ActivityBrowserBinding;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowserActivity extends AppCompatActivity {

    private ActivityBrowserBinding b;

    private final ExecutorService dbExec = Executors.newSingleThreadExecutor();
    private AppDb db;

    private final TabState[] tabs = new TabState[3];
    private int currentTab = 0;

    private RowAdapter bookmarksAdapter;
    private RowAdapter historyMiniAdapter;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityBrowserBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        db = AppDb.get(this);

        setupTabs();
        setupTopBar();
        setupLists();
        setupWebViews();
        setupBackDispatcher();

        switchTab(0, false);
        loadBookmarks();
        loadMiniHistory();

        String openUrl = getIntent().getStringExtra("openUrl");
        if (!TextUtils.isEmpty(openUrl)) {
            WebView w = tabs[currentTab].web;
            if (w != null) w.loadUrl(openUrl);
        }

        b.getRoot().setAlpha(0f);
        b.getRoot().animate().alpha(1f).setDuration(220).start();
    }

    private void setupTabs() {
        b.tabs.addTab(b.tabs.newTab().setText(getString(R.string.tab_browser)));
        b.tabs.addTab(b.tabs.newTab().setText(getString(R.string.tab_bookmarks)));
        b.tabs.addTab(b.tabs.newTab().setText(getString(R.string.tab_quick_history)));

        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { switchTab(tab.getPosition(), true); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupTopBar() {
        b.btnBack.setOnClickListener(v -> {
            pulse(v);
            WebView w = tabs[currentTab].web;
            if (w != null && w.canGoBack()) w.goBack();
        });

        b.btnForward.setOnClickListener(v -> {
            pulse(v);
            WebView w = tabs[currentTab].web;
            if (w != null && w.canGoForward()) w.goForward();
        });

        b.btnRefresh.setOnClickListener(v -> {
            pulse(v);
            WebView w = tabs[currentTab].web;
            if (w != null) w.reload();
        });

        b.btnBookmark.setOnClickListener(v -> {
            pulse(v);
            WebView w = tabs[currentTab].web;
            if (w == null) return;

            String url = w.getUrl();
            String title = w.getTitle();
            if (TextUtils.isEmpty(url)) return;

            String t = TextUtils.isEmpty(title) ? url : title;

            dbExec.execute(() -> {
                int exists = db.dao().bookmarkExists(url);
                if (exists > 0) {
                    runOnUiThread(() -> toast("Вже додано в закладки"));
                    return;
                }
                db.dao().upsertBookmark(new AppDb.Bookmark(url, t, System.currentTimeMillis()));
                runOnUiThread(() -> {
                    toast("Додано в закладки");
                    updateBookmarkIndicator(url);
                    loadBookmarks();
                });
            });
        });

        b.btnOpenHistory.setOnClickListener(v -> {
            pulse(v);
            startActivity(new android.content.Intent(this, HistoryActivity.class));
        });

        b.inputUrl.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                goFromInput();
                return true;
            }
            return false;
        });

        b.inputUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                b.btnGo.setEnabled(s != null && s.length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.btnGo.setOnClickListener(v -> {
            pulse(v);
            goFromInput();
        });
    }

    private void setupLists() {
        b.listBookmarks.setLayoutManager(new LinearLayoutManager(this));
        b.listHistoryMini.setLayoutManager(new LinearLayoutManager(this));

        bookmarksAdapter = new RowAdapter(
                item -> {
                    TabLayout.Tab t0 = b.tabs.getTabAt(0);
                    if (t0 != null) b.tabs.selectTab(t0);
                    switchTab(0, true);

                    WebView w = tabs[currentTab].web;
                    if (w != null) w.loadUrl(item.url);
                },
                item -> dbExec.execute(() -> {
                    db.dao().deleteBookmark(item.url);
                    runOnUiThread(() -> {
                        WebView w = tabs[currentTab].web;
                        if (w != null) updateBookmarkIndicator(w.getUrl());
                        loadBookmarks();
                    });
                })
        );

        historyMiniAdapter = new RowAdapter(
                item -> {
                    TabLayout.Tab t0 = b.tabs.getTabAt(0);
                    if (t0 != null) b.tabs.selectTab(t0);
                    switchTab(0, true);

                    WebView w = tabs[currentTab].web;
                    if (w != null) w.loadUrl(item.url);
                },
                item -> dbExec.execute(() -> {
                    db.dao().deleteHistory(item.url);
                    runOnUiThread(this::loadMiniHistory);
                })
        );

        b.listBookmarks.setAdapter(bookmarksAdapter);
        b.listHistoryMini.setAdapter(historyMiniAdapter);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViews() {
        tabs[0] = new TabState(b.web1);
        tabs[1] = new TabState(b.web2);
        tabs[2] = new TabState(b.web3);

        for (int i = 0; i < tabs.length; i++) {
            WebView w = tabs[i].web;
            if (w == null) continue;

            WebSettings s = w.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            s.setSupportZoom(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);

            CookieManager.getInstance().setAcceptCookie(true);

            w.setWebChromeClient(new WebChromeClient() {
                @Override public void onProgressChanged(WebView view, int newProgress) {
                    b.progress.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                    b.progress.setProgress(newProgress);
                }

                @Override public void onReceivedTitle(WebView view, String title) {
                    if (view == tabs[currentTab].web) {
                        b.pageTitle.setText(TextUtils.isEmpty(title) ? getString(R.string.app_name) : title);
                    }
                }
            });

            w.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return false;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (view == tabs[currentTab].web) {
                        b.inputUrl.setText(url);
                        b.pageTitle.setText(getString(R.string.loading));
                        animateContent();
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    String title = view.getTitle();
                    String t = TextUtils.isEmpty(title) ? url : title;

                    if (view == tabs[currentTab].web) {
                        b.inputUrl.setText(url);
                        b.pageTitle.setText(t);
                        b.btnBack.setEnabled(view.canGoBack());
                        b.btnForward.setEnabled(view.canGoForward());
                        updateBookmarkIndicator(url);
                    }

                    dbExec.execute(() ->
                            db.dao().insertHistory(new AppDb.History(url, t, System.currentTimeMillis()))
                    );
                }
            });

            w.setVisibility(View.GONE);
            w.loadUrl(defaultUrl(i));
        }
    }

    private void updateBookmarkIndicator(String url) {
        if (TextUtils.isEmpty(url)) {
            b.btnBookmark.setText("☆");
            return;
        }
        dbExec.execute(() -> {
            int exists = db.dao().bookmarkExists(url);
            runOnUiThread(() -> b.btnBookmark.setText(exists > 0 ? "★" : "☆"));
        });
    }

    private void setupBackDispatcher() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (b.tabs.getSelectedTabPosition() != 0) {
                    TabLayout.Tab t0 = b.tabs.getTabAt(0);
                    if (t0 != null) b.tabs.selectTab(t0);
                    switchTab(0, true);
                    return;
                }
                WebView w = tabs[currentTab].web;
                if (w != null && w.canGoBack()) {
                    w.goBack();
                    return;
                }
                finish();
            }
        });
    }

    private void goFromInput() {
        String raw = String.valueOf(b.inputUrl.getText()).trim();
        if (TextUtils.isEmpty(raw)) return;

        String url = raw;
        boolean hasScheme = raw.startsWith("http://") || raw.startsWith("https://");
        boolean looksLikeUrl = raw.contains(".") && !raw.contains(" ");
        if (!hasScheme && looksLikeUrl) url = "https://" + raw;
        if (!hasScheme && !looksLikeUrl) url = "https://www.google.com/search?q=" + android.net.Uri.encode(raw);

        WebView w = tabs[currentTab].web;
        if (w != null) w.loadUrl(url);
    }

    private void switchTab(int index, boolean animate) {
        currentTab = index;

        b.panelBrowser.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        b.panelBookmarks.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        b.panelHistoryMini.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        if (tabs[0].web != null) tabs[0].web.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (tabs[1].web != null) tabs[1].web.setVisibility(View.GONE);
        if (tabs[2].web != null) tabs[2].web.setVisibility(View.GONE);

        if (index == 1) loadBookmarks();
        if (index == 2) loadMiniHistory();

        if (animate) animatePanels();
    }

    private void loadBookmarks() {
        dbExec.execute(() -> {
            List<AppDb.Row> rows = new ArrayList<>();
            for (AppDb.Bookmark bm : db.dao().getBookmarks()) {
                rows.add(new AppDb.Row(bm.url, bm.title, bm.time, true));
            }
            runOnUiThread(() -> bookmarksAdapter.set(rows));
        });
    }

    private void loadMiniHistory() {
        dbExec.execute(() -> {
            List<AppDb.Row> rows = new ArrayList<>();
            for (AppDb.History h : db.dao().getHistory(12)) {
                rows.add(new AppDb.Row(h.url, h.title, h.time, false));
            }
            runOnUiThread(() -> historyMiniAdapter.set(rows));
        });
    }

    private void animatePanels() {
        View target = currentTab == 0 ? b.panelBrowser : (currentTab == 1 ? b.panelBookmarks : b.panelHistoryMini);
        target.setAlpha(0f);
        target.setTranslationY(14f);
        target.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    private void animateContent() {
        b.topBar.animate().alpha(0.92f).setDuration(80).withEndAction(() ->
                b.topBar.animate().alpha(1f).setDuration(140).start()
        ).start();
    }

    private void pulse(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
        ).start();
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private String defaultUrl(int i) {
        if (i == 0) return "https://www.google.com";
        if (i == 1) return "https://uk.wikipedia.org";
        return "https://developer.android.com";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExec.shutdown();
    }

    private static class TabState {
        final WebView web;
        TabState(WebView web) { this.web = web; }
    }
}
