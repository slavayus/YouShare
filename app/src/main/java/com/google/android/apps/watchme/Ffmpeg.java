package com.google.android.apps.watchme;

public class Ffmpeg {


    static {
        System.loadLibrary("ffmpeg");
    }

    public static native boolean init(int width, int height, int audio_sample_rate, String rtmpUrl);

    public static native void shutdown();

    // Returns the size of the encoded frame.
    public static native int encodeVideoFrame(byte[] yuv_image);

    public static native int encodeAudioFrame(short[] audio_data, int length);
}
