package com.ifmo.youshare;

import android.app.Activity;
import android.app.Fragment;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusOneButton;
import com.google.android.gms.plus.model.people.Person;
import com.ifmo.youshare.util.EventData;

import java.util.List;

public class EventsListFragment extends Fragment implements
        ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = EventsListFragment.class.getName();
    private Callbacks mCallbacks;
    private ImageLoader mImageLoader;
    private GoogleApiClient mGoogleApiClient;
    private GridView mGridView;

    public EventsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View listView = inflater.inflate(R.layout.list_fragment, container,
                false);
        mGridView = listView.findViewById(R.id.grid_view);
        TextView emptyView = listView
                .findViewById(android.R.id.empty);
        mGridView.setEmptyView(emptyView);
        return listView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setProfileInfo();
    }

    public void setEvents(List<EventData> events) {
        if (!isAdded()) {
            return;
        }

        mGridView.setAdapter(new LiveEventAdapter(events));
    }

    public void setProfileInfo() {
        if (mGoogleApiClient.isConnected()
                && Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (!currentPerson.isPlusUser()) {
                Toast.makeText(getActivity(),
                        R.string.you_need_be_plus_users,
                        Toast.LENGTH_SHORT).show();
                mGoogleApiClient.clearDefaultAccountAndReconnect();
            } else {

                if (currentPerson.hasImage()) {
                    ((LinearLayout) getActivity().findViewById(R.id.user_info)).removeView(getActivity().findViewById(R.id.default_avatar));

                    NetworkImageView networkAvatarView = getActivity().findViewById(R.id.network_avatar);
                    networkAvatarView.setVisibility(View.VISIBLE);
                    networkAvatarView.setImageUrl(currentPerson.getImage().getUrl(), mImageLoader);
                }

                if (currentPerson.hasName() && currentPerson.getName().hasFamilyName()) {
                    ((TextView) getActivity().findViewById(R.id.user_family_name))
                            .setText(currentPerson.getName().getFamilyName());
                }

                if (currentPerson.hasName() && currentPerson.getName().hasGivenName()) {
                    ((TextView) getActivity().findViewById(R.id.user_given_name))
                            .setText(currentPerson.getName().getGivenName());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mGridView.getAdapter() != null) {
            ((LiveEventAdapter) mGridView.getAdapter())
                    .notifyDataSetChanged();
        }

        setProfileInfo();
        mCallbacks.onConnected(Plus.AccountApi.getAccountName(mGoogleApiClient));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            Toast.makeText(getActivity(),
                    R.string.connection_to_google_play_failed,
                    Toast.LENGTH_SHORT).show();

            Log.e(TAG,
                    String.format(
                            "Connection to Play Services Failed, error: %d, reason: %s",
                            connectionResult.getErrorCode(),
                            connectionResult.toString()));
            try {
                connectionResult.startResolutionForResult(getActivity(), 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        mImageLoader = mCallbacks.onGetImageLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        mImageLoader = null;
    }

    public interface Callbacks {
        public ImageLoader onGetImageLoader();

        public void onEventSelected(EventData event);

        public void onConnected(String connectedAccountName);
    }

    private class LiveEventAdapter extends BaseAdapter {
        private List<EventData> mEvents;

        private LiveEventAdapter(List<EventData> events) {
            mEvents = events;
        }

        @Override
        public int getCount() {
            return mEvents.size();
        }

        @Override
        public Object getItem(int i) {
            return mEvents.get(i);
        }

        @Override
        public long getItemId(int i) {
            return mEvents.get(i).getId().hashCode();
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup container) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(
                        R.layout.list_item, container, false);
            }

            EventData event = mEvents.get(position);
            ((TextView) convertView.findViewById(android.R.id.text1))
                    .setText(event.getTitle());
            ((NetworkImageView) convertView.findViewById(R.id.thumbnail)).setImageUrl(event.getThumbUri(), mImageLoader);
            if (mGoogleApiClient.isConnected()) {
                ((PlusOneButton) convertView.findViewById(R.id.plus_button))
                        .initialize(event.getWatchUri(), null);
            }
            convertView.findViewById(R.id.main_target).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mCallbacks.onEventSelected(mEvents.get(position));
                        }
                    });
            return convertView;
        }
    }
}
