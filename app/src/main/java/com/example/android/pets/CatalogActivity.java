/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.pets.data.PetContract;
import com.example.android.pets.data.PetDbHelper;

import java.net.URI;
import java.util.Arrays;

/**
 * Displays list of pets that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = CatalogActivity.class.getSimpleName();
    private static final int LOADER_ID = 0;
    PetDbHelper mDbHelper;
    SQLiteDatabase db;

    PetCursorAdapter mPetCursorAdapter;
    ListView mPetListView;

    private String[] projection =  {
        PetContract.PetEntry._ID,
        PetContract.PetEntry.COLUMN_NAME,
        PetContract.PetEntry.COLUMN_BREED,
        PetContract.PetEntry.COLUMN_GENDER,
        PetContract.PetEntry.COLUMN_WEIGHT
    };

    @Override
    protected void onStart() {
        super.onStart();
        //displayDatabaseInfo();
        //getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);
        Log.i(LOG_TAG, "önCreate()");

        mPetListView = (ListView) findViewById(R.id.listview);
        View emptyView = findViewById(R.id.empty_view);
        mPetListView.setEmptyView(emptyView);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        //displayDatabaseInfo();
        mPetCursorAdapter = new PetCursorAdapter(this, null);
        mPetListView.setAdapter(mPetCursorAdapter);

        mDbHelper = new PetDbHelper(getApplicationContext());
        db = mDbHelper.getReadableDatabase();
        mPetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.i(LOG_TAG, String.valueOf(id));
                Intent intent = new Intent(getApplication(), EditorActivity.class);
                intent.setData(ContentUris.withAppendedId(PetContract.PetEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    private void insertPet() {
        ContentValues values = new ContentValues();

        values.put(PetContract.PetEntry.COLUMN_NAME, "Toto");
        values.put(PetContract.PetEntry.COLUMN_BREED, "Terrier");
        values.put(PetContract.PetEntry.COLUMN_GENDER, PetContract.PetEntry.GENDER_MALE);
        values.put(PetContract.PetEntry.COLUMN_WEIGHT, 7);

        Uri uriReturned = getContentResolver().insert(PetContract.PetEntry.CONTENT_URI, values);
        if (uriReturned == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_pet_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_pet_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertPet();
                displayDatabaseInfo();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                getContentResolver().delete(PetContract.PetEntry.CONTENT_URI, null, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayDatabaseInfo() {
        Cursor cursor = getContentResolver().query(
                PetContract.PetEntry.CONTENT_URI,
                projection, null, null, null);
        mPetCursorAdapter = new PetCursorAdapter(this, cursor);
        mPetListView.setAdapter(mPetCursorAdapter);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getApplicationContext(),
                PetContract.PetEntry.CONTENT_URI,
                new String[]{
                        PetContract.PetEntry._ID,
                        PetContract.PetEntry.COLUMN_BREED,
                        PetContract.PetEntry.COLUMN_NAME},
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mPetCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mPetCursorAdapter.swapCursor(null);
    }
}
