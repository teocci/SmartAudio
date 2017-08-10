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

package net.kseek.streaming;

import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.preference.PreferenceManager;

import net.kseek.streaming.audio.AACStream;
import net.kseek.streaming.audio.AMRNBStream;
import net.kseek.streaming.audio.AudioQuality;
import net.kseek.streaming.audio.AudioStream;
import net.kseek.streaming.gl.SurfaceView;
import net.kseek.streaming.video.H263Stream;
import net.kseek.streaming.video.H264Stream;
import net.kseek.streaming.video.VideoQuality;
import net.kseek.streaming.video.VideoStream;

import java.io.IOException;

/**
 * Call {@link #getInstance()} to get access to the SessionBuilder.
 */
public class SessionBuilder {

	public final static String TAG = "SessionBuilder";

	/** Can be used with {@link #setVideoEncoder}. */
	public final static int VIDEO_NONE = 0;

	/** Can be used with {@link #setVideoEncoder}. */
	public final static int VIDEO_H264 = 1;

	/** Can be used with {@link #setVideoEncoder}. */
	public final static int VIDEO_H263 = 2;

	/** Can be used with {@link #setAudioEncoder}. */
	public final static int AUDIO_NONE = 0;

	/** Can be used with {@link #setAudioEncoder}. */
	public final static int AUDIO_AMRNB = 3;

	/** Can be used with {@link #setAudioEncoder}. */
	public final static int AUDIO_AAC = 5;

	// Default configuration
	private VideoQuality videoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;
	private AudioQuality audioQuality = AudioQuality.DEFAULT_AUDIO_QUALITY;
	private Context context;
	private int videoEncoder = VIDEO_H263;
	private int audioEncoder = AUDIO_AMRNB;
	private int camera = CameraInfo.CAMERA_FACING_BACK;
	private int timeToLive = 64;
	private int orientation = 0;
	private boolean flash = false;
	private SurfaceView surfaceView = null;
	private String origin = null;
	private String destination = null;
	private Session.Callback sessionCallback = null;

	// Removes the default public constructor
	private SessionBuilder() {}

	// The SessionManager implements the singleton pattern
	private static volatile SessionBuilder sessionInstance = null;

	/**
	 * Returns a reference to the {@link SessionBuilder}.
	 * @return The reference to the {@link SessionBuilder}
	 */
	public final static SessionBuilder getInstance() {
		if (sessionInstance == null) {
			synchronized (SessionBuilder.class) {
				if (sessionInstance == null) {
					SessionBuilder.sessionInstance = new SessionBuilder();
				}
			}
		}
		return sessionInstance;
	}	

	/**
	 * Creates a new {@link Session}.
	 * @return The new Session
	 * @throws IOException 
	 */
	public Session build() {
		Session session;

		session = new Session();
		session.setOrigin(origin);
		session.setDestination(destination);
		session.setTimeToLive(timeToLive);
		session.setCallback(sessionCallback);

		switch (audioEncoder) {
		case AUDIO_AAC:
			AACStream stream = new AACStream();
			session.addAudioTrack(stream);
			if (context!=null)
				stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(context));
			break;
		case AUDIO_AMRNB:
			session.addAudioTrack(new AMRNBStream());
			break;
		}

