/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kseek.streaming.video;

import android.annotation.SuppressLint;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.service.textservice.SpellCheckerService.Session;
import android.util.Base64;
import android.util.Log;

import net.kseek.streaming.SessionBuilder;
import net.kseek.streaming.exceptions.ConfNotSupportedException;
import net.kseek.streaming.exceptions.StorageUnavailableException;
import net.kseek.streaming.hw.EncoderDebugger;
import net.kseek.streaming.mp4.MP4Config;
import net.kseek.streaming.rtp.H264Packetizer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A class for streaming H.264 from the camera of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setVideoQuality(VideoQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class H264Stream extends VideoStream
{

    public final static String TAG = "H264Stream";

    private Semaphore lock = new Semaphore(0);
    private MP4Config config;

    /**
     * Constructs the H.264 stream.
     * Uses CAMERA_FACING_BACK by default.
     */
    public H264Stream()
    {
        this(CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * Constructs the H.264 stream.
     *
     * @param cameraId Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
     * @throws IOException
     */
    public H264Stream(int cameraId)
    {
        super(cameraId);
        mimeType = "video/avc";
        cameraImageFormat = ImageFormat.NV21;
        videoEncoder = MediaRecorder.VideoEncoder.H264;
        packetizer = new H264Packetizer();
    }

    /**
     * Returns a description of the stream using SDP. It can then be included in an SDP file.
     */
    public synchronized String getSessionDescription() throws IllegalStateException
    {
        if (config == null) throw new IllegalStateException("You need to call configure() first !");
        return "m=video " + String.valueOf(getDestinationPorts()[0]) + " RTP/AVP 96\r\n" +
                "a=rtpmap:96 H264/90000\r\n" +
                "a=fmtp:96 packetization-mode=1;profile-level-id=" + config.getProfileLevel() + ";" +
                "sprop-parameter-sets=" + config.getB64SPS() + "," + config.getB64PPS() + ";\r\n";
    }

    /**
     * Starts the stream.
     * This will also open the camera and display the preview if {@link #startPreview()} has not already been called.
     */
    public synchronized void start() throws IllegalStateException, IOException
    {
        if (!streaming) {
            configure();
            byte[] pps = Base64.decode(config.getB64PPS(), Base64.NO_WRAP);
            byte[] sps = Base64.decode(config.getB64SPS(), Base64.NO_WRAP);
            ((H264Packetizer) packetizer).setStreamParameters(pps, sps);
            super.start();
        }
    }

    /**
     * Configures the stream. You need to call this before calling {@link #getSessionDescription()} to apply
     * your configuration of the stream.
     */
    public synchronized void configure() throws IllegalStateException, IOException
    {
        super.configure();
        currentMode = requestedMode;
        currentQuality = requestedQuality.clone();
        config = testH264();
    }

    /**
     * Tests if streaming with the given configuration (bit rate, frame rate, resolution) is possible
     * and determines the pps and sps. Should not be called by the UI thread.
     **/
    private MP4Config testH264() throws IllegalStateException, IOException
    {
        if (currentMode != MODE_MEDIARECORDER_API) return testMediaCodecAPI();
        else return testMediaRecorderAPI();
    }

    @SuppressLint("NewApi")
    private MP4Config testMediaCodecAPI() throws RuntimeException, IOException
    {
        createCamera();
        updateCamera();
        try {
//			if (currentQuality.resWidth >=1300) {
//				// Using the MediaCodec API with the buffer method for high resolutions is too slow
//				currentMode = MODE_MEDIARECORDER_API;
//			}
            EncoderDebugger debugger = EncoderDebugger.debug(settings, currentQuality.resWidth, currentQuality.resHeight);
            return new MP4Config(debugger.getB64SPS(), debugger.getB64PPS());
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT < 23) {
                // Fallback on the old streaming method using the MediaRecorder API
                Log.e(TAG, "Resolution not supported with the MediaCodec API, we fallback on the old streaming method.");
                currentMode = MODE_MEDIARECORDER_API;
                return testH264();
            }

            return null;
        }
    }

    // Should not be called by the UI thread
    private MP4Config testMediaRecorderAPI() throws RuntimeException, IOException
    {
        final String KEY_MP4_CONFIG = PREF_PREFIX + "h264-mr-" + requestedQuality.framerate + "," +
                requestedQuality.resWidth + "," +
                requestedQuality.resHeight;

        Log.e(TAG, "Testing H264 support using: " + KEY_MP4_CONFIG);

        if (settings != null) {
            if (settings.contains(KEY_MP4_CONFIG)) {
                String[] s = settings.getString(KEY_MP4_CONFIG, "").split(",");
                return new MP4Config(s[0], s[1], s[2]);
            }
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new StorageUnavailableException("No external storage or external storage not ready !");
        }

        final String FILE_NAME = "/camtest-test.mp4";
        final String TEST_FILE = Environment.getExternalStorageDirectory().getPath() + FILE_NAME;

        Log.i(TAG, "Testing H264 support... Test file saved at: " + TEST_FILE);

        try {
            File file = new File(TEST_FILE);
            file.createNewFile();
        } catch (IOException e) {
            throw new StorageUnavailableException(e.getMessage());
        }

        // Save flash state & set it to false so that led remains off while testing h264
        boolean savedFlashState = flashEnabled;
        flashEnabled = false;

        boolean tmpPreviewStarted = previewStarted;

        boolean cameraOpen = camera != null;
        createCamera();

        // Stops the preview if needed
        if (previewStarted) {
            lockCamera();
            try {
                camera.stopPreview();
            } catch (Exception e) {}
            previewStarted = false;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        unlockCamera();

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setCamera(camera);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setVideoEncoder(videoEncoder);
            mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
            mediaRecorder.setVideoSize(requestedQuality.resWidth, requestedQuality.resHeight);
            mediaRecorder.setVideoFrameRate(requestedQuality.framerate);
            mediaRecorder.setVideoEncodingBitRate((int) (requestedQuality.bitrate * 0.8));
            mediaRecorder.setOutputFile(TEST_FILE);
            mediaRecorder.setMaxDuration(3000);

            // We wait a little and stop recording
            mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener()
            {
                public void onInfo(MediaRecorder mr, int what, int extra)
                {
                    Log.d(TAG, "MediaRecorder callback called !");
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG, "MediaRecorder: MAX_DURATION_REACHED");
                    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        Log.d(TAG, "MediaRecorder: MAX_FILESIZE_REACHED");
                    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
                        Log.d(TAG, "MediaRecorder: INFO_UNKNOWN");
                    } else {
                        Log.d(TAG, "WTF ?");
                    }
                    lock.release();
                }
            });

            // Start recording
            mediaRecorder.prepare();
            mediaRecorder.start();

            if (lock.tryAcquire(6, TimeUnit.SECONDS)) {
                Log.d(TAG, "MediaRecorder callback was called :)");
                Thread.sleep(400);
            } else {
                Log.d(TAG, "MediaRecorder callback was not called after 6 seconds... :(");
            }
        } catch (IOException e) {
            throw new ConfNotSupportedException(e.getMessage());
        } catch (RuntimeException e) {
            throw new ConfNotSupportedException(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder = null;
            lockCamera();
            if (!cameraOpen) destroyCamera();
            // Restore flash state
            flashEnabled = savedFlashState;
            if (tmpPreviewStarted) {
                // If the preview was started before the test, we try to restart it.
                try {
                    startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Retrieve SPS & PPS & ProfileId with MP4Config
        MP4Config config = new MP4Config(TEST_FILE);

        // Delete dummy video
        File file = new File(TEST_FILE);
        if (!file.delete()) Log.e(TAG, "Temp file could not be erased");

        Log.i(TAG, "H264 Test succeded...");

        // Save test result
        if (settings != null) {
            Editor editor = settings.edit();
            editor.putString(KEY_MP4_CONFIG, config.getProfileLevel() + "," + config.getB64SPS() + "," + config.getB64PPS());
            editor.apply();
        }

        return config;
    }
}
