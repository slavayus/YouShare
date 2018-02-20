package com.ifmo.youshare;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;

import com.google.android.apps.watchme.Ffmpeg;


public class VideoStreamingConnection implements VideoStreamingInterface {
    // CONSTANTS.
    private static final int AUDIO_SAMPLE_RATE = 44100;

    // Member variables.
    private VideoFrameGrabber videoFrameGrabber;
    private AudioFrameGrabber audioFrameGrabber;
    private Object frame_mutex = new Object();
    private boolean encoding;

    @Override
    public void open(String url, Camera camera, Surface previewSurface) {
        Log.d(MainActivity.APP_NAME, "open");

        videoFrameGrabber = new VideoFrameGrabber();
        videoFrameGrabber.setFrameCallback(new VideoFrameGrabber.FrameCallback() {
            @Override
            public void handleFrame(byte[] yuv_image) {
                if (encoding) {
                    synchronized (frame_mutex) {
                        int encoded_size = Ffmpeg.encodeVideoFrame(yuv_image);

                        // Logging.Verbose("Encoded video! Size = " + encoded_size);
                    }
                }
            }
        });

        audioFrameGrabber = new AudioFrameGrabber();
        audioFrameGrabber.setFrameCallback(new AudioFrameGrabber.FrameCallback() {
            @Override
            public void handleFrame(short[] audioData, int length) {
                if (encoding) {
                    synchronized (frame_mutex) {
                        int encoded_size = Ffmpeg.encodeAudioFrame(audioData, length);

                        // Logging.Verbose("Encoded audio! Size = " + encoded_size);
                    }
                }
            }
        });

        synchronized (frame_mutex) {
            Size previewSize = videoFrameGrabber.start(camera);
            audioFrameGrabber.start(AUDIO_SAMPLE_RATE);

            int width = previewSize.width;
            int height = previewSize.height;
            encoding = Ffmpeg.init(width, height, AUDIO_SAMPLE_RATE, url);

            Log.i(MainActivity.APP_NAME, "Ffmpeg.init() returned " + encoding);
        }
    }

    @Override
    public void close() {
        Log.i(MainActivity.APP_NAME, "close");

        videoFrameGrabber.stop();
        audioFrameGrabber.stop();

        encoding = false;
        if (encoding) {
            Ffmpeg.shutdown();
        }
    }
}
