package com.ifmo.youshare;

import android.hardware.Camera;
import android.view.Surface;

public interface VideoStreamingInterface {
    void open(String url, Camera camera, Surface previewSurface);

    void close();
}
