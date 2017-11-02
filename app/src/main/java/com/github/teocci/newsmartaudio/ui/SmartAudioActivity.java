package com.github.teocci.newsmartaudio.ui;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.teocci.newsmartaudio.R;
import com.github.teocci.newsmartaudio.api.CustomRtspServer;
import com.github.teocci.newsmartaudio.bluetooth.BluetoothService;
import com.github.teocci.newsmartaudio.interfaces.ControlListener;
import com.github.teocci.newsmartaudio.net.ControlConnector;
import com.github.teocci.newsmartaudio.utils.LogHelper;
import com.github.teocci.newsmartaudio.utils.NSDHelper;

import net.kseek.streaming.SessionBuilder;
import net.kseek.streaming.rtsp.RtspServer;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

import static com.github.teocci.newsmartaudio.utils.Config.AUDIO_ENCODER;
import static com.github.teocci.newsmartaudio.utils.Config.CLIENT_MODE;
import static com.github.teocci.newsmartaudio.utils.Config.COMMAND_SEPARATOR;
import static com.github.teocci.newsmartaudio.utils.Config.KEY_STATION_NAME;
import static com.github.teocci.newsmartaudio.utils.Config.PARAMETER_SEPARATOR;
import static com.github.teocci.newsmartaudio.utils.Utilities.getLocalIpAddress;
import static net.kseek.streaming.utils.Config.KEY_NOTIFICATION_ENABLED;
import static net.kseek.streaming.utils.Config.KEY_STREAM_AUDIO;

/**
 * SmartAudio basically launches an RTSP server, so clients can then connect to it
 * and start/stop audio streams on the phone.
 */
public class SmartAudioActivity extends AppCompatActivity
{
    static final public String TAG = LogHelper.makeLogTag(SmartAudioActivity.class);

    private TextView currentStationName, deviceIpValue, deviceIpTitle, version, signWifi, textBitrate, controlText;
    private LinearLayout signInformation, signStreaming;
    private ImageView imageViewLeft, imageViewRight;
    private Animation pulseAnimation;

    private PowerManager.WakeLock wakeLock;
    private RtspServer rtspServer;

    private NSDHelper nsdHelper;

    private String stationName;
    private List<String> stationNameList;

    private final Handler handler = new Handler();
    private boolean notificationEnabled;

    private BluetoothService bluetoothService;
    private ControlConnector controlConnector;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//    private MenuItem bluetoothMenu;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_CONNECT_DEVICE = 2;

