package com.github.teocci.featureguide.lib;

import android.app.Activity;
import android.view.View;

import com.github.teocci.featureguide.lib.utils.LogHelper;

/**
 * {@link ChainFeatureGuide} is designed to be used with {@link Sequence}. The purpose is to run FeatureGuide in a series.
 * {@link ChainFeatureGuide} extends from {@link FeatureGuide} with extra capability to be run in sequence.
 * Check OverlaySequenceTourActivity.java in the Example of FeatureGuide to learn how to use.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class ChainFeatureGuide extends FeatureGuide
{
    private static final String TAG = LogHelper.makeLogTag(ChainFeatureGuide.class);

    private Sequence sequence;

    public ChainFeatureGuide(Activity activity)
    {
        super(activity);
    }

    /* Static builder */
    public static ChainFeatureGuide init(Activity activity)
    {
        return new ChainFeatureGuide(activity);
    }

    @Override
    public FeatureGuide playOn(View targetView)
    {
        throw new RuntimeException("playOn() should not be called ChainFeatureGuide, ChainFeatureGuide is meant to be used with Sequence. Use FeatureGuide class for playOn() for single FeatureGuide. Only use ChainFeatureGuide if you intend to run FeatureGuide in consecutively.");
    }

    public ChainFeatureGuide playLater(View view)
    {
        highlightedView = view;
        return this;
    }

    @Override
    public ChainFeatureGuide with(Technique technique)
    {
        return (ChainFeatureGuide) super.with(technique);
    }

    @Override
    public ChainFeatureGuide motionType(MotionType motionType)
    {
        return (ChainFeatureGuide) super.motionType(motionType);
    }

    @Override
    public ChainFeatureGuide setOverlay(Overlay overlay)
    {
        return (ChainFeatureGuide) super.setOverlay(overlay);
    }

    @Override
    public ChainFeatureGuide setToolTip(BaseTooltip toolTip)
    {
        return (ChainFeatureGuide) super.setToolTip(toolTip);
    }

    @Override
    public ChainFeatureGuide setPointer(Pointer pointer)
    {
        return (ChainFeatureGuide) super.setPointer(pointer);
    }

    public ChainFeatureGuide next()
    {
        if (frameLayout != null) {
            cleanUp();
        }

        if (sequence.currentSequence < sequence.featureGuideArray.length) {
            setToolTip(sequence.getToolTip());
            setPointer(sequence.getPointer());
            setOverlay(sequence.getOverlay());

            highlightedView = sequence.getNextTourGuide().highlightedView;

            setupView();
            sequence.currentSequence++;
        }
        return this;
    }

    /**
     * Sequence related method
     */
    public ChainFeatureGuide playInSequence(Sequence sequence)
    {
        setSequence(sequence);
        next();
        return this;
    }

    public ChainFeatureGuide setSequence(Sequence sequence)
    {
        this.sequence = sequence;
        this.sequence.setParentTourGuide(this);
        for (ChainFeatureGuide tourGuide : sequence.featureGuideArray) {
            if (tourGuide.highlightedView == null) {
                throw new NullPointerException("Please specify the view using 'playLater' method");
            }
        }
        return this;
    }
}
