package com.github.teocci.featureguide.lib;

import android.view.View;

/**
 * {@link Sequence} is used with {@link ChainFeatureGuide} to make FeatureGuide run in row.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Sequence
{
    ChainFeatureGuide[] featureGuideArray;
    private Overlay defaultOverlay;
    private BaseTooltip defaultToolTip;
    private Pointer defaultPointer;

    private ContinueMethod continueMethod;
    private boolean disableTargetButton;
    public int currentSequence;

    ChainFeatureGuide parentTourGuide;

    /**
     * {@link ContinueMethod#OVERLAY} -
     * {@link ContinueMethod#OVERLAY_LISTENER} -
     */
    public enum ContinueMethod
    {
        OVERLAY, OVERLAY_LISTENER
    }

    private Sequence(SequenceBuilder builder)
    {
        this.featureGuideArray = builder.chainFeatureArray;
        this.defaultOverlay = builder.defaultOverlay;
        this.defaultToolTip = builder.defaultTooltip;
        this.defaultPointer = builder.defaultPointer;
        this.continueMethod = builder.continueMethod;
        this.currentSequence = builder.currentSequence;

        // TODO: to be implemented
        this.disableTargetButton = builder.disableTargetButton;
    }

    /**
     * sets the parent FeatureGuide that will run this Sequence
     */
    protected void setParentTourGuide(ChainFeatureGuide parentTourGuide)
    {
        this.parentTourGuide = parentTourGuide;

        if (continueMethod == ContinueMethod.OVERLAY) {
            for (final ChainFeatureGuide tourGuide : featureGuideArray) {
                tourGuide.overlay.onClickListener = new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Sequence.this.parentTourGuide.next();
                    }
                };
            }
        }
    }

    public ChainFeatureGuide getNextTourGuide()
    {
        return featureGuideArray[currentSequence];
    }

    public ContinueMethod getContinueMethod()
    {
        return continueMethod;
    }

    public ChainFeatureGuide[] getTourGuideArray()
    {
        return featureGuideArray;
    }

    public Overlay getDefaultOverlay()
    {
        return defaultOverlay;
    }

    public BaseTooltip getDefaultToolTip()
    {
        return defaultToolTip;
    }

    public BaseTooltip getToolTip()
    {
        // individual tour guide has higher priority
        if (featureGuideArray[currentSequence].toolTip != null) {
            return featureGuideArray[currentSequence].toolTip;
        } else {
            return defaultToolTip;
        }
    }

    public Overlay getOverlay()
    {
        // Overlay is used as a method to proceed to next FeatureGuide, so the default overlay is already assigned appropriately if needed
        return featureGuideArray[currentSequence].overlay;
    }

    public Pointer getPointer()
    {
        // individual tour guide has higher priority
        if (featureGuideArray[currentSequence].pointer != null) {
            return featureGuideArray[currentSequence].pointer;
        } else {
            return defaultPointer;
        }
    }

    public static class SequenceBuilder
    {
        ChainFeatureGuide[] chainFeatureArray;
        Overlay defaultOverlay;
        BaseTooltip defaultTooltip;
        Pointer defaultPointer;
        ContinueMethod continueMethod;
        int currentSequence;
        boolean disableTargetButton;

        public SequenceBuilder add(ChainFeatureGuide... tourGuideArray)
        {
            chainFeatureArray = tourGuideArray;
            return this;
        }

        public SequenceBuilder setDefaultOverlay(Overlay defaultOverlay)
        {
            this.defaultOverlay = defaultOverlay;
            return this;
        }

        // This might not be useful, but who knows.. maybe someone needs it
        public SequenceBuilder setDefaultToolTip(BaseTooltip defaultToolTip)
        {
            defaultTooltip = defaultToolTip;
            return this;
        }

        public SequenceBuilder setDefaultPointer(Pointer defaultPointer)
        {
            this.defaultPointer = defaultPointer;
            return this;
        }

        // TODO: this is an uncompleted feature, make it private first
        // This is intended to be used to disable the button, so people cannot click on
        // in during a Tour, instead, people can only click on Next button or Overlay to proceed
        private SequenceBuilder setDisableButton(boolean disableTargetButton)
        {
            this.disableTargetButton = disableTargetButton;
            return this;
        }

        /**
         * @param continueMethod ContinueMethod.OVERLAY or ContinueMethod.OVERLAY_LISTENER
         *                       ContnueMethod.OVERLAY - clicking on Overlay will make FeatureGuide
         *                       proceed to the next one.
         *                       ContinueMethod.OVERLAY_LISTENER - you need to provide OverlayListeners,
         *                       and call tourGuideHandler.next() in the listener to proceed to the next one.
         */
        public SequenceBuilder setContinueMethod(ContinueMethod continueMethod)
        {
            this.continueMethod = continueMethod;
            return this;
        }

        public Sequence build()
        {
            currentSequence = 0;
            checkIfContinueMethodNull();
            checkAtLeastTwoTourGuideSupplied();
            checkOverlayListener(continueMethod);

            return new Sequence(this);
        }

        private void checkIfContinueMethodNull()
        {
            if (continueMethod == null) {
                throw new IllegalArgumentException("Continue Method is not set. Please provide ContinueMethod in setContinueMethod");
            }
        }

        private void checkAtLeastTwoTourGuideSupplied()
        {
            if (chainFeatureArray == null || chainFeatureArray.length <= 1) {
                throw new IllegalArgumentException("In order to run a sequence, you must at least supply 2 FeatureGuide into Sequence using add()");
            }
        }

        private void checkOverlayListener(ContinueMethod continueMethod)
        {
            if (continueMethod == ContinueMethod.OVERLAY_LISTENER) {
                boolean pass = true;
                if (defaultOverlay != null && defaultOverlay.onClickListener != null) {
                    pass = true;
                    // when default listener is available, we loop through individual tour guide, and
                    // assign default listener to individual tour guide
                    for (ChainFeatureGuide tourGuide : chainFeatureArray) {
                        if (tourGuide.overlay == null) {
                            tourGuide.overlay = defaultOverlay;
                        }
                        if (tourGuide.overlay != null && tourGuide.overlay.onClickListener == null) {
                            tourGuide.overlay.onClickListener = defaultOverlay.onClickListener;
                        }
                    }
                } else { // case where: default listener is not available

                    for (ChainFeatureGuide tourGuide : chainFeatureArray) {
                        //Both of the overlay and default listener is not null, throw the error
                        if (tourGuide.overlay != null && tourGuide.overlay.onClickListener == null) {
                            pass = false;
                            break;
                        } else if (tourGuide.overlay == null) {
                            pass = false;
                            break;
                        }
                    }

                }

                if (!pass) {
                    throw new IllegalArgumentException("ContinueMethod.OVERLAY_LISTENER is chosen as the ContinueMethod, but no Default Overlay Listener is set, or not all OVERLAY.mListener is set for all the FeatureGuide passed in.");
                }
            } else if (continueMethod == ContinueMethod.OVERLAY) {
                // when Overlay ContinueMethod is used, listener must not be supplied (to avoid unexpected result)
                boolean pass = true;
                if (defaultOverlay != null && defaultOverlay.onClickListener != null) {
                    pass = false;
                } else {
                    for (ChainFeatureGuide tourGuide : chainFeatureArray) {
                        if (tourGuide.overlay != null && tourGuide.overlay.onClickListener != null) {
                            pass = false;
                            break;
                        }
                    }
                }
                if (defaultOverlay != null) {
                    for (ChainFeatureGuide tourGuide : chainFeatureArray) {
                        if (tourGuide.overlay == null) {
                            tourGuide.overlay = defaultOverlay;
                        }
                    }
                }

                if (!pass) {
                    throw new IllegalArgumentException("ContinueMethod.OVERLAY is chosen as the ContinueMethod, but either default overlay listener is still set OR individual overlay listener is still set, make sure to clear all Overlay listener");
                }
            }
        }
    }
}