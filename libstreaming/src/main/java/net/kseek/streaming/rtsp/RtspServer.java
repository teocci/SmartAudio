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

package net.kseek.streaming.rtsp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import net.kseek.streaming.Session;
import net.kseek.streaming.SessionBuilder;
import net.kseek.streaming.ntp.NTPClient;
import net.kseek.streaming.utils.Config;
import net.kseek.streaming.video.VideoQuality;
import net.kseek.streaming.video.VideoStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326).
 * 
 * It allows remote control of an android device cameras & microphone.
 * For each connected client, a Session is instantiated.
 * The Session will start or stop streams according to what the client wants.
 * 
 */
public class RtspServer extends Service {

	public final static String TAG = RtspServer.class.getSimpleName();

	/** The server name that will appear in responses. */
	public static String SERVER_NAME = "Camtest RTSP Server";

	/** Port used by default. */
	public static final int DEFAULT_RTSP_PORT = 8086;

	/** Port already in use. */
	public final static int ERROR_BIND_FAILED = 0x00;

	/** A stream could not be started. */
	public final static int ERROR_START_FAILED = 0x01;

	/** Streaming started. */
	public final static int MESSAGE_STREAMING_STARTED = 0X00;
	
	/** Streaming stopped. */
	public final static int MESSAGE_STREAMING_STOPPED = 0X01;
	
	/** Key used in the SharedPreferences to store whether the RTSP server is enabled or not. */
//	public final static String KEY_ENABLED = "rtsp_enabled";

	/** Key used in the SharedPreferences for the port used by the RTSP server. */
//	public final static String KEY_PORT = "rtsp_port";

	protected SessionBuilder sessionBuilder;
	protected SharedPreferences sharedPreferences;
	protected boolean enabled = true;
	protected int rtspPort = DEFAULT_RTSP_PORT;
	protected WeakHashMap<Session,Object> sessions = new WeakHashMap<>(2);
	
	private RequestListener listenerThread;
	private final IBinder binder = new LocalBinder();
	private boolean restart = false;
	private final LinkedList<CallbackListener> listeners = new LinkedList<>();

