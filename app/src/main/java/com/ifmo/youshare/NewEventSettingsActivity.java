package com.ifmo.youshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

public class NewEventSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void saveNewEvent(View view) {
        int numberOfValidFields = 0;
        Intent intent = new Intent();

        String eventName = ((TextInputEditText) findViewById(R.id.new_event_name)).getText().toString().trim();
        if ("".equals(eventName)) {
            Toast.makeText(this, R.string.enter_event_name, Toast.LENGTH_SHORT).show();
        } else {
            numberOfValidFields++;
            intent.putExtra("name", eventName);
        }

        intent.putExtra("description", ((TextInputEditText) findViewById(R.id.new_event_description)).getText().toString());

        RadioGroup radioButtonGroup = findViewById(R.id.privacy_radio_group);
        int checkedRadioButtonId = radioButtonGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == -1) {
            Toast.makeText(this, R.string.chose_privacy_event, Toast.LENGTH_SHORT).show();
        } else {
            numberOfValidFields++;

            String resourceEntryName = getResources().getResourceEntryName(checkedRadioButtonId);
            String privacy = resourceEntryName.substring(resourceEntryName.indexOf("_")+1, resourceEntryName.length());

            intent.putExtra("privacy", privacy);
        }

        if (numberOfValidFields == 2) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public void cancelNewEvent(View view) {
        setResult(RESULT_CANCELED, null);
        finish();
    }
}
