package com.github.teocci.featureguide.lib;

import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;

/**
 * {@link Overlay} shows a tinted background to cover up the rest of the screen. A 'hole'
 * will be made on this overlay to let users obtain focus on the targeted element.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Overlay
{
    public final static int NOT_SET = -1;

    public int backgroundColor;
    public boolean disableClick;
    public boolean disableClickThroughHole;

    public Style overlayStyle;
    public Animation enterAnimation, exitAnimation;

    public int holeOffsetLeft = 0;
    public int holeOffsetTop = 0;
    public int holeRadius = NOT_SET;

    public View.OnClickListener onClickListener;
    public int paddingDp = 10;
    public int roundedCornerRadiusDp = 0;

    public enum Style
    {
        CIRCLE, RECTANGLE, ROUNDED_RECTANGLE, NO_HOLE
    }

    public Overlay()
    {
        this(true, Color.parseColor("#55000000"), Style.CIRCLE);
    }

    public Overlay(boolean disableClick, int backgroundColor, Style overlayStyle)
    {
        this.disableClick = disableClick;
        this.backgroundColor = backgroundColor;
        this.overlayStyle = overlayStyle;
    }

    /**
     * Set background color
     *
     * @param backgroundColor
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setBackgroundColor(int backgroundColor)
    {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * Set to true if you want to block all user input to pass through this overlay, set to false if you want to allow user input under the overlay
     *
     * @param yesNo
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay disableClick(boolean yesNo)
    {
        disableClick = yesNo;
        return this;
    }

    /**
     * Set to true if you want to disallow the highlighted view to be clicked through the hole,
     * set to false if you want to allow the highlighted view to be clicked through the hole
     *
     * @param yesNo
     * @return return Overlay instance for chaining purpose
     */
    public Overlay disableClickThroughHole(boolean yesNo)
    {
        disableClickThroughHole = yesNo;
        return this;
    }

    public Overlay setOverlayStyle(Style overlayStyle)
    {
        this.overlayStyle = overlayStyle;
        return this;
    }

    /**
     * Set enter animation
     *
     * @param enterAnimation
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setEnterAnimation(Animation enterAnimation)
    {
        this.enterAnimation = enterAnimation;
        return this;
    }

    /**
     * Set exit animation
     *
     * @param exitAnimation
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setExitAnimation(Animation exitAnimation)
    {
        this.exitAnimation = exitAnimation;
        return this;
    }

    /**
     * Set {@link Overlay#onClickListener} for the {@link Overlay}
     *
     * @param onClickListener
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setOnClickListener(View.OnClickListener onClickListener)
    {
        this.onClickListener = onClickListener;
        return this;
    }

    /**
     * This method sets the hole's radius.
     * If this is not set, the size of view hole fill follow the max(view.width, view.height)
     * If this is set, it will take precedence
     * It only has effect when {@link Overlay.Style#CIRCLE} is chosen
     *
     * @param holeRadius the radius of the view hole, setting 0 will make the hole disappear, in pixels
     * @return return {@link Overlay} instance for chaining purpose
     */
    public Overlay setHoleRadius(int holeRadius)
    {
        this.holeRadius = holeRadius;
        return this;
    }


    /**
     * This method sets offsets to the hole's position relative the position of the targeted view.
     *
     * @param offsetLeft left offset, in pixels
     * @param offsetTop  top offset, in pixels
     * @return {@link Overlay} instance for chaining purpose
     */
    public Overlay setHoleOffsets(int offsetLeft, int offsetTop)
    {
        holeOffsetLeft = offsetLeft;
        holeOffsetTop = offsetTop;
        return this;
    }

    /**
     * This method sets the padding to be applied to the hole cutout from the overlay
     *
     * @param paddingDp padding, in dp
     * @return {@link Overlay} intance for chaining purpose
     */
    public Overlay setHolePadding(int paddingDp)
    {
        this.paddingDp = paddingDp;
        return this;
    }

    /**
     * This method sets the radius for the rounded corner
     * It only has effect when {@link Overlay.Style#ROUNDED_RECTANGLE} is chosen
     *
     * @param roundedCornerRadiusDp padding, in pixels
     * @return {@link Overlay} intance for chaining purpose
     */
    public Overlay setRoundedCornerRadius(int roundedCornerRadiusDp)
    {
        this.roundedCornerRadiusDp = roundedCornerRadiusDp;
        return this;
    }
}
