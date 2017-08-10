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

package net.kseek.streaming.audio;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.service.textservice.SpellCheckerService.Session;
import android.util.Log;

import net.kseek.streaming.SessionBuilder;
import net.kseek.streaming.rtp.AACADTSPacketizer;
import net.kseek.streaming.rtp.AACLATMPacketizer;
import net.kseek.streaming.rtp.MediaCodecInputStream;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * A class for streaming AAC from the camera of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setAudioQuality(AudioQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class AACStream extends AudioStream {

	public final static String TAG = "AACStream";

	/** MPEG-4 Audio Object Types supported by ADTS. **/
	private static final String[] AUDIO_OBJECT_TYPES = {
		"NULL",							  // 0
		"AAC Main",						  // 1
		"AAC LC (Low Complexity)",		  // 2
		"AAC SSR (Scalable Sample Rate)", // 3
		"AAC LTP (Long Term Prediction)"  // 4	
	};

	/** There are 13 supported frequencies by ADTS. **/
	public static final int[] AUDIO_SAMPLING_RATES = {
		96000, // 0
		88200, // 1
		64000, // 2
		48000, // 3
		44100, // 4
		32000, // 5
		24000, // 6
		22050, // 7
		16000, // 8
		12000, // 9
		11025, // 10
		8000,  // 11
		7350,  // 12
		-1,   // 13
		-1,   // 14
		-1,   // 15
	};

	private String sessionDescription = null;
	private int profile, samplingRateIndex, channel, config;
	private SharedPreferences settings = null;
	private AudioRecord audioRecord = null;
	private Thread mThread = null;

	public AACStream() {
		super();

		if (!AACStreamingSupported()) {
			Log.e(TAG,"AAC not supported on this phone");
			throw new RuntimeException("AAC not supported by this phone !");
		} else {
			Log.d(TAG,"AAC supported on this phone");
		}

	}

	private static boolean AACStreamingSupported() {
		if (Build.VERSION.SDK_INT<14) return false;
		try {
			MediaRecorder.OutputFormat.class.getField("AAC_ADTS");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Some data (the actual sampling rate used by the phone and the AAC profile) needs to be stored once {@link #getSessionDescription()} is called.
	 * @param prefs The SharedPreferences that will be used to store the sampling rate 
	 */
	public void setPreferences(SharedPreferences prefs) {
		settings = prefs;
	}

	@Override
	public synchronized void start() throws IllegalStateException, IOException {
		if (!streaming) {
			configure();
			super.start();
		}
	}

	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		quality = requestedQuality.clone();

		// Checks if the user has supplied an exotic sampling rate
		int i=0;
		for (;i<AUDIO_SAMPLING_RATES.length;i++) {
			if (AUDIO_SAMPLING_RATES[i] == quality.samplingRate) {
				samplingRateIndex = i;
				break;
			}
		}
		// If he did, we force a reasonable one: 16 kHz
		if (i>12) quality.samplingRate = 16000;

		if (currentMode != requestedMode || packetizer==null) {
			currentMode = requestedMode;
			if (currentMode == MODE_MEDIARECORDER_API) {
				packetizer = new AACADTSPacketizer();
			} else { 
				packetizer = new AACLATMPacketizer();
			}
			packetizer.setDestination(destination, rtpPort, rtcpPort);
			packetizer.getRtpSocket().setOutputStream(outputStream, channelIdentifier);
		}

		if (currentMode == MODE_MEDIARECORDER_API) {

			testADTS();

			// All the MIME types parameters used here are described in RFC 3640
			// SizeLength: 13 bits will be enough because ADTS uses 13 bits for frame length
			// config: contains the object type + the sampling rate + the channel number

			// TODO: streamType always 5 ? profile-level-id always 15 ?

			sessionDescription = "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
					"a=rtpmap:96 mpeg4-generic/"+quality.samplingRate+"\r\n"+
					"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; "+
					"config="+Integer.toHexString(config)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";

		} else {

			profile = 2; // AAC LC
			channel = 1;
			config = (profile & 0x1F) << 11 | (samplingRateIndex & 0x0F) << 7 | (channel & 0x0F) << 3;

			sessionDescription = "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
					"a=rtpmap:96 mpeg4-generic/"+quality.samplingRate+"\r\n"+
					"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; "+
					"config="+Integer.toHexString(config)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";
		}
	}

	@Override
	protected void encodeWithMediaRecorder() throws IOException {
		testADTS();
		((AACADTSPacketizer)packetizer).setSamplingRate(quality.samplingRate);
		super.encodeWithMediaRecorder();
	}

	@Override
	@SuppressLint({ "InlinedApi", "NewApi" })
	protected void encodeWithMediaCodec() throws IOException {
		final int bufferSize = AudioRecord.getMinBufferSize(quality.samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*2;

		((AACLATMPacketizer)packetizer).setSamplingRate(quality.samplingRate);

		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, quality.samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
		MediaFormat format = new MediaFormat();
		format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
		format.setInteger(MediaFormat.KEY_BIT_RATE, quality.bitRate);
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, quality.samplingRate);
		format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
		mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		audioRecord.startRecording();
		mediaCodec.start();

		final MediaCodecInputStream inputStream = new MediaCodecInputStream(mediaCodec);
		final ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();

		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				int len = 0, bufferIndex = 0;
				try {
					while (!Thread.interrupted()) {
						bufferIndex = mediaCodec.dequeueInputBuffer(10000);
						if (bufferIndex>=0) {
							inputBuffers[bufferIndex].clear();
							len = audioRecord.read(inputBuffers[bufferIndex], bufferSize);
							if (len ==  AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
								Log.e(TAG,"An error occured with the AudioRecord API !");
							} else {
								//Log.v(TAG,"Pushing raw audio to the decoder: len="+len+" bs: "+inputBuffers[bufferIndex].capacity());
								mediaCodec.queueInputBuffer(bufferIndex, 0, len, System.nanoTime()/1000, 0);
							}
						}
					}
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		});

		mThread.start();

		// The packetizer encapsulates this stream in an RTP stream and send it over the network
		packetizer.setInputStream(inputStream);
		packetizer.start();

		streaming = true;
	}

	/** Stops the stream. */
	public synchronized void stop() {
		if (streaming) {
			if (currentMode==MODE_MEDIACODEC_API) {
				Log.d(TAG, "Interrupting threads...");
				mThread.interrupt();
				audioRecord.stop();
				audioRecord.release();
				audioRecord = null;
			}
			super.stop();
		}
	}

	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 * Will fail if called when streaming.
	 */
	public String getSessionDescription() throws IllegalStateException {
		if (sessionDescription == null) throw new IllegalStateException("You need to call configure() first !");
		return sessionDescription;
	}

	/** 
	 * Records a short sample of AAC ADTS from the microphone to find out what the sampling rate really is
	 * On some phone indeed, no error will be reported if the sampling rate used differs from the 
	 * one selected with setAudioSamplingRate 
	 * @throws IOException 
	 * @throws IllegalStateException
	 */
	@SuppressLint("InlinedApi")
	private void testADTS() throws IllegalStateException, IOException {
		
		setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		try {
			Field name = MediaRecorder.OutputFormat.class.getField("AAC_ADTS");
			setOutputFormat(name.getInt(null));
		}
		catch (Exception ignore) {
			setOutputFormat(6);
		}

		String key = PREF_PREFIX+"aac-"+quality.samplingRate;

		if (settings!=null) {
			if (settings.contains(key)) {
				String[] s = settings.getString(key, "").split(",");
				quality.samplingRate = Integer.valueOf(s[0]);
				config = Integer.valueOf(s[1]);
				channel = Integer.valueOf(s[2]);
				return;
			}
		}

		final String TESTFILE = Environment.getExternalStorageDirectory().getPath()+"/spydroid-test.adts";

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new IllegalStateException("No external storage or external storage not ready !");
		}

		// The structure of an ADTS packet is described here: http://wiki.multimedia.cx/index.php?title=ADTS

		// ADTS header is 7 or 9 bytes long
		byte[] buffer = new byte[9];

		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(audioSource);
		mediaRecorder.setOutputFormat(outputFormat);
		mediaRecorder.setAudioEncoder(audioEncoder);
		mediaRecorder.setAudioChannels(1);
		mediaRecorder.setAudioSamplingRate(quality.samplingRate);
		mediaRecorder.setAudioEncodingBitRate(quality.bitRate);
		mediaRecorder.setOutputFile(TESTFILE);
		mediaRecorder.setMaxDuration(1000);
		mediaRecorder.prepare();
		mediaRecorder.start();

		// We record for 1 sec
		// TODO: use the MediaRecorder.OnInfoListener
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}

		mediaRecorder.stop();
		mediaRecorder.release();
		mediaRecorder = null;

		File file = new File(TESTFILE);
		RandomAccessFile raf = new RandomAccessFile(file, "r");

		// ADTS packets start with a sync word: 12bits set to 1
		while (true) {
			if ( (raf.readByte()&0xFF) == 0xFF ) {
				buffer[0] = raf.readByte();
				if ( (buffer[0]&0xF0) == 0xF0) break;
			}
		}

		raf.read(buffer,1,5);

		samplingRateIndex = (buffer[1]&0x3C)>>2 ;
		profile = ( (buffer[1]&0xC0) >> 6 ) + 1 ;
		channel = (buffer[1]&0x01) << 2 | (buffer[2]&0xC0) >> 6 ;
		quality.samplingRate = AUDIO_SAMPLING_RATES[samplingRateIndex];

		// 5 bits for the object type / 4 bits for the sampling rate / 4 bits for the channel / padding
		config = (profile & 0x1F) << 11 | (samplingRateIndex & 0x0F) << 7 | (channel & 0x0F) << 3;

		Log.i(TAG,"MPEG VERSION: " + ( (buffer[0]&0x08) >> 3 ) );
		Log.i(TAG,"PROTECTION: " + (buffer[0]&0x01) );
		Log.i(TAG,"PROFILE: " + AUDIO_OBJECT_TYPES[ profile ] );
		Log.i(TAG,"SAMPLING FREQUENCY: " + quality.samplingRate );
		Log.i(TAG,"CHANNEL: " + channel );

		raf.close();

		if (settings!=null) {
			Editor editor = settings.edit();
			editor.putString(key, quality.samplingRate+","+config+","+channel);
			editor.commit();
		}

		if (!file.delete()) Log.e(TAG,"Temp file could not be erased");
	}
}
