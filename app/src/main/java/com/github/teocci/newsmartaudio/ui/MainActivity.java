package com.github.teocci.newsmartaudio.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import static com.github.teocci.newsmartaudio.utils.Config.KEY_FEATURE_GUIDE;
import static com.github.teocci.newsmartaudio.utils.Config.NOTIFICATION_ENABLED;

public class MainActivity extends AppCompatActivity
{
    static final public String TAG = MainActivity.class.getSimpleName();

    public FeatureGuide featureGuideHandler;
    public FeatureGuide fgHandler;

    private CircleButtonView circleButton;

    private boolean featureGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void closeService()
    {
        // Removes notification
        if (NOTIFICATION_ENABLED) removeNotification();
        // Kills RTSP server
        this.stopService(new Intent(this, CustomRtspServer.class));
        // Returns to home menu
        finish();
    }

    private void removeNotification()
    {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    }
}