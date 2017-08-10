package com.github.teocci.featureguide.lib;

import android.graphics.Color;
import android.text.SpannableString;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class BaseTooltip
{
    public String title;
    public SpannableString description;
    public int backgroundColor, textColor;
    public Animation enterAnimation, exitAnimation;
    public boolean shadow;
    public int gravity;
    public View.OnClickListener onClickListener;
    public ViewGroup customView;
    public int width;

    public BaseTooltip()
    {
        /* default values */
        title = "";
        description = new SpannableString("");
        backgroundColor = Color.parseColor("#3498db");
        textColor = Color.parseColor("#FFFFFF");

        enterAnimation = new AlphaAnimation(0f, 1f);
        enterAnimation.setDuration(1000);
        enterAnimation.setFillAfter(true);
        enterAnimation.setInterpolator(new BounceInterpolator());
        shadow = true;
        width = -1;

        // TODO: exit animation
        gravity = Gravity.CENTER;
    }

    /**
     * Set title text
     *
     * @param title
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setTitle(String title)
    {
        this.title = title;
        return this;
    }

    /**
     * Set description text
     *
     * @param description
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setDescription(SpannableString description)
    {
        this.description = description;
        return this;
    }

    /**
     * Set background color
     *
     * @param backgroundColor
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setBackgroundColor(int backgroundColor)
    {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * Set text color
     *
     * @param textColor
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setTextColor(int textColor)
    {
        this.textColor = textColor;
        return this;
    }

    /**
     * Set enter animation
     *
     * @param enterAnimation
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setEnterAnimation(Animation enterAnimation)
    {
        this.enterAnimation = enterAnimation;
        return this;
    }
    /**
     * Set exit animation
     * @param exitAnimation
     * @return return BaseTooltip instance for chaining purpose
     */
//    TODO:
//    public BaseTooltip setExitAnimation(Animation exitAnimation){
//        exitAnimation = exitAnimation;
//        return this;
//    }

    /**
     * Set the gravity, the setPointerGravity is centered relative to the targeted button
     *
     * @param gravity Gravity.CENTER, Gravity.TOP, Gravity.BOTTOM, etc
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setGravity(int gravity)
    {
        this.gravity = gravity;
        return this;
    }

    /**
     * Set if you want to have setShadow
     *
     * @param shadow
     * @return return BaseTooltip instance for chaining purpose
     */
    public BaseTooltip setShadow(boolean shadow)
    {
        this.shadow = shadow;
        return this;
    }

    /**
     * Method to set the width of the BaseTooltip
     *
     * @param px desired width of BaseTooltip in pixels
     * @return BaseTooltip instance for chaining purposes
     */
    public BaseTooltip setWidth(int px)
    {
        if (px >= 0) width = px;
        return this;
    }

    public BaseTooltip setOnClickListener(View.OnClickListener onClickListener)
    {
        this.onClickListener = onClickListener;
        return this;
    }

    public ViewGroup getCustomView()
    {
        return customView;
    }

    public BaseTooltip setCustomView(ViewGroup view)
    {
        customView = view;
        return this;
    }
}
