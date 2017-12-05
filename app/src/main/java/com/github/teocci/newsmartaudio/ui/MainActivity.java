package com.github.teocci.newsmartaudio.ui;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.teocci.featureguide.lib.BaseTooltip;
import com.github.teocci.featureguide.lib.FeatureGuide;
import com.github.teocci.featureguide.lib.Overlay;
import com.github.teocci.featureguide.lib.TooltipActionView;
import com.github.teocci.newsmartaudio.R;
import com.github.teocci.newsmartaudio.api.CustomRtspServer;
import com.github.teocci.newsmartaudio.views.CircleButtonView;

import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.github.teocci.newsmartaudio.utils.Config.KEY_FEATURE_GUIDE;
import static com.github.teocci.newsmartaudio.utils.Config.REQUEST_ALL;

public class MainActivity extends AppCompatActivity
{
    static final public String TAG = MainActivity.class.getSimpleName();

    public FeatureGuide featureGuideHandler;
    public FeatureGuide fgHandler;

    private CircleButtonView circleButton;

    private boolean featureGuide;
    private boolean gotPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermissions();
        initSettings();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.menu_quit).setShowAsAction(1);
        menu.findItem(R.id.menu_options).setShowAsAction(1);

        MenuItem menuItem = menu.findItem(R.id.menu_options);
        initFeatureMenuGuide(menuItem);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Map<String, Integer> perm = new HashMap<>();
        perm.put(CAMERA, PERMISSION_DENIED);
        perm.put(RECORD_AUDIO, PERMISSION_DENIED);
        perm.put(WRITE_EXTERNAL_STORAGE, PERMISSION_DENIED);
        perm.put(READ_PHONE_STATE, PERMISSION_DENIED);

        for (int i = 0; i < permissions.length; i++) {
            perm.put(permissions[i], grantResults[i]);
        }

        if (perm.get(CAMERA) == PERMISSION_GRANTED &&
                perm.get(RECORD_AUDIO) == PERMISSION_GRANTED &&
                perm.get(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED &&
                perm.get(READ_PHONE_STATE) == PERMISSION_GRANTED) {
            gotPermissions = true;
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, READ_PHONE_STATE)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.permission_warning)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initPermissions()
    {
        int cameraPermission = ContextCompat.checkSelfPermission(this, CAMERA);
        int microphonePermission = ContextCompat.checkSelfPermission(this, RECORD_AUDIO);
        int storagePermission = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        int phonePermission = ContextCompat.checkSelfPermission(this, READ_PHONE_STATE);
        gotPermissions = cameraPermission == PERMISSION_GRANTED &&
                microphonePermission == PERMISSION_GRANTED &&
                storagePermission == PERMISSION_GRANTED &&
                phonePermission == PERMISSION_GRANTED;
        if (!gotPermissions)
            requirePermissions();
    }

    private void initSettings()
    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

//        ActionBar actionBar = getActionBar();
//        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#272D39"));
//        actionBar.setBackgroundDrawable(colorDrawable);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        featureGuide = settings.getBoolean(KEY_FEATURE_GUIDE, true);

        circleButton = (CircleButtonView) findViewById(R.id.circleButton);
        circleButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cleanFeatureGuide();
                Intent intent = new Intent(getApplicationContext(), SmartAudioActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initFeatureMenuGuide(final MenuItem item)
    {
        View anchorView = item.getActionView();
        if (anchorView != null) {
            if (anchorView instanceof TooltipActionView) {
                TooltipActionView tooltipActionView = (TooltipActionView) anchorView;
                tooltipActionView.setMenuItem(item);
                tooltipActionView.setOnMenuItemClick(new MenuItem.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        Intent intent = new Intent(getApplicationContext(), PreferencesActivity.class);
                        startActivityForResult(intent, 0);
                        return false;
                    }
                });
            }

            if (featureGuide) {
                String note = getResources().getString(R.string.setting_guide_description);
                SpannableString text = new SpannableString(note);
                Drawable d = ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_settings_black);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                text.setSpan(span, note.indexOf('X'), note.indexOf('X') + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                BaseTooltip toolTip = new BaseTooltip()
                        .setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View view)
                            {
                                fgHandler.cleanUp();
                                initFeatureCircleGuide(circleButton);
                            }
                        })
                        .setTitle(getResources().getString(R.string.setting_guide_title))
                        .setTextColor(Color.parseColor("#131720"))
                        .setBackgroundColor(Color.parseColor("#FFFFFF"))
                        .setDescription(text);

                fgHandler = FeatureGuide.init(this).with(FeatureGuide.Technique.CLICK)
                        .setToolTip(toolTip)
                        .setOverlay(new Overlay().setBackgroundColor(Color.parseColor("#AA000000")))
                        .playOn(anchorView);

            }
        } else {
            throw new NullPointerException("anchor menuItem haven`t actionViewClass");
        }
    }

    private void initFeatureCircleGuide(final View item)
    {
        BaseTooltip toolTip = new BaseTooltip()
                .setTitle(getResources().getString(R.string.button_guide_title))
                .setTextColor(Color.parseColor("#131720"))
                .setBackgroundColor(Color.parseColor("#FFFFFF"))
                .setDescription(new SpannableString(getResources().getString(R.string.button_guide_description)));

        featureGuideHandler = FeatureGuide.init(this).with(FeatureGuide.Technique.CLICK)
                .setToolTip(toolTip)
                .setOverlay(new Overlay().setBackgroundColor(Color.parseColor("#AA000000")))
                .playOn(item);
    }

    private void requirePermissions()
    {
        ActivityCompat.requestPermissions(
                this,
                new String[]{CAMERA, RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE},
                REQUEST_ALL
        );
    }

    private void closeService()
    {
        // Kills RTSP server
        this.stopService(new Intent(this, CustomRtspServer.class));
        // Returns to home menu
        finish();
    }

    private void removeNotification()
    {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    }

    private void cleanFeatureGuide()
    {
        if (featureGuideHandler != null) {
            featureGuideHandler.cleanUp();

            featureGuide = false;
            PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(KEY_FEATURE_GUIDE, featureGuide);
            editor.apply();
        }
    }
}