package com.github.teocci.newsmartaudio.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;

import com.github.teocci.featureguide.lib.BaseTooltip;
import com.github.teocci.featureguide.lib.FeatureGuide;
import com.github.teocci.featureguide.lib.Overlay;
import com.github.teocci.featureguide.lib.TooltipActionView;
import com.github.teocci.newsmartaudio.R;
import com.github.teocci.newsmartaudio.api.CustomRtspServer;
import com.github.teocci.newsmartaudio.views.CircleButtonView;

import static com.github.teocci.newsmartaudio.utils.Config.NOTIFICATION_ENABLED;

public class MainActivity extends AppCompatActivity
{
    static final public String TAG = MainActivity.class.getSimpleName();

    public static final int OVERLAY_METHOD = 1;
    public static final int OVERLAY_LISTENER_METHOD = 2;

    public static final String CONTINUE_METHOD = "continue_method";

    public FeatureGuide featureGuideHandler;
    public FeatureGuide fgHandler;

    private Animation enterAnimation, exitAnimation;

    private CircleButtonView circleButton;
    private View options;

    private int currentContinueMethod;

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

        Intent intent = getIntent();
        currentContinueMethod = intent.getIntExtra(CONTINUE_METHOD, OVERLAY_METHOD);

        circleButton = (CircleButtonView) findViewById(R.id.circleButton);
        circleButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (featureGuideHandler != null) {
                    featureGuideHandler.cleanUp();
                }
                Intent intent = new Intent(getApplicationContext(), SmartAudioActivity.class);
                startActivity(intent);
            }
        });

//        BaseTooltip toolTip = new BaseTooltip()
//                .setTitle("Welcome!")
//                .setDescription("Click on \"Start\" to begin.");
//
//        featureGuideHandler = FeatureGuide.init(this).with(FeatureGuide.Technique.CLICK)
//                .setToolTip(toolTip)
//                .setOverlay(new Overlay().setBackgroundColor(Color.parseColor("#AA333333")))
//                .playOn(circleButton);
//
//                /* setup enter and exit animation */
//        enterAnimation = new AlphaAnimation(0f, 1f);
//        enterAnimation.setDuration(600);
//        enterAnimation.setFillAfter(true);
//
//        exitAnimation = new AlphaAnimation(1f, 0f);
//        exitAnimation.setDuration(600);
//        exitAnimation.setFillAfter(true);
//
//        if (currentContinueMethod == OVERLAY_LISTENER_METHOD) {
//            ChainFeatureGuide featureGuideOptions = ChainFeatureGuide.init(this)
//                    .setToolTip(new BaseTooltip()
//                            .setTitle("Tip")
//                            .setDescription("Individual Overlay will be used when it's supplied.")
//                            .setGravity(Gravity.BOTTOM | Gravity.START)
//                            .setBackgroundColor(Color.parseColor("#c0392b"))
//                    )
//                    .setOverlay(new Overlay()
//                            .setBackgroundColor(Color.parseColor("#EE2c3e50"))
//                            .setEnterAnimation(enterAnimation)
//                            .setExitAnimation(exitAnimation)
//                    )
//                    .playLater(circleButton);ioi
//
//            ChainFeatureGuide featureGuideCircleButton = ChainFeatureGuide.init(this)
//                    .setToolTip(new BaseTooltip()
//                            .setTitle("Welcome!")
//                            .setDescription("Click on \"Start\" to begin.")
//                            .setGravity(Gravity.BOTTOM | Gravity.START)
//                            .setBackgroundColor(Color.parseColor("#c0392b"))
//                    )
//                    .setOverlay(new Overlay()
//                            .setBackgroundColor(Color.parseColor("#EE2c3e50"))
//                            .setEnterAnimation(enterAnimation)
//                            .setExitAnimation(exitAnimation)
//                    )
//                    .playLater(circleButton);
//
//            Sequence sequence = new Sequence.SequenceBuilder()
//                    .add(featureGuideOptions, featureGuideCircleButton)
//                    .setDefaultOverlay(new Overlay()
//                            .setEnterAnimation(enterAnimation)
//                            .setExitAnimation(exitAnimation)
//                    )
//                    .setDefaultPointer(null)
//                    .setContinueMethod(Sequence.ContinueMethod.OVERLAY)
//                    .build();
//
//
//            ChainFeatureGuide.init(this).playInSequence(sequence);
//        }
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

//        String note = "Click on preferences X if you want to change the device name or the RTSP port.";
//        SpannableString text = new SpannableString(note);
//        text.setSpan(new ForegroundColorSpan(Color.RED), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        Drawable d = ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_settings);
//        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
//        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
//        text.setSpan(span, note.indexOf('X'), note.indexOf('X') + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        MenuItem menuItem = menu.findItem(R.id.menu_options);
        initFeatureMenuGuide(menuItem);
//        CustomTooltip.Builder builder = new CustomTooltip.Builder(menuItem)
//                .setCornerRadius(10f)
//                .setBackgroundColor(Color.parseColor("#0092ff"))
//                .setGravity(Gravity.BOTTOM)
//                .setOnDismissListener(new OnDismissListener() {
//                    @Override
//                    public void onDismiss()
//                    {
//                        initFeatureCircleGuide(circleButton);
//                    }
//                })
//                .setText(text)
//                .setTextColor(Color.parseColor("#131720"))
//                .setDismissOnClick(true);
//        builder.show();

//        onOptionsItemSelected(menu.findItem(R.id.menu_options));
//        findViewById(R.id.menu_options).performClick();
//        return super.onCreateOptionsMenu(menu);
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
        Intent intent;

        switch (item.getItemId()) {
//            case R.id.menu_options:
////                initFeatureMenuGuide(findViewById(R.id.menu_options));
//                // Starts QualityListActivity where user can change the streaming currentQuality
//                intent = new Intent(this.getBaseContext(), PreferencesActivity.class);
//                startActivityForResult(intent, 0);
//                return true;
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