//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.scrip0.backremlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioFormat.Builder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.common.base.Preconditions;
import com.google.mediapipe.components.AudioDataConsumer;
import com.google.mediapipe.components.AudioDataProcessor;
import com.google.mediapipe.components.TextureFrameConsumer;
import com.google.mediapipe.components.TextureFrameProcessor;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Graph;
import com.google.mediapipe.framework.GraphService;
import com.google.mediapipe.framework.GraphTextureFrame;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketCallback;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.SurfaceOutput;
import com.google.mediapipe.framework.TextureFrame;
import com.google.mediapipe.proto.CalculatorProto.CalculatorGraphConfig;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

public class BackgroundFrameProcessor implements TextureFrameProcessor, AudioDataProcessor {
    private static final String TAG = "BackgroundFrameProcessor";
    private static final int BYTES_PER_MONO_SAMPLE = 2;
    private static final int AUDIO_ENCODING = 2;
    private List<TextureFrameConsumer> videoConsumers = new ArrayList();
    private List<AudioDataConsumer> audioConsumers = new ArrayList();
    private Graph mediapipeGraph;
    private AndroidPacketCreator packetCreator;
    private BackgroundFrameProcessor.OnWillAddFrameListener addFrameListener;
    private BackgroundFrameProcessor.ErrorListener asyncErrorListener;
    private String videoInputStream;
    private String videoInputStreamCpu;
    private String videoOutputStream;
    private String backgroundOutputStream;
    private SurfaceOutput videoSurfaceOutput;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private String audioInputStream;
    private String audioOutputStream;
    private int numAudioChannels = 1;
    private double audioSampleRate;
    private boolean useImage = false;
    private Bitmap imageBackground;

    public BackgroundFrameProcessor(Context context, long parentNativeContext, String graphName, String inputStream, String backgroundInputStream, @Nullable String outputStream) {
        try {
            this.initializeGraphAndPacketCreator(context, graphName);
            this.addVideoStreams(parentNativeContext, inputStream, backgroundInputStream, outputStream);
        } catch (MediaPipeException var8) {
            Log.e("FrameProcessor", "MediaPipe error: ", var8);
        }

    }

    public BackgroundFrameProcessor(Context context, String graphName) {
        this.initializeGraphAndPacketCreator(context, graphName);
    }

    public BackgroundFrameProcessor(CalculatorGraphConfig graphConfig) {
        this.initializeGraphAndPacketCreator(graphConfig);
    }

    public void setUseImage(boolean use) {
        this.useImage = use;
    }

    public void setImageBackground(Bitmap imageBackground) {
        this.imageBackground = imageBackground;
    }

    private void initializeGraphAndPacketCreator(Context context, String graphName) {
        this.mediapipeGraph = new Graph();
        if ((new File(graphName)).isAbsolute()) {
            this.mediapipeGraph.loadBinaryGraph(graphName);
        } else {
            this.mediapipeGraph.loadBinaryGraph(AndroidAssetUtil.getAssetBytes(context.getAssets(), graphName));
        }

        this.packetCreator = new AndroidPacketCreator(this.mediapipeGraph);
    }

    private void initializeGraphAndPacketCreator(CalculatorGraphConfig graphConfig) {
        this.mediapipeGraph = new Graph();
        this.mediapipeGraph.loadBinaryGraph(graphConfig);
        this.packetCreator = new AndroidPacketCreator(this.mediapipeGraph);
    }

    public void setAsynchronousErrorListener(@Nullable BackgroundFrameProcessor.ErrorListener listener) {
        this.asyncErrorListener = listener;
    }

    public void setAsynchronousErrorListener(@Nullable BackgroundFrameProcessor.ErrorListener listener, @Nullable Handler handler) {
        this.setAsynchronousErrorListener(handler == null ? listener : (e) -> {
            handler.post(() -> {
                listener.onError(e);
            });
        });
    }

