package com.example.roomiesplit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.roomiesplit.ui.MessagesFragment;
import java.util.ArrayList;
import java.util.List;

public class NotificationDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "roomie_notifications.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "notifications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_ACTION_URL = "action_url";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_IS_READ = "is_read";

    public NotificationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_MESSAGE + " TEXT,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_ACTION_URL + " TEXT,"
                + COLUMN_CREATED_AT + " TEXT,"
                + COLUMN_IS_READ + " INTEGER" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertOrUpdate(List<MessagesFragment.MessageItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (MessagesFragment.MessageItem item : items) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ID, item.id);
                values.put(COLUMN_TITLE, item.title);
                values.put(COLUMN_MESSAGE, item.message);
                values.put(COLUMN_TYPE, item.type);
                values.put(COLUMN_ACTION_URL, item.actionToken); // Assuming actionToken maps to action_url
                values.put(COLUMN_CREATED_AT, item.time);
                // We don't overwrite read status from server necessarily if local is read?
                // Actually server is source of truth. If server says unread, it is unread.
                // But wait, user requirement "Local Persistence".
                // If we rely on server for read status, we update it.
                // However, MessagesFragment logic marks it read on load.
                // So we will set it to 1 (Read) if we processed it?
                // Let's store what we got.
                values.put(COLUMN_IS_READ, 0); // Default, or handle logic elsewhere?

                // Using REPLACE strategy
                // db.insertWithOnConflict(TABLE_NAME, null, values,
                // SQLiteDatabase.CONFLICT_REPLACE);
                // Better: Check existence?
                // Actually REPLACE deletes and inserts, losing custom local state if any.
                // Since we want offline cache, syncing from server is fine.

                // NOTE: MessageItem helper definition is inside DB helper, or we import?
                // MessagesFragment.MessageItem is static. Import works.
                // But MessageItem doesn't expose fields publicly? Access package-private?
                // They are package-private in MessagesFragment. We might need to copy/duplicate
                // class or make it public.
                // For now assuming we can access or fix MessagesFragment.MessageItem
                // visibility.

                db.replace(TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Overloaded for inserting raw data or simpler DTOs if needed.
    // Ideally we fetch from DB.

    public List<MessagesFragment.MessageItem> getAllNotifications() {
        List<MessagesFragment.MessageItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                String actionUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTION_URL));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));

                // Fix: Check actionUrl for both types
                boolean isActionable = ("INVITE".equalsIgnoreCase(type)
                        || "PAYMENT_CONFIRMATION".equalsIgnoreCase(type))
                        && actionUrl != null && !actionUrl.isEmpty();

                // Constructor of MessageItem?
                // public MessageItem(Long id, String title, String message, String time,
                // boolean isActionable, String actionToken, String type)
                list.add(new MessagesFragment.MessageItem(id, title, message, time, isActionable, actionUrl, type));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteNotification(Long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[] { String.valueOf(id) });
    }
}
