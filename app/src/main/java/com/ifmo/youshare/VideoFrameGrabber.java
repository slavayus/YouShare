package com.ifmo.youshare;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;

public class VideoFrameGrabber {
    // Member variables
    private Camera camera;
    private FrameCallback frameCallback;

    public void setFrameCallback(FrameCallback callback) {
        frameCallback = callback;
    }

    /**
     * Starts camera recording to buffer.
     *
     * @param camera - Camera to be recorded.
     * @return preview size.
     */
    public Size start(Camera camera) {
        this.camera = camera;

        Camera.Parameters params = camera.getParameters();
        params.setPreviewSize(StreamerActivity.CAMERA_WIDTH, StreamerActivity.CAMERA_HEIGHT);
        camera.setParameters(params);

        Size previewSize = params.getPreviewSize();
        int bufferSize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(
                params.getPreviewFormat());
        camera.addCallbackBuffer(new byte[bufferSize]);

        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] yuv_image, Camera camera) {
                if (frameCallback != null) {
                    frameCallback.handleFrame(yuv_image);
                }
                camera.addCallbackBuffer(yuv_image);
            }
        });

        return previewSize;
    }

    public void stop() {
        camera.setPreviewCallbackWithBuffer(null);
        camera = null;
    }

    public interface FrameCallback {
        void handleFrame(byte[] yuv_image);
    }
}
