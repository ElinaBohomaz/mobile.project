package com.example.simplebrowser;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;

@Database(entities = {AppDb.History.class, AppDb.Bookmark.class}, version = 1, exportSchema = false)
public abstract class AppDb extends RoomDatabase {

    private static volatile AppDb INSTANCE;

    public static AppDb get(Context c) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(c.getApplicationContext(), AppDb.class, "simple_browser.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract Dao dao();

    @Entity(tableName = "history")
    public static class History {
        @PrimaryKey
        @NonNull
        public String url;

        @NonNull
        public String title;

        public long time;

        public History(@NonNull String url, @NonNull String title, long time) {
            this.url = url;
            this.title = title;
            this.time = time;
        }
    }

    @Entity(tableName = "bookmarks")
    public static class Bookmark {
        @PrimaryKey
        @NonNull
        public String url;

        @NonNull
        public String title;

        public long time;

        public Bookmark(@NonNull String url, @NonNull String title, long time) {
            this.url = url;
            this.title = title;
            this.time = time;
        }
    }

    public static class Row {
        public final String url;
        public final String title;
        public final long time;
        public final boolean isBookmark;

        public Row(String url, String title, long time, boolean isBookmark) {
            this.url = url;
            this.title = title;
            this.time = time;
            this.isBookmark = isBookmark;
        }
    }

    @androidx.room.Dao
    public interface Dao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertHistory(History item);

        @Query("SELECT * FROM history ORDER BY time DESC LIMIT :limit")
        List<History> getHistory(int limit);

        @Query("SELECT COUNT(*) FROM history")
        int historyCount();

        @Query("DELETE FROM history")
        void clearHistory();

        @Query("DELETE FROM history WHERE url = :url")
        void deleteHistory(String url);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsertBookmark(Bookmark item);

        @Query("SELECT * FROM bookmarks ORDER BY time DESC")
        List<Bookmark> getBookmarks();

        @Query("SELECT COUNT(*) FROM bookmarks WHERE url = :url")
        int bookmarkExists(String url);

        @Query("DELETE FROM bookmarks WHERE url = :url")
        void deleteBookmark(String url);
    }
}
