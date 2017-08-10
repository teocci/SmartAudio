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

import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import net.kseek.streaming.audio.AudioQuality;
import net.kseek.streaming.audio.AudioStream;
import net.kseek.streaming.exceptions.CameraInUseException;
import net.kseek.streaming.exceptions.ConfNotSupportedException;
import net.kseek.streaming.exceptions.InvalidSurfaceException;
import net.kseek.streaming.exceptions.StorageUnavailableException;
import net.kseek.streaming.gl.SurfaceView;
import net.kseek.streaming.rtsp.RtspClient;
import net.kseek.streaming.video.VideoQuality;
import net.kseek.streaming.video.VideoStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * You should instantiate this class with the {@link SessionBuilder}.<br />
 * This is the class you will want to use to stream audio and or video to some peer using RTP.<br />
 * 
 * It holds a {@link VideoStream} and a {@link AudioStream} together and provides 
 * synchronous and asynchronous functions to start and stop those steams.
 * You should implement a callback interface {@link Callback} to receive notifications and error reports.<br />
 * 
 * If you want to stream to a RTSP server, you will need an instance of this class and hand it to a {@link RtspClient}.
 * 
 * If you don't use the RTSP protocol, you will still need to send a session description to the receiver 
 * for him to be able to decode your audio/video streams. You can obtain this session description by calling 
 * {@link #configure()} or {@link #syncConfigure()} to configure the session with its parameters 
 * (audio samplingrate, video resolution) and then {@link Session#getSessionDescription()}.<br />
 * 
 * See the example 2 here: https://github.com/fyhertz/libstreaming-examples to 
 * see an example of how to get a SDP.<br />
 * 
 * See the example 3 here: https://github.com/fyhertz/libstreaming-examples to 
 * see an example of how to stream to a RTSP server.<br />
 * 
 */
public class Session {

	public final static String TAG = "Session";

	public final static int STREAM_VIDEO = 0x01;

	public final static int STREAM_AUDIO = 0x00;

	public final static int[] CHANNEL_IDS = {STREAM_AUDIO, STREAM_VIDEO};

	/** Some app is already using a camera (Camera.open() has failed). */
	public final static int ERROR_CAMERA_ALREADY_IN_USE = 0x00;

	/** The phone may not support some streaming parameters that you are trying to use (bit rate, frame rate, resolution...). */
	public final static int ERROR_CONFIGURATION_NOT_SUPPORTED = 0x01;

	/** 
	 * The internal storage of the phone is not ready. 
	 * libstreaming tried to store a test file on the sdcard but couldn't.
	 * See H264Stream and AACStream to find out why libstreaming would want to something like that. 
	 */
	public final static int ERROR_STORAGE_NOT_READY = 0x02;

	/** The phone has no flash. */
	public final static int ERROR_CAMERA_HAS_NO_FLASH = 0x03;

	/** The supplied SurfaceView is not a valid surface, or has not been created yet. */
	public final static int ERROR_INVALID_SURFACE = 0x04;

	/** 
	 * The destination set with {@link Session#setDestination(String)} could not be resolved. 
	 * May mean that the phone has no access to the internet, or that the DNS server could not
	 * resolved the host name.
	 */
	public final static int ERROR_UNKNOWN_HOST = 0x05;

	/**
	 * Some other error occurred !
	 */
	public final static int ERROR_OTHER = 0x06;

	private String origin;
	private String destination;
	private int timeToLive = 64;
	private long timestamp;

	private AudioStream audioStream = null;
	private VideoStream videoStream = null;

	private Callback sessionCallback;
	private Handler mainHandler;

	private Handler handler;

	/** 
	 * Creates a streaming session that can be customized by adding tracks.
	 */
	public Session() {
		long uptime = System.currentTimeMillis();

		HandlerThread thread = new HandlerThread("net.kseek.streaming.Session");
		thread.start();

		this.handler = new Handler(thread.getLooper());
		this.mainHandler = new Handler(Looper.getMainLooper());
		this.timestamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000); // NTP timestamp
		this.origin = "127.0.0.1";
	}

	/**
	 * The callback interface you need to implement to get some feedback
	 * Those will be called from the UI thread.
	 */
	public interface Callback {

		/** 
		 * Called periodically to inform you on the bandwidth 
		 * consumption of the streams when streaming. 
		 */
		void onBitrateUpdate(long bitrate);

		/** Called when some error occurs. */
		void onSessionError(int reason, int streamType, Exception e);

		/** 
		 * Called when the previw of the {@link VideoStream}
		 * has correctly been started.
		 * If an error occurs while starting the preview,
		 * {@link Callback#onSessionError(int, int, Exception)} will be
		 * called instead of {@link Callback#onPreviewStarted()}.
		 */
		void onPreviewStarted();

		/** 
		 * Called when the session has correctly been configured 
		 * after calling {@link Session#configure()}.
		 * If an error occurs while configuring the {@link Session},
		 * {@link Callback#onSessionError(int, int, Exception)} will be
		 * called instead of  {@link Callback#onSessionConfigured()}.
		 */
		void onSessionConfigured();

		/** 
		 * Called when the streams of the session have correctly been started.
		 * If an error occurs while starting the {@link Session},
		 * {@link Callback#onSessionError(int, int, Exception)} will be
		 * called instead of  {@link Callback#onSessionStarted()}. 
		 */
		void onSessionStarted();

		/** Called when the stream of the session have been stopped. */
		void onSessionStopped();

	}

	/** You probably don't need to use that directly, use the {@link SessionBuilder}. */
	void addAudioTrack(AudioStream track) {
		removeAudioTrack();
		this.audioStream = track;
	}

	/** You probably don't need to use that directly, use the {@link SessionBuilder}. */
	void addVideoTrack(VideoStream track) {
		removeVideoTrack();
		this.videoStream = track;
	}

	/** You probably don't need to use that directly, use the {@link SessionBuilder}. */
	void removeAudioTrack() {
		if (audioStream != null) {
			audioStream.stop();
			audioStream = null;
		}
	}

	/** You probably don't need to use that directly, use the {@link SessionBuilder}. */
	void removeVideoTrack() {
		if (videoStream != null) {
			videoStream.stopPreview();
			videoStream = null;
		}
	}

	/** Returns the underlying {@link AudioStream} used by the {@link Session}. */
	public AudioStream getAudioTrack() {
		return audioStream;
	}

	/** Returns the underlying {@link VideoStream} used by the {@link Session}. */
	public VideoStream getVideoTrack() {
		return videoStream;
	}	

	/**
	 * Sets the callback interface that will be called by the {@link Session}.
	 * @param callback The implementation of the {@link Callback} interface
	 */
	public void setCallback(Callback callback) {
		this.sessionCallback = callback;
	}	

	/** 
	 * The origin address of the session.
	 * It appears in the session description.
	 * @param origin The origin address
	 */
	public void setOrigin(String origin) {
		this.origin = origin;
	}	

	/** 
	 * The destination address for all the streams of the session. <br />
	 * Changes will be taken into account the next time you start the session.
	 * @param destination The destination address
	 */
	public void setDestination(String destination) {
		this.destination =  destination;
	}

	/** 
	 * Set the TTL of all packets sent during the session. <br />
	 * Changes will be taken into account the next time you start the session.
	 * @param ttl The Time To Live
	 */
	public void setTimeToLive(int ttl) {
		this.timeToLive = ttl;
	}

	/** 
	 * Sets the configuration of the stream. <br />
	 * You can call this method at any time and changes will take 
	 * effect next time you call {@link #configure()}.
	 * @param quality Quality of the stream
	 */
	public void setVideoQuality(VideoQuality quality) {
		if (videoStream != null) {
			videoStream.setVideoQuality(quality);
		}
	}

	/**
	 * Sets a Surface to show a preview of recorded media (video). <br />
	 * You can call this method at any time and changes will take 
	 * effect next time you call {@link #start()} or {@link #startPreview()}.
	 */
	public void setSurfaceView(final SurfaceView view) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (videoStream != null) {
					videoStream.setSurfaceView(view);
				}
			}				
		});
	}

	/** 
	 * Sets the orientation of the preview. <br />
	 * You can call this method at any time and changes will take 
	 * effect next time you call {@link #configure()}.
	 * @param orientation The orientation of the preview
	 */
	public void setPreviewOrientation(int orientation) {
		if (videoStream != null) {
			videoStream.setPreviewOrientation(orientation);
		}
	}	

	/** 
	 * Sets the configuration of the stream. <br />
	 * You can call this method at any time and changes will take 
	 * effect next time you call {@link #configure()}.
	 * @param quality Quality of the stream
	 */
	public void setAudioQuality(AudioQuality quality) {
		if (audioStream != null) {
			audioStream.setAudioQuality(quality);
		}
	}

	/**
	 * Returns the {@link Callback} interface that was set with 
	 * {@link #setCallback(Callback)} or null if none was set.
	 */
	public Callback getCallback() {
		return sessionCallback;
	}	

	/** 
	 * Returns a Session Description that can be stored in a file or sent to a client with RTSP.
	 * @return The Session Description.
	 * @throws IllegalStateException Thrown when {@link #setDestination(String)} has never been called.
	 */
	public String getSessionDescription() {
		StringBuilder sessionDescription = new StringBuilder();

		if (destination==null) {
			throw new IllegalStateException("setDestination() has not been called !");
		}

		sessionDescription.append("v=0\r\n");
		// TODO: Add IPV6 support
		sessionDescription.append("o=- "+timestamp+" "+timestamp+" IN IP4 "+origin+"\r\n");
		sessionDescription.append("s=Unnamed\r\n");
		sessionDescription.append("i=N/A\r\n");
		sessionDescription.append("c=IN IP4 "+destination+"\r\n");
		// t=0 0 means the session is permanent (we don't know when it will stop)
		sessionDescription.append("t=0 0\r\n");
		sessionDescription.append("a=recvonly\r\n");

		// Prevents two different sessions from using the same peripheral at the same time
		if (audioStream != null) {
			sessionDescription.append(audioStream.getSessionDescription());
			sessionDescription.append("a=control:trackID="+0+"\r\n");
		}
		if (videoStream != null) {
			sessionDescription.append(videoStream.getSessionDescription());
			sessionDescription.append("a=control:trackID="+1+"\r\n");
		}			

		return sessionDescription.toString();
	}

	/** Returns the destination set with {@link #setDestination(String)}. */
	public String getDestination() {
		return destination;
	}

	/** Returns an approximation of the bandwidth consumed by the session in bit per second. */
	public long getBitrate() {
		long sum = 0;
		if (audioStream != null) sum += audioStream.getBitrate();
		if (videoStream != null) sum += videoStream.getBitrate();

		return sum;
	}

	/** Indicates if a track is currently running. */
	public boolean isStreaming() {
		return ((audioStream!=null && audioStream.isStreaming()) ||
				(videoStream!=null && videoStream.isStreaming()));
	}

	/** 
	 * Configures all streams of the session.
	 **/
	public void configure() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					syncConfigure();
				} catch (Exception e) {}
			}
		});
	}	

	/** 
	 * Does the same thing as {@link #configure()}, but in a synchronous manner. <br />
	 * Throws exceptions in addition to calling a callback 
	 * {@link Callback#onSessionError(int, int, Exception)} when
	 * an error occurs.	
	 **/
	public void syncConfigure()  
			throws
			RuntimeException,
			IOException {

		for (int id : CHANNEL_IDS) {
			Stream stream = id==STREAM_AUDIO ? audioStream : videoStream;
			if (stream!=null && !stream.isStreaming()) {
				try {
					stream.configure();
				} catch (CameraInUseException e) {
					postError(ERROR_CAMERA_ALREADY_IN_USE , id, e);
					throw e;
				} catch (StorageUnavailableException e) {
					postError(ERROR_STORAGE_NOT_READY , id, e);
					throw e;
				} catch (ConfNotSupportedException e) {
					postError(ERROR_CONFIGURATION_NOT_SUPPORTED , id, e);
					throw e;
				} catch (InvalidSurfaceException e) {
					postError(ERROR_INVALID_SURFACE , id, e);
					throw e;
				} catch (IOException e) {
					postError(ERROR_OTHER, id, e);
					throw e;
				} catch (RuntimeException e) {
					postError(ERROR_OTHER, id, e);
					throw e;
				}
			}
		}

		postSessionConfigured();
	}

	/** 
	 * Asynchronously starts all streams of the session.
	 **/
	public void start() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					syncStart();
				} catch (Exception e) {}
			}				
		});
	}

	/** 
	 * Starts a stream in a synchronous manner. <br />
	 * Throws exceptions in addition to calling a callback.
	 * @param id The id of the stream to start
	 **/
	public void syncStart(int id) 			
			throws CameraInUseException,
			ConfNotSupportedException,
			InvalidSurfaceException,
			IOException {

		Stream stream = id==STREAM_AUDIO ? audioStream : videoStream;
		if (stream!=null && !stream.isStreaming()) {
			try {
				InetAddress destinationAddress =  InetAddress.getByName(destination);
				stream.setTimeToLive(timeToLive);
				stream.setDestinationAddress(destinationAddress);
				stream.start();
				if (getTrack(1-id) == null || getTrack(1-id).isStreaming()) {
					postSessionStarted();
				}
				if (getTrack(1-id) == null || !getTrack(1-id).isStreaming()) {
					handler.post(mUpdateBitrate);
				}
			} catch (UnknownHostException e) {
				postError(ERROR_UNKNOWN_HOST, id, e);
				throw e;
			} catch (CameraInUseException e) {
				postError(ERROR_CAMERA_ALREADY_IN_USE , id, e);
				throw e;
			} catch (StorageUnavailableException e) {
				postError(ERROR_STORAGE_NOT_READY , id, e);
				throw e;
			} catch (ConfNotSupportedException e) {
				postError(ERROR_CONFIGURATION_NOT_SUPPORTED , id, e);
				throw e;
			} catch (InvalidSurfaceException e) {
				postError(ERROR_INVALID_SURFACE , id, e);
				throw e;
			} catch (IOException e) {
				postError(ERROR_OTHER, id, e);
				throw e;
			} catch (RuntimeException e) {
				postError(ERROR_OTHER, id, e);
				throw e;
			}
		}
	}	

	/** 
	 * Does the same thing as {@link #start()}, but in a synchronous manner. <br /> 
	 * Throws exceptions in addition to calling a callback.
	 **/
	public void syncStart() 			
			throws CameraInUseException,
			ConfNotSupportedException,
			InvalidSurfaceException,
			IOException {

		syncStart(1);
		try {
			syncStart(0);
		} catch (RuntimeException e) {
			syncStop(1);
			throw e;
		} catch (IOException e) {
			syncStop(1);
			throw e;
		}
	}

	/** Stops all existing streams. */
	public void stop() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				syncStop();
			}
		});
	}

	/** 
	 * Stops one stream in a synchronous manner.
	 * @param id The id of the stream to stop
	 **/	
	private void syncStop(final int id) {
		Stream stream = id==STREAM_AUDIO ? audioStream : videoStream;
		if (stream!=null) {
			stream.stop();
		}
	}		

	/** Stops all existing streams in a synchronous manner. */
	public void syncStop() {
		syncStop(0);
		syncStop(1);
		postSessionStopped();
	}

	/**
	 * Asynchronously starts the camera preview. <br />
	 * You should of course pass a {@link SurfaceView} to {@link #setSurfaceView(SurfaceView)}
	 * before calling this method. Otherwise, the {@link Callback#onSessionError(int, int, Exception)}
	 * callback will be called with {@link #ERROR_INVALID_SURFACE}.
	 */
	public void startPreview() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (videoStream != null) {
					try {
						videoStream.startPreview();
						postPreviewStarted();
						videoStream.configure();
					} catch (CameraInUseException e) {
						postError(ERROR_CAMERA_ALREADY_IN_USE , STREAM_VIDEO, e);
					} catch (ConfNotSupportedException e) {
						postError(ERROR_CONFIGURATION_NOT_SUPPORTED , STREAM_VIDEO, e);
					} catch (InvalidSurfaceException e) {
						postError(ERROR_INVALID_SURFACE , STREAM_VIDEO, e);
					} catch (RuntimeException e) {
						postError(ERROR_OTHER, STREAM_VIDEO, e);
					} catch (StorageUnavailableException e) {
						postError(ERROR_STORAGE_NOT_READY, STREAM_VIDEO, e);
					} catch (IOException e) {
						postError(ERROR_OTHER, STREAM_VIDEO, e);
					}
				}
			}
		});
	}

	/**
	 * Asynchronously stops the camera preview.
	 */
	public void stopPreview() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (videoStream != null) {
					videoStream.stopPreview();
				}
			}
		});
	}	

	/**	Switch between the front facing and the back facing camera of the phone. <br />
	 * If {@link #startPreview()} has been called, the preview will be  briefly interrupted. <br />
	 * If {@link #start()} has been called, the stream will be  briefly interrupted.<br />
	 * To find out which camera is currently selected, use {@link #getCamera()}
	 **/
	public void switchCamera() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (videoStream != null) {
					try {
						videoStream.switchCamera();
						postPreviewStarted();
					} catch (CameraInUseException e) {
						postError(ERROR_CAMERA_ALREADY_IN_USE , STREAM_VIDEO, e);
					} catch (ConfNotSupportedException e) {
						postError(ERROR_CONFIGURATION_NOT_SUPPORTED , STREAM_VIDEO, e);
					} catch (InvalidSurfaceException e) {
						postError(ERROR_INVALID_SURFACE , STREAM_VIDEO, e);
					} catch (IOException e) {
						postError(ERROR_OTHER, STREAM_VIDEO, e);
					} catch (RuntimeException e) {
						postError(ERROR_OTHER, STREAM_VIDEO, e);
					}
				}
			}
		});
	}

	/**
	 * Returns the id of the camera currently selected. <br />
	 * It can be either {@link CameraInfo#CAMERA_FACING_BACK} or 
	 * {@link CameraInfo#CAMERA_FACING_FRONT}.
	 */
	public int getCamera() {
		return videoStream != null ? videoStream.getCamera() : 0;

	}

	/** 
	 * Toggles the LED of the phone if it has one.
	 * You can get the current state of the flash with 
	 * {@link Session#getVideoTrack()} and {@link VideoStream#getFlashState()}.
	 **/
	public void toggleFlash() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (videoStream != null) {
					try {
						videoStream.toggleFlash();
					} catch (RuntimeException e) {
						postError(ERROR_CAMERA_HAS_NO_FLASH, STREAM_VIDEO, e);
					}
				}
			}
		});
	}	

	/** Deletes all existing tracks & release associated resources. */
	public void release() {
		removeAudioTrack();
		removeVideoTrack();
		handler.getLooper().quit();
	}

	private void postPreviewStarted() {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (sessionCallback != null) {
					sessionCallback.onPreviewStarted();
				}
			}
		});
	}

	private void postSessionConfigured() {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (sessionCallback != null) {
					sessionCallback.onSessionConfigured();
				}
			}
		});
	}

	private void postSessionStarted() {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (sessionCallback != null) {
					sessionCallback.onSessionStarted();
				}
			}
		});
	}		

	private void postSessionStopped() {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (sessionCallback != null) {
					sessionCallback.onSessionStopped();
				}
			}
		});
	}	

	private void postError(final int reason, final int streamType,final Exception e) {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (sessionCallback != null) {
					sessionCallback.onSessionError(reason, streamType, e);
				}
			}
		});
	}	

	private void postBitRate(final long bitrate) {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (sessionCallback != null) {
					sessionCallback.onBitrateUpdate(bitrate);
				}
			}
		});
	}		

	private Runnable mUpdateBitrate = new Runnable() {
		@Override
		public void run() {
			if (isStreaming()) { 
				postBitRate(getBitrate());
				handler.postDelayed(mUpdateBitrate, 500);
			} else {
				postBitRate(0);
			}
		}
	};


	public boolean trackExists(int id) {
		if (id==0) 
			return audioStream!=null;
		else
			return videoStream!=null;
	}

	public Stream getTrack(int id) {
		if (id==0)
			return audioStream;
		else
			return videoStream;
	}
}