    public void addVideoStreams(long parentNativeContext, @Nullable String inputStream, @Nullable String backgroundInputStream, @Nullable String outputStream) {
        this.videoInputStream = inputStream;
        this.videoOutputStream = outputStream;
        this.backgroundOutputStream = backgroundInputStream;
        this.mediapipeGraph.setParentGlContext(parentNativeContext);
        if (this.videoOutputStream != null) {
            this.mediapipeGraph.addPacketCallback(this.videoOutputStream, new PacketCallback() {
                public void process(Packet packet) {
                    List currentConsumers;
                    synchronized (this) {
                        currentConsumers = BackgroundFrameProcessor.this.videoConsumers;
                    }

                    TextureFrameConsumer consumer;
                    GraphTextureFrame frame;
                    for (Iterator var3 = currentConsumers.iterator(); var3.hasNext(); consumer.onNewFrame(frame)) {
                        consumer = (TextureFrameConsumer) var3.next();
                        frame = PacketGetter.getTextureFrame(packet);
                        if (Log.isLoggable("FrameProcessor", Log.VERBOSE)) {
                            Log.v("FrameProcessor", String.format("Output tex: %d width: %d height: %d to consumer %h", frame.getTextureName(), frame.getWidth(), frame.getHeight(), consumer));
                        }
                    }

                }
            });
            this.videoSurfaceOutput = this.mediapipeGraph.addSurfaceOutput(this.videoOutputStream);
        }

    }

    public void addAudioStreams(@Nullable String inputStream, @Nullable String outputStream, int numInputChannels, int numOutputChannels, double audioSampleRateInHz) {
        this.audioInputStream = inputStream;
        this.audioOutputStream = outputStream;
        this.numAudioChannels = numInputChannels;
        this.audioSampleRate = audioSampleRateInHz;
        if (this.audioInputStream != null) {
            Packet audioHeader = this.packetCreator.createTimeSeriesHeader(this.numAudioChannels, this.audioSampleRate);
            this.mediapipeGraph.setStreamHeader(this.audioInputStream, audioHeader);
        }

        if (this.audioOutputStream != null) {
            int outputAudioChannelMask = numOutputChannels == 2 ? 12 : 16;
            final AudioFormat audioFormat = (new Builder()).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate((int) this.audioSampleRate).setChannelMask(outputAudioChannelMask).build();
            this.mediapipeGraph.addPacketCallback(this.audioOutputStream, new PacketCallback() {
                public void process(Packet packet) {
                    List currentAudioConsumers;
                    synchronized (this) {
                        currentAudioConsumers = BackgroundFrameProcessor.this.audioConsumers;
                    }

                    Iterator var3 = currentAudioConsumers.iterator();

                    while (var3.hasNext()) {
                        AudioDataConsumer consumer = (AudioDataConsumer) var3.next();
                        byte[] buffer = PacketGetter.getAudioByteData(packet);
                        ByteBuffer audioData = ByteBuffer.wrap(buffer);
                        consumer.onNewAudioData(audioData, packet.getTimestamp(), audioFormat);
                    }

                }
            });
        }

    }

    public synchronized <T> void setServiceObject(GraphService<T> service, T object) {
        this.mediapipeGraph.setServiceObject(service, object);
    }

    public void setInputSidePackets(Map<String, Packet> inputSidePackets) {
        Preconditions.checkState(!this.started.get(), "setInputSidePackets must be called before the graph is started");
        this.mediapipeGraph.setInputSidePackets(inputSidePackets);
    }

    public void setConsumer(TextureFrameConsumer consumer) {
        synchronized (this) {
            this.videoConsumers = Arrays.asList(consumer);
        }
    }

    public void setAudioConsumer(AudioDataConsumer consumer) {
        synchronized (this) {
            this.audioConsumers = Arrays.asList(consumer);
        }
    }

    public void setVideoInputStreamCpu(String inputStream) {
        this.videoInputStreamCpu = inputStream;
    }

