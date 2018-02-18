package com.ifmo.youshare;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.android.volley.toolbox.ImageLoader;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.ifmo.youshare.util.EventData;
import com.ifmo.youshare.util.NetworkSingleton;
import com.ifmo.youshare.util.Utils;
import com.ifmo.youshare.util.YouTubeApi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EventsListFragment.Callbacks {

    public static final String APP_NAME = "YouShare";
    public static final String ACCOUNT_KEY = "accountName";
    private static final int REQUEST_GMS_ERROR_DIALOG = 0;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int NEW_EVENT_SETTINGS_INTENT_REQUEST = 1;

    private ImageLoader mImageLoader;
    private GoogleAccountCredential credential;
    private String mChosenAccountName;
    private EventsListFragment mEventsListFragment;

    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ensureLoader();

        credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(Utils.SCOPES));
        // set exponential backoff policy
        credential.setBackOff(new ExponentialBackOff());

        if (savedInstanceState != null) {
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY);
        } else {
            loadAccount();
        }

        credential.setSelectedAccountName(mChosenAccountName);

        mEventsListFragment = (EventsListFragment) getFragmentManager()
                .findFragmentById(R.id.list_fragment);
    }

    private void ensureLoader() {
        if (mImageLoader == null) {
            // Get the ImageLoader through your singleton class.
            mImageLoader = NetworkSingleton.getInstance(this).getImageLoader();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public ImageLoader onGetImageLoader() {
        ensureLoader();
        return mImageLoader;
    }

    @Override
    public void onEventSelected(EventData liveBroadcast) {
//        startStreaming(liveBroadcast);
    }

    @Override
    public void onConnected(String connectedAccountName) {
        mChosenAccountName = connectedAccountName;
        saveAccount();
        credential.setSelectedAccountName(mChosenAccountName);
        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GMS_ERROR_DIALOG:
                break;
            case NEW_EVENT_SETTINGS_INTENT_REQUEST:
                if (RESULT_OK == resultCode) {
                    new CreateLiveEventTask(
                            data.getStringExtra("name"),
                            data.getStringExtra("description"),
                            data.getStringExtra("privacy")).execute();
                }
        }
    }


    private void saveAccount() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).apply();
    }

    private void loadData() {
        if (mChosenAccountName == null) {
            return;
        }
        //Loading events
        new GetLiveEventsTask().execute();
    }


    private class GetLiveEventsTask extends
            AsyncTask<Void, Void, List<EventData>> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.loadingEvents), true);
        }

        @Override
        protected List<EventData> doInBackground(
                Void... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                return YouTubeApi.getLiveEvents(youtube);
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {
            if (fetchedEvents == null) {
                progressDialog.dismiss();
                return;
            }

            mEventsListFragment.setEvents(fetchedEvents);
            progressDialog.dismiss();
        }
    }

    public void createEvent(View view) {
        Intent intent = new Intent(this, NewEventSettingsActivity.class);
        startActivityForResult(intent, NEW_EVENT_SETTINGS_INTENT_REQUEST);
    }

    private class CreateLiveEventTask extends
            AsyncTask<Void, Void, List<EventData>> {
        private final String name;
        private final String description;
        private final String privacy;
        private ProgressDialog progressDialog;

        CreateLiveEventTask(String name, String description, String privacy) {
            this.name = name;
            this.description = description;
            this.privacy = privacy;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null,
                    getResources().getText(R.string.creatingEvent), true);
        }

        @Override
        protected List<EventData> doInBackground(Void... params) {
            YouTube youtube = new YouTube.Builder(transport, jsonFactory,
                    credential).setApplicationName(APP_NAME)
                    .build();
            try {
                String date = new Date().toString();
                YouTubeApi.createLiveEvent(youtube, name, description, privacy);
                return YouTubeApi.getLiveEvents(youtube);

            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(MainActivity.APP_NAME, "", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(
                List<EventData> fetchedEvents) {

            progressDialog.dismiss();
        }
    }

}
