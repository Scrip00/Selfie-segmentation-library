package com.scrip0.backremlib;

// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.glutil.EglManager;

import java.util.Timer;
import java.util.TimerTask;

// TODO optimize imports

/**
 * Main activity of MediaPipe example apps.
 */
public class BackActivity {
    private static final String BINARY_GRAPH_NAME = "portrait_segmentation_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.FRONT;

    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private static final boolean FLIP_FRAMES_VERTICALLY = true;

    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        System.loadLibrary("opencv_java3");
    }

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;

    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private BackgroundFrameProcessor processor;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;

    // Handles camera access via the {@link CameraX} Jetpack support library.
    private CameraXPreviewHelper cameraHelper;

    // Stores app context
    private final Context context;

    // Stores view
    private final ViewGroup viewGroup;

    // Store current video-frame number and total number of frames
    private int frame, maxFrame;

    // Stores video second per frame
    private long spf;

    private boolean cropVideo;

    // Stores all video-frames
    private Bitmap[] videoFrames;
    private Timer timer;

    // Needed to clean timer if video stopped
    private boolean isVideoPlaying;

    public BackActivity(Context context, ViewGroup viewGroup) {
        Log.d("LOL", "CREATED");
        this.context = context;
        this.viewGroup = viewGroup;

        isVideoPlaying = false;

        previewDisplayView = new SurfaceView(context);
        setupPreviewDisplayView();

        AndroidAssetUtil.initializeNativeAssetManager(context);

        eglManager = new EglManager(null);
        processor =
                new BackgroundFrameProcessor(
                        context,
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        "img_path",
                        OUTPUT_VIDEO_STREAM_NAME);
        processor.getVideoSurfaceOutput().setFlipY(FLIP_FRAMES_VERTICALLY);
    }

    private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        viewGroup.addView(previewDisplayView);
        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(null);
                            }
                        });
    }

    public void pause() {
        converter.close();
        // Hide preview display until we re-open the camera again.
        previewDisplayView.setVisibility(View.GONE);
        cleanTimer();
    }

    public void resume() {
        converter = new ExternalTextureConverter(eglManager.getContext(), 2);
        converter.setFlipY(FLIP_FRAMES_VERTICALLY);
        converter.setConsumer(processor);
        startCamera();
        if (isVideoPlaying) {
            if (spf == -1) {
                partymode();
            } else {
                continueVideo();
            }
        }
    }

    public void setColor(int color) {
        isVideoPlaying = false;
        cleanTimer();
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0F, 0F, (float) 2, (float) 2, paint);
        setImage(bitmap, false);
    }

    public void setColorARGB(int a, int r, int g, int b) {
        isVideoPlaying = false;
        cleanTimer();
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setARGB(a, r, g, b);
        canvas.drawRect(0F, 0F, (float) 2, (float) 2, paint);
        setImage(bitmap, false);
    }

    public void setImageBackground(Bitmap background, boolean crop) {
        Log.d("LOL", "BACK");
        isVideoPlaying = false;
        cleanTimer();
        setImage(background, crop);
    }

    private void setImage(Bitmap background, boolean crop) throws NullPointerException {
        if (background == null && crop)
            throw new NullPointerException("Background image should not be null");
        if (previewDisplayView.getMeasuredHeight() == 0) {
            previewDisplayView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                    if (!isVideoPlaying) cleanTimer();
                    if (crop) {
                        processor.setImageBackground(cropImg(background));
                    } else
                        processor.setImageBackground(background);
                }
            });
        } else {
            if (!isVideoPlaying) cleanTimer();
            if (crop) {
                processor.setImageBackground(cropImg(background));
            } else
                processor.setImageBackground(background);
        }
    }

    public void partymode() {
        spf = -1;
        isVideoPlaying = true;
        cleanTimer();
        timer = new Timer();
        final int ARRAY_SIZE = 300;
        final int MAX_COLOR = 360;
        final int MIN_COLOR = 0;
        double jump = (MAX_COLOR - MIN_COLOR) / (ARRAY_SIZE * 1.0);
        int[] colors = new int[ARRAY_SIZE];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.HSVToColor(new float[]{(float) (MIN_COLOR + (jump * i)), 1.0f, 1.0f});
        }
        timer.schedule(new TimerTask() {
            int i = 0;

            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                paint.setColor(colors[i]);
                i++;
                if (i >= colors.length) i = 0;
                canvas.drawRect(0F, 0F, (float) 2, (float) 2, paint);
                setImage(bitmap, false);
            }
        }, 0, 1);
    }

    public Bitmap cropImg(Bitmap img) {
        int width = previewDisplayView.getMeasuredWidth();
        int height = previewDisplayView.getMeasuredHeight();
        double scale = (double) width / height;
        if ((double) width / height <= (double) img.getWidth() / img.getHeight()) {
            return Bitmap.createBitmap(img, (int) (img.getWidth() - scale * img.getHeight()) / 2, 0, (int) (scale * img.getHeight()), img.getHeight());
        } else {
            return Bitmap.createBitmap(img, 0, (int) (img.getHeight() - scale * img.getWidth()) / 2, img.getWidth(), (int) (scale / img.getWidth()));
        }
    }

    public void cleanTimer() {
        if (timer != null) timer.cancel();
    }

    @SuppressLint("NewApi")
    public void setVideo(String path, boolean crop) {
        cleanTimer();
        isVideoPlaying = true;
        MediaMetadataRetriever mret = new MediaMetadataRetriever();
        mret.setDataSource(path);
        cropVideo = crop;

        maxFrame = Integer.parseInt(mret.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));

        spf = Integer.parseInt(mret.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / maxFrame;
        videoFrames = new Bitmap[maxFrame];
        int preloadCount = 60;
        if (maxFrame < preloadCount) {
            for (int i = 0; i < maxFrame; i++) {
                videoFrames[i] = ARGBBitmap(mret.getFrameAtIndex(i));
            }
        } else {
            long elapsedTime = System.currentTimeMillis();
            for (int i = 0; i < preloadCount; i++) {
                videoFrames[i] = ARGBBitmap(mret.getFrameAtIndex(i));
            }
            elapsedTime = System.currentTimeMillis() - elapsedTime;
            double loadTime = (int) (elapsedTime / preloadCount);

            int preloadFrames;
            if (loadTime < (double) spf) {
                preloadFrames = preloadCount;
            } else preloadFrames = (int) ((double) maxFrame * (loadTime - (double) spf) / loadTime);

            for (int i = preloadCount; i < preloadFrames; i++) {
                videoFrames[i] = ARGBBitmap(mret.getFrameAtIndex(i));
            }

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = preloadFrames; i < maxFrame; i++) {
                        videoFrames[i] = ARGBBitmap(mret.getFrameAtIndex(i));
                    }
                    mret.release();
                }
            });
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setVideoFrame(crop);
            }
        }, 0, spf);
    }

    private void continueVideo() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setVideoFrame(cropVideo);
            }
        }, 0, spf);
    }

    @SuppressLint("NewApi")
    private void setVideoFrame(boolean crop) {
        if (frame > maxFrame - 1) frame = 0;
        if (videoFrames == null) return;
        if (videoFrames[frame] == null) {
            frame = 0;
        } else {
            setImage(videoFrames[frame], crop);
            frame++;
        }
        frame++;
    }

    private Bitmap ARGBBitmap(Bitmap img) {
        return img.copy(Bitmap.Config.ARGB_8888, true);
    }

    protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraHelper.isCameraRotated();

        // Configure the output width and height as the computed display size.
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    protected Size computeViewSize(int width, int height) {
        return new Size(width, height);
    }

    public void startCamera() {
        cameraHelper = new CameraXPreviewHelper();
        previewFrameTexture = converter.getSurfaceTexture();
        cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    previewFrameTexture = surfaceTexture;
                    // Make the display view visible to start showing the preview. This triggers the
                    // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
                    previewDisplayView.setVisibility(View.VISIBLE);
                });
        cameraHelper.startCamera((Activity) context, CAMERA_FACING, /*surfaceTexture=*/ null);
    }
}