    public void addPacketCallback(String outputStream, PacketCallback callback) {
        this.mediapipeGraph.addPacketCallback(outputStream, callback);
    }

    public void addConsumer(TextureFrameConsumer consumer) {
        synchronized (this) {
            List<TextureFrameConsumer> newConsumers = new ArrayList(this.videoConsumers);
            newConsumers.add(consumer);
            this.videoConsumers = newConsumers;
        }
    }

    public boolean removeConsumer(TextureFrameConsumer listener) {
        synchronized (this) {
            List<TextureFrameConsumer> newConsumers = new ArrayList(this.videoConsumers);
            boolean existed = newConsumers.remove(listener);
            this.videoConsumers = newConsumers;
            return existed;
        }
    }

    public Graph getGraph() {
        return this.mediapipeGraph;
    }

    public AndroidPacketCreator getPacketCreator() {
        return this.packetCreator;
    }

    public SurfaceOutput getVideoSurfaceOutput() {
        return this.videoSurfaceOutput;
    }

    public void close() {
        if (this.started.get()) {
            try {
                this.mediapipeGraph.closeAllPacketSources();
                this.mediapipeGraph.waitUntilGraphDone();
            } catch (MediaPipeException var3) {
                if (this.asyncErrorListener != null) {
                    this.asyncErrorListener.onError(var3);
                } else {
                    Log.e("FrameProcessor", "Mediapipe error: ", var3);
                }
            }

            try {
                this.mediapipeGraph.tearDown();
            } catch (MediaPipeException var2) {
                Log.e("FrameProcessor", "Mediapipe error: ", var2);
            }
        }

    }

    public void preheat() {
        if (!this.started.getAndSet(true)) {
            this.startGraph();
        }

    }

    public void setOnWillAddFrameListener(@Nullable BackgroundFrameProcessor.OnWillAddFrameListener addFrameListener) {
        this.addFrameListener = addFrameListener;
    }

    private boolean maybeAcceptNewFrame(long timestamp) {
        if (!this.started.getAndSet(true)) {
            this.startGraph();
        }

        return true;
    }

    public void onNewFrame(TextureFrame frame) {
        Packet imagePacket = null;
        Packet backgroundImagePacket = null;
        long timestamp = frame.getTimestamp();

        try {
            if (Log.isLoggable("FrameProcessor", Log.VERBOSE)) {
                Log.v("FrameProcessor", String.format("Input tex: %d width: %d height: %d", frame.getTextureName(), frame.getWidth(), frame.getHeight()));
            }

            if (this.maybeAcceptNewFrame(frame.getTimestamp())) {
                if (this.addFrameListener != null) {
                    this.addFrameListener.onWillAddFrame(timestamp);
                }

                if (this.useImage) {
                    imagePacket = this.packetCreator.createImage(frame);
                } else {
                    imagePacket = this.packetCreator.createGpuBuffer(frame);
                }

                if (imageBackground == null) {
                    imageBackground = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(imageBackground);
                    Paint paint = new Paint();
                    paint.setAlpha(0);
                    canvas.drawRect(0F, 0F, (float) 1, (float) 1, paint);
                }

                backgroundImagePacket = this.packetCreator.createRgbImageFrame(imageBackground);
                frame = null;

                try {
                    this.mediapipeGraph.addConsumablePacketToInputStream(this.backgroundOutputStream, backgroundImagePacket, timestamp);
                    this.mediapipeGraph.addConsumablePacketToInputStream(this.videoInputStream, imagePacket, timestamp);
                    imagePacket = null;
                    backgroundImagePacket = null;
                    return;
                } catch (MediaPipeException var10) {
                    if (this.asyncErrorListener == null) {
                        Log.e("FrameProcessor", "Mediapipe error: ", var10);
                        return;
                    }

                    throw var10;
                }
            }
        } catch (RuntimeException var11) {
            if (this.asyncErrorListener != null) {
                this.asyncErrorListener.onError(var11);
                return;
            }

            throw var11;
        } finally {
            if (backgroundImagePacket != null) {
                backgroundImagePacket.release();
            }

            if (imagePacket != null) {
                imagePacket.release();
            }

            if (frame != null) {
                frame.release();
            }

        }

    }

