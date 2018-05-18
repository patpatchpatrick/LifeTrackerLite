package com.example.android.lifetrackerlite.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.lifetrackerlite.data.LTContract.GoalsHabitsEntry;

public class LTProvider extends ContentProvider {

    private static final int GOALSHABITS = 100;
    private static final int GOALSHABITS_ID = 101;


    //URI matcher to handle different URIs input into provider
    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(LTContract.CONTENT_AUTHORITY, LTContract.PATH_GOALSHABITS, GOALSHABITS);
        sUriMatcher.addURI(LTContract.CONTENT_AUTHORITY, LTContract.PATH_GOALSHABITS + "/#", GOALSHABITS_ID);
    }

    //Tag for log messages
    public static final String LOG_TAG = LTProvider.class.getSimpleName();
    private LTDbHelper mDbHelper;

    @Override
    public boolean onCreate() {

        //Create new instance of LTDbHelper to access database.
        mDbHelper = new LTDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        //Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        //Cursor to hold results of query
        Cursor cursor;

        //Match the URI
        int match = sUriMatcher.match(uri);
        switch (match) {
            case GOALSHABITS:
                //Query the table directly with the given inputs
                cursor = database.query(GoalsHabitsEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case GOALSHABITS_ID:
                //Query the table for a specific goal ID
                selection = GoalsHabitsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(GoalsHabitsEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        final int match = sUriMatcher.match(uri);

        switch (match) {
            case GOALSHABITS:
                return GoalsHabitsEntry.CONTENT_LIST_TYPE;
            case GOALSHABITS_ID:
                return GoalsHabitsEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GOALSHABITS:
                return insertGoal(uri, contentValues);
            default:
                //Query is not supported for a specific GOAL ID, will hit default exception
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }

    }

    private Uri insertGoal(Uri uri, ContentValues values) {

        // Checks to determine values are ok before inserting into database
        // Check to ensure name is not null
        String goalName = values.getAsString(GoalsHabitsEntry.COLUMN_GOAL_NAME);
        if (goalName == null) {
            throw new IllegalArgumentException("Goal requires a name");
        }

        // Check that the goal is valid
        Integer goal = values.getAsInteger(GoalsHabitsEntry.COLUMN_GOAL_OR_HABIT);
        if (goal == null || !GoalsHabitsEntry.isValidGoal(goal)) {
            throw new IllegalArgumentException("Goal requires valid goal or habit int");
        }

        // Check that the goal type is valid
        Integer goalType = values.getAsInteger(GoalsHabitsEntry.COLUMN_GOAL_TYPE);
        if (goalType == null || !GoalsHabitsEntry.isValidGoalType(goalType)) {
            throw new IllegalArgumentException("Goal requires valid goal type");
        }

        // Check that the goal completed int is valid
        Integer goalCompleted = values.getAsInteger(GoalsHabitsEntry.COLUMN_GOAL_COMPLETED);
        if (goalCompleted == null || !GoalsHabitsEntry.isValidGoalCompleted(goalCompleted)) {
            throw new IllegalArgumentException("Goal requires valid goal completed int");
        }

        //If data is valid, insert data into SQL database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(GoalsHabitsEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        //Notify any listeners that the data has changed for the URI
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);

        int rowsDeleted;

        switch (match) {
            case GOALSHABITS:
                rowsDeleted = database.delete(GoalsHabitsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case GOALSHABITS_ID:
                selection = GoalsHabitsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                rowsDeleted = database.delete(GoalsHabitsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        //If 1 or more rows were deleted, notify all listeners that data at the given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