    private ServiceConnection rtspServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            rtspServer = ((RtspServer.LocalBinder) service).getService();
            rtspServer.addCallbackListener(rtspCallbackListener);
            rtspServer.start();
            update();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private RtspServer.CallbackListener rtspCallbackListener = new RtspServer.CallbackListener()
    {
        @Override
        public void onError(RtspServer server, Exception e, int error)
        {
            // We alert the user that the port is already used by another app.
            if (error == RtspServer.ERROR_BIND_FAILED) {
                new AlertDialog.Builder(SmartAudioActivity.this)
                        .setTitle(R.string.port_used)
                        .setMessage(getString(R.string.bind_failed, "RTSP"))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            public void onClick(final DialogInterface dialog, final int id)
                            {
                                startActivityForResult(new Intent(SmartAudioActivity.this,
                                        PreferencesActivity.class), 0);
                            }
                        })
                        .show();
            }
        }

        @Override
        public void onMessage(RtspServer server, int message)
        {
            if (message == RtspServer.MESSAGE_STREAMING_STARTED) {
                update();
            } else if (message == RtspServer.MESSAGE_STREAMING_STOPPED) {
                update();
            }
        }

    };

    private ControlListener controlListener = new ControlListener()
    {
        @Override
        public void receiveCommand(String str)
        {
            String[] commands = str.split(COMMAND_SEPARATOR);
            switch (commands[0]) {
                case "BT":
                    sendCommandToBT(commands[1]);
                    break;
                case "P":
                    break;
                case "SET":
                    String[] parameter = commands[1].split(PARAMETER_SEPARATOR);
                    switch (parameter[0]) {
                        case "NAME":
                            // change de name
                            break;
                        case "ZOOM":
                            int zoom = Integer.valueOf(parameter[1]);

                            if (rtspServer != null && rtspServer.isStreaming())
                                rtspServer.setZoom(zoom);
                            break;
                    }
                    break;
                case "SETOK":
                    break;
            }
        }

        @Override
        public void connectionSetting(final boolean isConnected)
        {
//            adapter.getHandsetFragment().controlTextUpdate(isConnected);
            if (isConnected && controlConnector != null) {
                if (bluetoothService.isServiceConnected()) {
                    controlConnector.write("STATE;BT:1;\n");
                } else {
                    controlConnector.write("STATE;BT:0;\n");
                }
            }
        }
    };

    // BroadcastReceiver that detects wifi state changes
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            // This intent is also received when app resumes even if wifi state hasn't changed :/
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                update();
            }
        }
    };

    private Runnable updateBitrate = new Runnable()
    {
        @Override
        public void run()
        {
            if (rtspServer != null && rtspServer.isStreaming()) {
                long bitrate = 0;
                bitrate += rtspServer != null ? rtspServer.getBitrate() : 0;
                textBitrate.setText("" + bitrate / 1000 + " kbps");
                handler.postDelayed(updateBitrate, 1000);
            } else {
                textBitrate.setText("0 kbps");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smart_audio);

//        ActionBar actionBar = getActionBar();
//        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#272D39"));
//        actionBar.setBackgroundDrawable(colorDrawable);

        initBluetoothService();
        initControlConnector();
        initNsdHelper();

        initUserInterface();

        // Prevents the phone from going to sleep mode
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.github.teocci.newsmartaudio.wakelock");

        // Starts the service of the RTSP server
        this.startService(new Intent(this, CustomRtspServer.class));
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Lock screen
        wakeLock.acquire();

        // Did the user disabled the notification ?
        if (notificationEnabled) {
            Intent notificationIntent = new Intent(this, SmartAudioActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            Notification notification = builder.setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setTicker(getText(R.string.notification_config_title))
                    .setSmallIcon(R.drawable.ic_smart_audio_noti_icon)
                    .setContentTitle(getText(R.string.notification_config_title))
                    .setContentText(getText(R.string.notification_content)).build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,
                    notification);
        } else {
            removeNotification();
        }

//        bindService(
//                new Intent(this, CustomRtspServer.class),
//                rtspServiceConnection,
//                Context.BIND_AUTO_CREATE
//        );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        bindService(
                new Intent(this, CustomRtspServer.class),
                rtspServiceConnection,
                Context.BIND_AUTO_CREATE
        );
        registerReceiver(
                wifiStateReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        );
    }

    @Override
    public void onStop()
    {
        super.onStop();
        // A WakeLock should only be released when isHeld() is true !
        if (wakeLock.isHeld()) wakeLock.release();
        if (rtspServer != null) rtspServer.removeCallbackListener(rtspCallbackListener);
        unbindService(rtspServiceConnection);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        update();
        unregisterReceiver(wifiStateReceiver);
    }

    @Override
    public void onDestroy()
    {
        LogHelper.d(TAG, "SmartAudioActivity destroyed");
        nsdHelper.tearDown();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        LogHelper.e(TAG, "REQ:" + requestCode + ",RET:" + resultCode + ",DATA:" + intent);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    closeBTService();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    bluetoothService.getDeviceInfo(intent);
                }
                break;
        }
    }

    @Override
    public void onBackPressed()
    {
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_smart_audio, menu);

        menu.findItem(R.id.menu_quit).setShowAsAction(1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menu_quit:
                closeService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendCommandToBT(String str)
    {
        if (!bluetoothService.isServiceConnected()) return;

        try {
            outputStream.write(str.getBytes());

            byte[] buffer;
            buffer = outputStream.toByteArray();
            bluetoothService.write(buffer);

            outputStream.reset();
        } catch (Exception e) {
            LogHelper.e(TAG, e.getMessage());
        }
    }

    private void initUserInterface()
    {
        currentStationName = (TextView) findViewById(R.id.current_station_name);
        deviceIpTitle = (TextView) findViewById(R.id.device_ip_title);
        deviceIpValue = (TextView) findViewById(R.id.device_ip_value);
        version = (TextView) findViewById(R.id.version);
        signWifi = (TextView) findViewById(R.id.advice);
        signStreaming = (LinearLayout) findViewById(R.id.streaming);
        signInformation = (LinearLayout) findViewById(R.id.information);
        pulseAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.pulse);
        textBitrate = (TextView) findViewById(R.id.bitrate);
        controlText = (TextView) findViewById(R.id.control_text);
        imageViewLeft = (ImageView) findViewById(R.id.image_view_left);
        imageViewRight = (ImageView) findViewById(R.id.image_view_right);
    }

    public void initBluetoothService()
    {
        if (bluetoothService == null) {
            bluetoothService = new BluetoothService(this);
        }
        if (!bluetoothService.getDeviceState()) {
            closeBTService();
            return;
        }
        bluetoothService.enableBluetooth();
    }

    private void initControlConnector()
    {
        controlConnector = new ControlConnector(controlListener);
        controlConnector.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initNsdHelper()
    {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(!settings.getBoolean(KEY_STREAM_AUDIO, true) ? 0 : AUDIO_ENCODER);

        notificationEnabled = settings.getBoolean(KEY_NOTIFICATION_ENABLED, true);

        stationName = settings.getString(KEY_STATION_NAME, null);
        if ((stationName == null) || stationName.isEmpty())
            stationName = Build.MODEL;

        final SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_STATION_NAME, stationName);
        editor.apply();

        nsdHelper = new NSDHelper(this);
        nsdHelper.setOperationMode(CLIENT_MODE);
        nsdHelper.setStationName(stationName);
        nsdHelper.registerService();
    }

    public void update()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (deviceIpTitle != null) {
                    if (rtspServer != null) {
                        if (!rtspServer.isEnabled()) {
                            deviceIpTitle.setVisibility(View.INVISIBLE);
                            deviceIpValue.setVisibility(View.INVISIBLE);
                        } else {
                            deviceIpTitle.setVisibility(View.VISIBLE);
                            deviceIpValue.setVisibility(View.VISIBLE);
                        }
                        if (!rtspServer.isStreaming()) {
                            displayIpAddress();
                            displayCurrentStationName();
                        } else {
                            streamingState(1);
                        }
                    }
                }
            }
        });
    }

    private void displayIpAddress()
    {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ipAddress;
        if (info != null && info.getNetworkId() > -1) {
            int i = info.getIpAddress();
            String ip = String.format(
                    Locale.ENGLISH,
                    "%d.%d.%d.%d",
                    i & 0xff,
                    i >> 8 & 0xff,
                    i >> 16 & 0xff,
                    i >> 24 & 0xff
            );
            deviceIpValue.setText("rtsp://");
            deviceIpValue.append(ip);
            deviceIpValue.append(":" + rtspServer.getPort());
            streamingState(0);
        } else if ((ipAddress = getLocalIpAddress(true)) != null) {
            deviceIpValue.setText("rtsp://");
            deviceIpValue.append(ipAddress);
            deviceIpValue.append(":" + rtspServer.getPort());
            streamingState(0);
        } else {
            streamingState(2);
        }
    }

    private void displayCurrentStationName()
    {
        currentStationName.setText(stationName);
    }

    private void streamingState(int state)
    {
        if (state == 0) {
            // Not streaming2
            signStreaming.clearAnimation();
            signWifi.clearAnimation();
            signStreaming.setVisibility(View.GONE);
            signInformation.setVisibility(View.VISIBLE);
            signWifi.setVisibility(View.GONE);
            imageViewLeft.setImageResource(R.drawable.ic_disconnect_sign);
            imageViewRight.setImageResource(R.drawable.ic_disconnect_sign);
        } else if (state == 1) {
            // Streaming
            signWifi.clearAnimation();
            signStreaming.setVisibility(View.VISIBLE);
            signStreaming.startAnimation(pulseAnimation);
            handler.post(updateBitrate);
            signInformation.setVisibility(View.INVISIBLE);
            signWifi.setVisibility(View.GONE);
            imageViewLeft.setImageResource(R.drawable.ic_connect_sign);
            imageViewRight.setImageResource(R.drawable.ic_connect_sign);
        } else if (state == 2) {
            // No wifi !
            signStreaming.clearAnimation();
            signStreaming.setVisibility(View.GONE);
            signInformation.setVisibility(View.INVISIBLE);
            signWifi.setVisibility(View.VISIBLE);
            signWifi.startAnimation(pulseAnimation);
            imageViewLeft.setImageResource(R.drawable.ic_disconnect_sign);
            imageViewRight.setImageResource(R.drawable.ic_disconnect_sign);
        }
    }

    public void successConnection()
    {
        if (controlConnector != null)
            controlConnector.write("STATE;BT:1;");
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                bluetoothMenu.setVisible(false);
//            }
//        });
    }

    public void stopConnection()
    {
        if (controlConnector != null)
            controlConnector.write("STATE;BT:0;");
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                bluetoothMenu.setVisible(true);
//            }
//        });
    }

    private void closeService()
    {
        closeBTService();
        // Removes notification
        if (notificationEnabled) removeNotification();
        // Kills RTSP server
        this.stopService(new Intent(this, CustomRtspServer.class));
        // Returns to home menu
        finish();
    }

    private void closeBTService()
    {
        if (bluetoothService != null) {
            bluetoothService.stop();
            bluetoothService = null;
        }
        if (controlConnector != null) {
            controlConnector.close();
            controlConnector = null;
        }
    }

    private void removeNotification()
    {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    }

    public void log(String s)
    {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
}