    public void onNewFrame(final Bitmap bitmap, long timestamp) {
        Packet packet = null;

        try {
            if (this.maybeAcceptNewFrame(timestamp)) {
                if (this.addFrameListener != null) {
                    this.addFrameListener.onWillAddFrame(timestamp);
                }

                packet = this.getPacketCreator().createRgbImageFrame(bitmap);

                try {
                    this.mediapipeGraph.addConsumablePacketToInputStream(this.videoInputStreamCpu, packet, timestamp);
                    packet = null;
                    return;
                } catch (MediaPipeException var10) {
                    if (this.asyncErrorListener == null) {
                        Log.e("FrameProcessor", "Mediapipe error: ", var10);
                        return;
                    }

                    throw var10;
                }
            }
        } catch (RuntimeException var11) {
            if (this.asyncErrorListener != null) {
                this.asyncErrorListener.onError(var11);
                return;
            }

            throw var11;
        } finally {
            if (packet != null) {
                packet.release();
            }

        }

    }

    public void waitUntilIdle() {
        try {
            this.mediapipeGraph.waitUntilGraphIdle();
        } catch (MediaPipeException var2) {
            if (this.asyncErrorListener != null) {
                this.asyncErrorListener.onError(var2);
            } else {
                Log.e("FrameProcessor", "Mediapipe error: ", var2);
            }
        }

    }

    private void startGraph() {
        this.mediapipeGraph.startRunningGraph();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onNewAudioData(ByteBuffer audioData, long timestampMicros, AudioFormat audioFormat) {
        Packet audioPacket = null;

        try {
            if (!this.started.getAndSet(true)) {
                this.startGraph();
            }

            if (audioFormat.getChannelCount() == this.numAudioChannels && (double) audioFormat.getSampleRate() == this.audioSampleRate && audioFormat.getEncoding() == 2) {
                Preconditions.checkNotNull(this.audioInputStream);
                int numSamples = audioData.limit() / 2 / this.numAudioChannels;
                audioPacket = this.packetCreator.createAudioPacket(audioData, this.numAudioChannels, numSamples);

                try {
                    this.mediapipeGraph.addConsumablePacketToInputStream(this.audioInputStream, audioPacket, timestampMicros);
                    audioPacket = null;
                    return;
                } catch (MediaPipeException var12) {
                    if (this.asyncErrorListener == null) {
                        Log.e("FrameProcessor", "Mediapipe error: ", var12);
                        return;
                    }

                    throw var12;
                }
            }

            Log.e("FrameProcessor", "Producer's AudioFormat doesn't match FrameProcessor's AudioFormat");
        } catch (RuntimeException var13) {
            if (this.asyncErrorListener != null) {
                this.asyncErrorListener.onError(var13);
                return;
            }

            throw var13;
        } finally {
            if (audioPacket != null) {
                audioPacket.release();
            }

        }

    }

    public void addAudioConsumer(AudioDataConsumer consumer) {
        synchronized (this) {
            List<AudioDataConsumer> newConsumers = new ArrayList(this.audioConsumers);
            newConsumers.add(consumer);
            this.audioConsumers = newConsumers;
        }
    }

    public boolean removeAudioConsumer(AudioDataConsumer consumer) {
        synchronized (this) {
            List<AudioDataConsumer> newConsumers = new ArrayList(this.audioConsumers);
            boolean existed = newConsumers.remove(consumer);
            this.audioConsumers = newConsumers;
            return existed;
        }
    }

    public interface OnWillAddFrameListener {
        void onWillAddFrame(long timestamp);
    }

    public interface ErrorListener {
        void onError(RuntimeException error);
    }
}
