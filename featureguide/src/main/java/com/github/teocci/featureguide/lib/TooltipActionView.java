package com.github.teocci.featureguide.lib;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Implementation menu item wrapper view for CustomTooltip
 */
public class TooltipActionView extends FrameLayout {
    private TextView mTextView;
    private ImageView mImageView;
    private MenuItem mMenuItem;

    private MenuItem.OnMenuItemClickListener mOnMenuItemClickListener;

    public TooltipActionView(Context context) {
        this(context, null);
    }

    public TooltipActionView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.actionButtonStyle);
    }

    public TooltipActionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        setLongClickable(true);

        int itemWidth = getResources().getDimensionPixelSize(R.dimen.action_button_width);
        int itemPadding = getResources().getDimensionPixelSize(R.dimen.action_button_padding);
        LayoutParams layoutParams = new LayoutParams(itemWidth - itemPadding, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);

        mTextView = new TextView(context);
        mImageView = new ImageView(context);

        mTextView.setDuplicateParentStateEnabled(true);
        mImageView.setDuplicateParentStateEnabled(true);

        addView(mTextView, layoutParams);
        addView(mImageView, layoutParams);
    }

    @Override
    public boolean performClick() {
        if (mOnMenuItemClickListener != null) {
            mOnMenuItemClickListener.onMenuItemClick(mMenuItem);
        }
        return super.performClick();
    }

    @Override
    public boolean performLongClick() {
        if (mMenuItem != null && !TextUtils.isEmpty(mMenuItem.getTitle())) {
            final int[] screenPos = new int[2];
            final Rect displayFrame = new Rect();
            getLocationOnScreen(screenPos);
            getWindowVisibleDisplayFrame(displayFrame);
            final Context context = getContext();
            final int width = getWidth();
            final int height = getHeight();
            final int middleY = screenPos[1] + height / 2;
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            Toast cheatSheet = Toast.makeText(context, mMenuItem.getTitle(), Toast.LENGTH_SHORT);
            if (middleY < displayFrame.height()) {
                cheatSheet.setGravity(Gravity.TOP | Gravity.END, screenWidth - screenPos[0] - width / 2, height);
            } else {
                cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
            }
            cheatSheet.show();
        }
        return super.performLongClick();
    }

    @Nullable
    public MenuItem getMenuItem() {
        return mMenuItem;
    }

    public void setMenuItem(@NonNull MenuItem menuItem) {
        if (mMenuItem != menuItem) {
            mMenuItem = menuItem;

            View actionView = menuItem.getActionView();
            if (actionView != null && actionView.equals(this)) {
                if (menuItem.getIcon() != null) {
                    mImageView.setImageDrawable(menuItem.getIcon());
                } else if (menuItem.getTitle() != null) {
                    mTextView.setText(menuItem.getTitle());
                }
            }
        }
    }

    public void setOnMenuItemClick(MenuItem.OnMenuItemClickListener l) {
        mOnMenuItemClickListener = l;
    }
}