    /** Credentials for Basic Auth */
    private String username;
    private String password;

	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Config.KEY_VIDEO_WIDTH) || key.equals(Config.KEY_VIDEO_HEIGHT)) {
                updateVideoQuality(key);
            }  else if (key.equals(Config.KEY_VIDEO_FRAMERATE)) {
                updateVideoQuality(key);
            } else if (key.equals(Config.KEY_VIDEO_BITRATE)) {
                updateVideoQuality(key);
            } else if (key.equals(Config.KEY_RTSP_PORT)) {
				int port = Integer.parseInt(sharedPreferences.getString(Config.KEY_RTSP_PORT, String.valueOf(rtspPort)));
				if (port != rtspPort) {
					rtspPort = port;
					restart = true;
					start();
				}
			}
			else if (key.equals(Config.KEY_RTSP_ENABLED)) {
				enabled = sharedPreferences.getBoolean(Config.KEY_RTSP_ENABLED, enabled);
				start();
			}
		}
	};
	

	public RtspServer() {
	}

	/** Be careful: those callbacks won't necessarily be called from the ui thread ! */
	public interface CallbackListener {

		/** Called when an error occurs. */
		void onError(RtspServer server, Exception e, int error);

		/** Called when streaming starts/stops. */
		void onMessage(RtspServer server, int message);
		
	}

	/**
	 * See {@link RtspServer.CallbackListener} to check out what events will be fired once you set up a listener.
	 * @param listener The listener
	 */
	public void addCallbackListener(CallbackListener listener) {
		synchronized (listeners) {
			if (!listeners.isEmpty()) {
				for (CallbackListener cl : listeners) {
					if (cl == listener) return;
				}
			}
			listeners.add(listener);
		}
	}

	/**
	 * Removes the listener.
	 * @param listener The listener
	 */
	public void removeCallbackListener(CallbackListener listener) {
		synchronized (listeners) {
			this.listeners.remove(listener);
		}
	}

	/** Returns the port used by the RTSP server. */
	public int getPort() {
		return rtspPort;
	}

	/**
	 * Sets the port for the RTSP server to use.
	 * @param port The port
	 */
	public void setPort(int port) {
		Editor editor = sharedPreferences.edit();
		editor.putString(Config.KEY_RTSP_PORT, String.valueOf(port));
		editor.apply();
	}

    /**
     * Set Basic authorization to access RTSP Stream
     * @param username username
     * @param password password
     */
    public void setAuthorization(String username, String password)
    {
        this.username = username;
		this.password = password;
    }

	/** 
	 * Starts (or restart if needed, if for example the configuration 
	 * of the server has been modified) the RTSP server. 
	 */
	public void start() {
		if (!enabled || restart) stop();
		if (enabled && listenerThread == null) {
			try {
				listenerThread = new RequestListener();
			} catch (Exception e) {
				listenerThread = null;
			}
		}
		restart = false;
	}

	/** 
	 * Stops the RTSP server but not the Android Service. 
	 * To stop the Android Service you need to call {@link android.content.Context#stopService(Intent)}; 
	 */
	public void stop() {
		if (listenerThread != null) {
			try {
				listenerThread.kill();
				for ( Session session : sessions.keySet() ) {
				    if (session != null && session.isStreaming()) {
						session.stop();
				    } 
				}
			} catch (Exception e) {
			} finally {
				listenerThread = null;
				postMessage(MESSAGE_STREAMING_STOPPED);
			}
		}
	}

	/** Returns whether or not the RTSP server is streaming to some client(s). */
	public boolean isStreaming() {
		for ( Session session : sessions.keySet() ) {
		    if (session != null && session.isStreaming()) {
				return true;
		    } 
		}
		return false;
	}

	public void handleZoom(int newZoom) {
		for ( Session session : sessions.keySet() ) {
			if (session != null && session.isStreaming()) {
				session.getVideoTrack().handleZoom(newZoom);
			}
		}
	}

	public void setZoom(int newZoom) {
		for ( Session session : sessions.keySet() ) {
			if (session != null && session.isStreaming()) {
				session.getVideoTrack().setZoom(newZoom);
			}
		}
	}

	public void setNTPHostPort(String host, int port) {
		NTPClient.getInstance(host, port);
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** Returns the bandwidth consumed by the RTSP server in bits per second. */
	public long getBitrate() {
		long bitrate = 0;
		for ( Session session : sessions.keySet() ) {
		    if (session != null && session.isStreaming()) {
		    	bitrate += session.getBitrate();
		    } 
		}
		return bitrate;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onCreate() {

		// Let's restore the state of the service 
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		rtspPort = Integer.parseInt(sharedPreferences.getString(Config.KEY_RTSP_PORT, String.valueOf(rtspPort)));
		enabled = sharedPreferences.getBoolean(Config.KEY_RTSP_ENABLED, enabled);

		// If the configuration is modified, the server will adjust
		sharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

		start();
	}

	@Override
	public void onDestroy() {
		stop();
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}

	/** The Binder you obtain when a connection with the Service is established. */
	public class LocalBinder extends Binder {
		public RtspServer getService() {
			return RtspServer.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	protected void postMessage(int id) {
		synchronized (listeners) {
			if (!listeners.isEmpty()) {
				for (CallbackListener cl : listeners) {
					cl.onMessage(this, id);
				}
			}			
		}
	}

	protected void updateVideoQuality(String key) {
        Log.e(TAG, "onSharedPreferenceChanged | key: " + key);
        for ( Session session : sessions.keySet() ) {
            if (session != null && !session.isStreaming()) {
                int videoWidth = sharedPreferences.getInt(Config.KEY_VIDEO_WIDTH, 0);
                int videoHeight = sharedPreferences.getInt(Config.KEY_VIDEO_HEIGHT, 0);
                VideoQuality  videoQuality = new VideoQuality(videoWidth, videoHeight);
                videoQuality.framerate = Integer.parseInt(
                        sharedPreferences.getString(Config.KEY_VIDEO_FRAMERATE, "0")
                );
                videoQuality.bitrate = Integer.parseInt(
                        sharedPreferences.getString(Config.KEY_VIDEO_BITRATE, "0")
                ) * 1000;
                session.setVideoQuality(videoQuality);
            }
        }
    }
	
	protected void postError(Exception exception, int id) {
		synchronized (listeners) {
			if (!listeners.isEmpty()) {
				for (CallbackListener cl : listeners) {
					cl.onError(this, exception, id);
				}
			}			
		}
	}

	/** 
	 * By default the RTSP uses {@link UriParser} to parse the URI requested by the client
	 * but you can change that behavior by override this method.
	 * @param uri The uri that the client has requested
	 * @param client The socket associated to the client
	 * @return A proper session
	 */
	protected Session handleRequest(String uri, Socket client) throws IllegalStateException, IOException {
		Session session = UriParser.parse(uri);
		session.setOrigin(client.getLocalAddress().getHostAddress());
		if (session.getDestination()==null) {
			session.setDestination(client.getInetAddress().getHostAddress());
		}
		return session;
	}
	
	class RequestListener extends Thread implements Runnable {

		private final ServerSocket server;

		public RequestListener() throws IOException {
			try {
				server = new ServerSocket(rtspPort);
				start();
			} catch (BindException e) {
				Log.e(TAG,"Port already in use !");
				postError(e, ERROR_BIND_FAILED);
				throw e;
			}
		}

		public void run() {
			Log.i(TAG,"RTSP server listening on port "+server.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					new WorkerThread(server.accept()).start();
				} catch (SocketException e) {
					break;
				} catch (IOException e) {
					Log.e(TAG,e.getMessage());
					continue;
				}
			}
			Log.i(TAG,"RTSP server stopped !");
		}

		public void kill() {
			try {
				server.close();
			} catch (IOException e) {}
			try {
				this.join();
			} catch (InterruptedException ignore) {}
		}

	}

	// One thread per client
	class WorkerThread extends Thread implements Runnable {

		private final Socket mClient;
		private final OutputStream mOutput;
		private final BufferedReader mInput;

		// Each client has an associated session
		private Session session;

		public WorkerThread(final Socket client) throws IOException {
			mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
			mOutput = client.getOutputStream();
			mClient = client;
			session = new Session();
		}

		public void run() {
			Request request;
			Response response;

			Log.i(TAG, "Connection from "+mClient.getInetAddress().getHostAddress());

			while (!Thread.interrupted()) {

				request = null;
				response = null;

				// Parse the request
				try {
					request = Request.parseRequest(mInput);
				} catch (SocketException e) {
					// Client has left
					break;
				} catch (Exception e) {
					// We don't understand the request :/
					response = new Response();
					response.status = Response.STATUS_BAD_REQUEST;
				}

				// Do something accordingly like starting the streams, sending a session description
				if (request != null) {
					try {
						response = processRequest(request);
					}
					catch (Exception e) {
						// This alerts the main thread that something has gone wrong in this thread
						postError(e, ERROR_START_FAILED);
						Log.e(TAG,e.getMessage()!=null?e.getMessage():"An error occurred");
						e.printStackTrace();
						response = new Response(request);
					}
				}

				// We always send a response
				// The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
				try {
					response.send(mOutput);
				} catch (IOException e) {
					Log.e(TAG,"Response was not sent properly");
					break;
				}

			}

			// Streaming stops when client disconnects
			boolean streaming = isStreaming();
			session.syncStop();
			if (streaming && !isStreaming()) {
				postMessage(MESSAGE_STREAMING_STOPPED);
			}
			session.release();

			try {
				mClient.close();
			} catch (IOException ignore) {}

			Log.i(TAG, "Client disconnected");

		}

		public Response processRequest(Request request) throws IllegalStateException, IOException {
			Response response = new Response(request);

            //Ask for authorization unless this is an OPTIONS request
            if(!isAuthorized(request) && !request.method.equalsIgnoreCase("OPTIONS"))
            {
                response.attributes = "WWW-Authenticate: Basic realm=\""+SERVER_NAME+"\"\r\n";
                response.status = Response.STATUS_UNAUTHORIZED;
            }
            else
            {
			    /* ********************************************************************************** */
			    /* ********************************* Method DESCRIBE ******************************** */
			    /* ********************************************************************************** */
                if (request.method.equalsIgnoreCase("DESCRIBE")) {

                    // Parse the requested URI and configure the session
                    session = handleRequest(request.uri, mClient);
                    sessions.put(session, null);
                    session.syncConfigure();

                    String requestContent = session.getSessionDescription();
                    String requestAttributes =
                            "Content-Base: " + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/\r\n" +
                                    "Content-Type: application/sdp\r\n";

                    response.attributes = requestAttributes;
                    response.content = requestContent;

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                }

                /* ********************************************************************************** */
                /* ********************************* Method OPTIONS ********************************* */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("OPTIONS")) {
                    response.status = Response.STATUS_OK;
                    response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************** Method SETUP ********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("SETUP")) {
                    Pattern p;
                    Matcher m;
                    int p2, p1, ssrc, trackId, src[];
                    String destination;

                    p = Pattern.compile("trackID=(\\w+)", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(request.uri);

                    if (!m.find()) {
                        response.status = Response.STATUS_BAD_REQUEST;
                        return response;
                    }

                    trackId = Integer.parseInt(m.group(1));

                    if (!session.trackExists(trackId)) {
                        response.status = Response.STATUS_NOT_FOUND;
                        return response;
                    }

                    p = Pattern.compile("client_port=(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(request.headers.get("transport"));

                    if (!m.find()) {
                        int[] ports = session.getTrack(trackId).getDestinationPorts();
                        p1 = ports[0];
                        p2 = ports[1];
                    } else {
                        p1 = Integer.parseInt(m.group(1));
                        p2 = Integer.parseInt(m.group(2));
                    }

                    ssrc = session.getTrack(trackId).getSSRC();
                    src = session.getTrack(trackId).getLocalPorts();
                    destination = session.getDestination();

                    session.getTrack(trackId).setDestinationPorts(p1, p2);

                    boolean streaming = isStreaming();
                    session.syncStart(trackId);
                    if (!streaming && isStreaming()) {
                        postMessage(MESSAGE_STREAMING_STARTED);
                    }

                    response.attributes = "Transport: RTP/AVP/UDP;" + (InetAddress.getByName(destination).isMulticastAddress() ? "multicast" : "unicast") +
                            ";destination=" + session.getDestination() +
                            ";client_port=" + p1 + "-" + p2 +
                            ";server_port=" + src[0] + "-" + src[1] +
                            ";ssrc=" + Integer.toHexString(ssrc) +
                            ";mode=play\r\n" +
                            "Session: " + "1185d20035702ca" + ";timeout=10000" + "\r\n" +
                            "Cache-Control: no-cache\r\n";
                    response.status = Response.STATUS_OK;

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

					Log.d(TAG,response.attributes.replace("\r", ""));
                }

                /* ********************************************************************************** */
                /* ********************************** Method PLAY *********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("PLAY")) {
                    String requestAttributes = "RTP-Info: ";
                    if (session.trackExists(0))
                        requestAttributes += "url=rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/trackID=" + 0 + ";seq=0,";
                    if (session.trackExists(1))
                        requestAttributes += "url=rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/trackID=" + 1 + ";seq=0,";
                    requestAttributes = requestAttributes.substring(0, requestAttributes.length() - 1) + "\r\nSession: 1185d20035702ca\r\n";

                    response.attributes = requestAttributes;

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                }

                /* ********************************************************************************** */
                /* ********************************** Method PAUSE ********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("PAUSE")) {
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************* Method TEARDOWN ******************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("TEARDOWN")) {
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************* Unknown method ? ******************************* */
                /* ********************************************************************************** */
                else {
                    Log.e(TAG, "Command unknown: " + request);
                    response.status = Response.STATUS_BAD_REQUEST;
                }
            }
			return response;

		}

        /**
         * Check if the request is authorized
         * @param request
         * @return true or false
         */
        private boolean isAuthorized(Request request)
        {
            String auth = request.headers.get("authorization");
            if(username == null || password == null || username.isEmpty())
                return true;

            if(auth != null && !auth.isEmpty())
            {
                String received = auth.substring(auth.lastIndexOf(" ")+1);
                String local = username+":"+password;
                String localEncoded = Base64.encodeToString(local.getBytes(),Base64.NO_WRAP);
                if(localEncoded.equals(received))
                    return true;
            }

            return false;
        }
	}

	static class Request {

		// Parse method & uri
		public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP",Pattern.CASE_INSENSITIVE);
		// Parse a request header
		public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);

		public String method;
		public String uri;
		public HashMap<String,String> headers = new HashMap<String,String>();

		/** Parse the method, uri & headers of a RTSP request */
		public static Request parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException {
			Request request = new Request();
			String line;
			Matcher matcher;

			// Parsing request method & uri
			if ((line = input.readLine())==null) throw new SocketException("Client disconnected");
			matcher = regexMethod.matcher(line);
			matcher.find();
			request.method = matcher.group(1);
			request.uri = matcher.group(2);
			Log.e(TAG, "Parsing request method & uri: " + line + " | " + request.method + " --> " + request.uri);

			// Parsing headers of the request
			while ( (line = input.readLine()) != null && line.length()>3 ) {
				matcher = rexegHeader.matcher(line);
				matcher.find();
				request.headers.put(matcher.group(1).toLowerCase(Locale.US),matcher.group(2));
				Log.e(TAG, "Parsing headers of the request: " + line + " | " + matcher.group(1).toLowerCase(Locale.US) + " --> " + matcher.group(2));
			}
			if (line==null) throw new SocketException("Client disconnected");

			// It's not an error, it's just easier to follow what's happening in logcat with the request in red
			Log.e(TAG, request.method + " " + request.uri);

			return request;
		}
	}

	static class Response {

		// Status code definitions
		public static final String STATUS_OK = "200 OK";
		public static final String STATUS_BAD_REQUEST = "400 Bad Request";
        public static final String STATUS_UNAUTHORIZED = "401 Unauthorized";
		public static final String STATUS_NOT_FOUND = "404 Not Found";
		public static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

		public String status = STATUS_INTERNAL_SERVER_ERROR;
		public String content = "";
		public String attributes = "";

		private final Request request;

		public Response(Request request) {
			this.request = request;
		}

		public Response() {
			// Be carefull if you modify the send() method because request might be null !
			request = null;
		}

		public void send(OutputStream output) throws IOException {
			int seqid = -1;

			try {
				seqid = Integer.parseInt(request.headers.get("cseq").replace(" ",""));
			} catch (Exception e) {
				Log.e(TAG,"Error parsing CSeq: "+(e.getMessage()!=null?e.getMessage():""));
			}

			String response = 	"RTSP/1.0 "+status+"\r\n" +
					"Server: "+SERVER_NAME+"\r\n" +
					(seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
					"Content-Length: " + content.length() + "\r\n" +
					attributes +
					"\r\n" + 
					content;

			Log.d(TAG,response.replace("\r", ""));

			output.write(response.getBytes());
		}
	}
}
