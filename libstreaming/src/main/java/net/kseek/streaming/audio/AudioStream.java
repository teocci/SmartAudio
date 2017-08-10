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

import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import net.kseek.streaming.MediaStream;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

/** 
 * Don't use this class directly.
 */
public abstract class AudioStream  extends MediaStream {

	protected int audioSource;
	protected int outputFormat;
	protected int audioEncoder;
	protected AudioQuality requestedQuality = AudioQuality.DEFAULT_AUDIO_QUALITY.clone();
	protected AudioQuality quality = requestedQuality.clone();
	
	public AudioStream() {
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	}
	
	public void setAudioSource(int audioSource) {
		this.audioSource = audioSource;
	}

	public void setAudioQuality(AudioQuality quality) {
		this.requestedQuality = quality;
	}
	
	/** 
	 * Returns the currentQuality of the stream.
	 */
	public AudioQuality getAudioQuality() {
		return quality;
	}	
	
	protected void setAudioEncoder(int audioEncoder) {
		this.audioEncoder = audioEncoder;
	}
	
	protected void setOutputFormat(int outputFormat) {
		this.outputFormat = outputFormat;
	}
	
	@Override
	protected void encodeWithMediaRecorder() throws IOException {
		// We need a local socket to forward data output by the camera to the packetizer
		createSockets();

		Log.v(TAG,"Requested audio with "+
				quality.bitRate/1000+"kbps"+" at "+
				quality.samplingRate/1000+"kHz");
		
		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(audioSource);
		mediaRecorder.setOutputFormat(outputFormat);
		mediaRecorder.setAudioEncoder(audioEncoder);
		mediaRecorder.setAudioChannels(1);
		mediaRecorder.setAudioSamplingRate(quality.samplingRate);
		mediaRecorder.setAudioEncodingBitRate(quality.bitRate);
		
		// We write the output of the camera in a local socket instead of a file !			
		// This one little trick makes streaming feasible quiet simply: data from the camera
		// can then be manipulated at the other end of the socket
		FileDescriptor fd = pipeAPI == PIPE_API_PFD ?
				parcelWrite.getFileDescriptor() : sender.getFileDescriptor();

		mediaRecorder.setOutputFile(fd);
		mediaRecorder.setOutputFile(fd);

		mediaRecorder.prepare();
		mediaRecorder.start();

		InputStream inputStream;
		try {
			// receiver.getInputStream contains the data from the camera
			inputStream = pipeAPI == PIPE_API_PFD ?
					new ParcelFileDescriptor.AutoCloseInputStream(parcelRead) :
					receiver.getInputStream();
		} catch (IOException e) {
			stop();
			throw new IOException("Something happened with the local sockets :/ Start failed !");
		}

		// the packetizer encapsulates this stream in an RTP stream and send it over the network
		packetizer.setInputStream(inputStream);
		packetizer.start();
		streaming = true;
	}
}