		switch (videoEncoder) {
		case VIDEO_H263:
			session.addVideoTrack(new H263Stream(camera));
			break;
		case VIDEO_H264:
			H264Stream stream = new H264Stream(camera);
			if (context!=null)
				stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(context));
			session.addVideoTrack(stream);
			break;
		}

		if (session.getVideoTrack()!=null) {
			VideoStream video = session.getVideoTrack();
			video.setFlashState(flash);
			video.setVideoQuality(videoQuality);
			video.setSurfaceView(surfaceView);
			video.setPreviewOrientation(orientation);
			video.setDestinationPorts(5000 + (int)(Math.random()*1000));
		}

		if (session.getAudioTrack()!=null) {
			AudioStream audio = session.getAudioTrack();
			audio.setAudioQuality(audioQuality);
			audio.setDestinationPorts(6000 + (int)(Math.random()*1000));
		}

		return session;

	}

	/** 
	 * Access to the context is needed for the H264Stream class to store some stuff in the SharedPreferences.
	 * Note that you should pass the Application context, not the context of an Activity.
	 **/
	public SessionBuilder setContext(Context context) {
		this.context = context;
		return this;
	}

	/** Sets the destination of the session. */
	public SessionBuilder setDestination(String destination) {
		this.destination = destination;
		return this; 
	}

	/** Sets the origin of the session. It appears in the SDP of the session. */
	public SessionBuilder setOrigin(String origin) {
		this.origin = origin;
		return this;
	}

	/** Sets the video stream currentQuality. */
	public SessionBuilder setVideoQuality(VideoQuality quality) {
		this.videoQuality = quality.clone();
		return this;
	}
	
	/** Sets the audio encoder. */
	public SessionBuilder setAudioEncoder(int encoder) {
		this.audioEncoder = encoder;
		return this;
	}
	
	/** Sets the audio currentQuality. */
	public SessionBuilder setAudioQuality(AudioQuality quality) {
		this.audioQuality = quality.clone();
		return this;
	}

	/** Sets the default video encoder. */
	public SessionBuilder setVideoEncoder(int encoder) {
		this.videoEncoder = encoder;
		return this;
	}

	public SessionBuilder setFlashEnabled(boolean enabled) {
		this.flash = enabled;
		return this;
	}

	public SessionBuilder setCamera(int camera) {
		this.camera = camera;
		return this;
	}

	public SessionBuilder setTimeToLive(int ttl) {
		this.timeToLive = ttl;
		return this;
	}

	/** 
	 * Sets the SurfaceView required to preview the video stream. 
	 **/
	public SessionBuilder setSurfaceView(SurfaceView surfaceView) {
		this.surfaceView = surfaceView;
		return this;
	}
	
	/** 
	 * Sets the orientation of the preview.
	 * @param orientation The orientation of the preview
	 */
	public SessionBuilder setPreviewOrientation(int orientation) {
		this.orientation = orientation;
		return this;
	}	
	
	public SessionBuilder setCallback(Session.Callback callback) {
		this.sessionCallback = callback;
		return this;
	}
	
	/** Returns the context set with {@link #setContext(Context)}*/
	public Context getContext() {
		return context;
	}

	/** Returns the destination ip address set with {@link #setDestination(String)}. */
	public String getDestination() {
		return destination;
	}

	/** Returns the origin ip address set with {@link #setOrigin(String)}. */
	public String getOrigin() {
		return origin;
	}

	/** Returns the audio encoder set with {@link #setAudioEncoder(int)}. */
	public int getAudioEncoder() {
		return audioEncoder;
	}

	/** Returns the id of the {@link android.hardware.Camera} set with {@link #setCamera(int)}. */
	public int getCamera() {
		return camera;
	}

	/** Returns the video encoder set with {@link #setVideoEncoder(int)}. */
	public int getVideoEncoder() {
		return videoEncoder;
	}

	/** Returns the VideoQuality set with {@link #setVideoQuality(VideoQuality)}. */
	public VideoQuality getVideoQuality() {
		return videoQuality;
	}
	
	/** Returns the AudioQuality set with {@link #setAudioQuality(AudioQuality)}. */
	public AudioQuality getAudioQuality() {
		return audioQuality;
	}

	/** Returns the flash state set with {@link #setFlashEnabled(boolean)}. */
	public boolean getFlashState() {
		return flash;
	}

	/** Returns the SurfaceView set with {@link #setSurfaceView(SurfaceView)}. */
	public SurfaceView getSurfaceView() {
		return surfaceView;
	}
	
	
	/** Returns the time to live set with {@link #setTimeToLive(int)}. */
	public int getTimeToLive() {
		return timeToLive;
	}

	/** Returns a new {@link SessionBuilder} with the same configuration. */
	public SessionBuilder clone() {
		return new SessionBuilder()
		.setDestination(destination)
		.setOrigin(origin)
		.setSurfaceView(surfaceView)
		.setPreviewOrientation(orientation)
		.setVideoQuality(videoQuality)
		.setVideoEncoder(videoEncoder)
		.setFlashEnabled(flash)
		.setCamera(camera)
		.setTimeToLive(timeToLive)
		.setAudioEncoder(audioEncoder)
		.setAudioQuality(audioQuality)
		.setContext(context)
		.setCallback(sessionCallback);
	}
}