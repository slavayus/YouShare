package com.ifmo.youshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.android.volley.toolbox.ImageLoader;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.ifmo.youshare.util.EventData;
import com.ifmo.youshare.util.NetworkSingleton;
import com.ifmo.youshare.util.Utils;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EventsListFragment.Callbacks {

    public static final String APP_NAME = "YouShare";
    public static final String ACCOUNT_KEY = "accountName";
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_GMS_ERROR_DIALOG = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;

    private ImageLoader mImageLoader;
    private GoogleAccountCredential credential;
    private String mChosenAccountName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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

//        mEventsListFragment = (EventsListFragment) getFragmentManager()
//                .findFragmentById(R.id.list_fragment);
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
        saveAccount(connectedAccountName);
        credential.setSelectedAccountName(connectedAccountName);
        System.out.println(credential.getSelectedAccountName());
        //        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GMS_ERROR_DIALOG:
                break;
        }
    }


    private void saveAccount(String connectedAccountName) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putString(ACCOUNT_KEY, connectedAccountName).apply();
    }
}
