package com.lib.bridge.qrcode;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;

import io.reactivex.annotations.NonNull;

@Database(entities = {Scan.class},exportSchema = false,version = 1)
public abstract class ScanRoomDatabase extends RoomDatabase {

    private static ScanRoomDatabase instance;

    public abstract ScanDao scanDao();

    public static synchronized ScanRoomDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    ScanRoomDatabase.class, "scan_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build();
        }
        return instance;
    }

    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            new PopulateDbAsyncTask(instance).execute();
        }
    };

    private static class PopulateDbAsyncTask extends AsyncTask<Void, Void, Void> {
        private ScanDao scanDao;

        private PopulateDbAsyncTask(ScanRoomDatabase db) {
            scanDao = db.scanDao();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }
}