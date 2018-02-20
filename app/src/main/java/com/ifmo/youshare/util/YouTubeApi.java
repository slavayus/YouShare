package com.ifmo.youshare.util;

import android.util.Log;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.LiveBroadcasts.Transition;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.IngestionInfo;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastContentDetails;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamListResponse;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.MonitorStreamInfo;
import com.ifmo.youshare.MainActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class YouTubeApi {

    public static final String RTMP_URL_KEY = "rtmp://a.rtmp.youtube.com/live2";
    public static final String BROADCAST_ID_KEY = "2s9r-87tc-2pgm-18tv";
    private static final int FUTURE_DATE_OFFSET_MILLIS = 5 * 1000;
    private static final long MAX_BROADCASTS = 50;

    public static void createLiveEvent(YouTube youtube, String name, String description,
                                       String privacy) {
        // We need a date that's in the proper ISO format and is in the future,
        // since the API won't
        // create events that start in the past.
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long futureDateMillis = System.currentTimeMillis()
                + FUTURE_DATE_OFFSET_MILLIS;
        Date futureDate = new Date();
        futureDate.setTime(futureDateMillis);
        String date = dateFormat.format(futureDate);

        Log.i(MainActivity.APP_NAME, String.format(
                "Creating event: name='%s', description='%s', date='%s'.",
                name, description, date));

        try {

            LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet()
                    .setTitle(name)
                    .setDescription(description)
                    .setScheduledStartTime(new DateTime(futureDate));

            MonitorStreamInfo monitorStream = new MonitorStreamInfo()
                    .setEnableMonitorStream(false);

            LiveBroadcastContentDetails contentDetails = new LiveBroadcastContentDetails();
            contentDetails.setMonitorStream(monitorStream);

            // Create LiveBroadcastStatus with privacy status.
            LiveBroadcastStatus status = new LiveBroadcastStatus()
                    .setPrivacyStatus(privacy);

            LiveBroadcast broadcast = new LiveBroadcast()
                    .setKind("youtube#liveBroadcast")
                    .setSnippet(broadcastSnippet)
                    .setStatus(status)
                    .setContentDetails(contentDetails);

            // Create the insert request
            YouTube.LiveBroadcasts.Insert liveBroadcastInsert = youtube
                    .liveBroadcasts()
                    .insert("snippet,status,contentDetails", broadcast);

            // Request is executed and inserted broadcast is returned
            LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();

            // Create a snippet with title.
            LiveStreamSnippet streamSnippet = new LiveStreamSnippet()
                    .setTitle(name);

            // Create content distribution network with format and ingestion
            // type.
            CdnSettings cdn = new CdnSettings()
                    .setFormat("1080p")
                    .setIngestionType("rtmp");

            LiveStream stream = new LiveStream()
                    .setKind("youtube#liveStream")
                    .setSnippet(streamSnippet)
                    .setCdn(cdn);

            // Create the insert request
            YouTube.LiveStreams.Insert liveStreamInsert = youtube.liveStreams()
                    .insert("snippet,cdn", stream);

            // Request is executed and inserted stream is returned
            LiveStream returnedStream = liveStreamInsert.execute();

            // Create the bind request
            YouTube.LiveBroadcasts.Bind liveBroadcastBind = youtube
                    .liveBroadcasts().bind(returnedBroadcast.getId(),
                            "id,contentDetails");

            // Set stream id to bind
            liveBroadcastBind.setStreamId(returnedStream.getId());

            // Request is executed and bound broadcast is returned
            liveBroadcastBind.execute();

        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: "
                    + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getStackTrace());
            t.printStackTrace();
        }
    }

    // TODO: Catch those exceptions and handle them here.
    public static List<EventData> getLiveEvents(
            YouTube youtube) throws IOException {
        Log.i(MainActivity.APP_NAME, "Requesting live events.");

        YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube
                .liveBroadcasts()
                .list("id,snippet,contentDetails")
                .setBroadcastStatus("upcoming")
                .setMaxResults(MAX_BROADCASTS);
        //         liveBroadcastRequest.setMine(true);

        // List request is executed and list of broadcasts are returned
        LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();

        // Get the list of broadcasts associated with the user.
        List<LiveBroadcast> returnedList = returnedListResponse.getItems();

        List<EventData> resultList = new ArrayList<>(returnedList.size());
        EventData event;

        for (LiveBroadcast broadcast : returnedList) {
            event = new EventData();
            event.setEvent(broadcast);
            String streamId = broadcast.getContentDetails().getBoundStreamId();
            if (streamId != null) {
                String ingestionAddress = getIngestionAddress(youtube, streamId);
                event.setIngestionAddress(ingestionAddress);
            }
            resultList.add(event);
        }
        return resultList;
    }

    public static void startEvent(YouTube youtube, String broadcastId)
            throws IOException {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.e(MainActivity.APP_NAME, "", e);
        }

        Transition transitionRequest = youtube.liveBroadcasts().transition(
                "live", broadcastId, "status");
        transitionRequest.execute();
    }

    public static void endEvent(YouTube youtube, String broadcastId)
            throws IOException {
        Transition transitionRequest = youtube.liveBroadcasts().transition(
                "complete", broadcastId, "status");
        transitionRequest.execute();
    }

    public static String getIngestionAddress(YouTube youtube, String streamId)
            throws IOException {
        YouTube.LiveStreams.List liveStreamRequest = youtube.liveStreams()
                .list("cdn");
        liveStreamRequest.setId(streamId);
        LiveStreamListResponse returnedStream = liveStreamRequest.execute();

        List<LiveStream> streamList = returnedStream.getItems();
        if (streamList.isEmpty()) {
            return "";
        }
        IngestionInfo ingestionInfo = streamList.get(0).getCdn().getIngestionInfo();
        return ingestionInfo.getIngestionAddress() + "/"
                + ingestionInfo.getStreamName();
    }
}
