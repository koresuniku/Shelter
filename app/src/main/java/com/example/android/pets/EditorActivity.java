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
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract;
import com.example.android.pets.data.PetContract.PetEntry;
import com.example.android.pets.data.PetDbHelper;

import static java.security.AccessController.getContext;

/**
 * Allows user to create a new pet or edit an existing one.
 */
@SuppressWarnings("ConstantConditions")
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID = 0;

    PetDbHelper mDbHelper;
    SQLiteDatabase db;

    /** EditText field to enter the pet's name */
    private EditText mNameEditText;
    private String mNameString;

    /** EditText field to enter the pet's breed */
    private EditText mBreedEditText;
    private String mBreedString;

    /** EditText field to enter the pet's weight */
    private EditText mWeightEditText;
    private String mWeightString;

    /** EditText field to enter the pet's gender */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    private Uri currentPetUri;
    private String[] projection;

    private boolean mPetHasChanged = false;
    private boolean mPetDeletedInEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        currentPetUri = getIntent().getData();

        if (currentPetUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_pet));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_pet));
            projection = new String[]{
                    PetEntry.COLUMN_BREED,
                    PetEntry._ID,
                    PetEntry.COLUMN_NAME,
                    PetEntry.COLUMN_GENDER,
                    PetEntry.COLUMN_WEIGHT
            };
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();

        mDbHelper = new PetDbHelper(getApplicationContext());
        db = mDbHelper.getReadableDatabase();
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = 0; // Unknown
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Do nothing for now
                insertOrUpdatePet();
                //finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                ////NavUtils.navigateUpFromSameTask(this);
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (currentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void insertOrUpdatePet() {
        mNameString = mNameEditText.getText().toString().trim();
        mBreedString = mBreedEditText.getText().toString().trim();
        mWeightString = mWeightEditText.getText().toString().trim();
        int mWeightInt;
        if (mWeightString.equals("")) {
            mWeightInt = -1;
        } else {
            mWeightInt = Integer.parseInt(mWeightString);
        }


        if (currentPetUri != null &&
                TextUtils.isEmpty(mNameString) || TextUtils.isEmpty(mBreedString) ||
                TextUtils.isEmpty(mWeightString) || mGender == PetEntry.GENDER_UNKNOWN) {
            Toast toast;
            toast = Toast.makeText(getApplicationContext(), "Error with updating pet", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        ContentValues values = new ContentValues();

        values.put(PetEntry.COLUMN_NAME, mNameString);
        values.put(PetEntry.COLUMN_BREED, mBreedString);
        values.put(PetEntry.COLUMN_WEIGHT, mWeightInt);
        values.put(PetEntry.COLUMN_GENDER, mGender);

        Uri uriReturned;
        int rowsNumberReturned;
        if (currentPetUri == null) {
            uriReturned = getContentResolver().insert(PetEntry.CONTENT_URI, values);
            long rowId = ContentUris.parseId(uriReturned);
            Toast toast;
            if (rowId != -1 && rowId != -2) {
                toast = Toast.makeText(this, "Pet saved with id: " + rowId, Toast.LENGTH_SHORT);
                toast.show();
                finish();
            } else if (rowId != -2){
                toast = Toast.makeText(this, "Error with saving pet", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            int id = (int) ContentUris.parseId(currentPetUri);
            rowsNumberReturned = getContentResolver().update(
                    currentPetUri,
                    values,
                    null,
                    null
            );
            Toast toast;
            if (rowsNumberReturned != -1) {
                toast = Toast.makeText(this, "Pet updated", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            } else {
                toast = Toast.makeText(this, "Error with updating pet", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = PetEntry._ID + "=?";
        String[] selectionArgs = {String.valueOf(ContentUris.parseId(currentPetUri))};
        return new CursorLoader(
                getApplication(),
                PetEntry.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mPetDeletedInEditMode) return;
        data.moveToFirst();

        int nameColumnId = data.getColumnIndexOrThrow(PetEntry.COLUMN_NAME);
        int breedColumnId = data.getColumnIndexOrThrow(PetEntry.COLUMN_BREED);
        int genderColumnId = data.getColumnIndexOrThrow(PetEntry.COLUMN_GENDER);
        int weightColumnId = data.getColumnIndexOrThrow(PetEntry.COLUMN_WEIGHT);

        String nameString = data.getString(nameColumnId);
        String breedString = data.getString(breedColumnId);
        int genderInt = data.getInt(genderColumnId);
        String weightString = data.getString(weightColumnId);

        mNameEditText.setText(nameString);
        mBreedEditText.setText(breedString);
        switch (genderInt) {
            case PetEntry.GENDER_UNKNOWN:
                mGenderSpinner.setSelection(PetEntry.GENDER_UNKNOWN);
                break;
            case PetEntry.GENDER_MALE:
                mGenderSpinner.setSelection(PetEntry.GENDER_MALE);
                break;
            case PetEntry.GENDER_FEMALE:
                mGenderSpinner.setSelection(PetEntry.GENDER_FEMALE);
                break;
        }
        mWeightEditText.setText(weightString);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mBreedEditText.setText("");
        mGenderSpinner.setSelection(0);
        mWeightEditText.setText("");
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPetHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
                //getContentResolver().notifyChange(PetContract.BASE_CONTENT_URI, null);

                //finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        // Only perform the delete if this is an existing pet.
        if (currentPetUri != null) {
            mPetDeletedInEditMode = true;
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the pet that we want.
            int rowsDeleted = getContentResolver().delete(currentPetUri, null, null);
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                // Close the activity
                finish();
                Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
                        Toast.LENGTH_SHORT).show();
               // getContentResolver().notifyChange(currentPetUri, null);

            } else {
                // Otherwise, the delete was successful and we can display a toast.
                // Close the activity
                finish();
                Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
                        Toast.LENGTH_SHORT).show();
                //getContentResolver().notifyChange(currentPetUri, null);

            }

        }
    }
}