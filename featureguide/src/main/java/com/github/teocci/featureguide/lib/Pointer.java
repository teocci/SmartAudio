package com.github.teocci.featureguide.lib;

import android.graphics.Color;
import android.view.Gravity;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Pointer
{
    public int gravity = Gravity.CENTER;
    public int color = Color.WHITE;

    public Pointer()
    {
        this(Gravity.CENTER, Color.parseColor("#FFFFFF"));
    }

    public Pointer(int gravity, int color)
    {
        this.gravity = gravity;
        this.color = color;
    }

    /**
     * Set color
     *
     * @param color
     * @return return Pointer instance for chaining purpose
     */
    public Pointer setColor(int color)
    {
        this.color = color;
        return this;
    }

    /**
     * Set gravity
     *
     * @param gravity
     * @return return Pointer instance for chaining purpose
     */
    public Pointer setGravity(int gravity)
    {
        this.gravity = gravity;
        return this;
    }
}
