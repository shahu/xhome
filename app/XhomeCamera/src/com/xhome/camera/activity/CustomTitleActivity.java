/*******************************************************************************
*
*
*    xhome-camera
*
*    CustomTitleActivity
*    TODO File description or class description.
*
*    @author: Evilsylvana
*    @since:  2013-9-7
*    @version: 1.0
*
******************************************************************************/
package com.xhome.camera.activity;

import com.xhome.camera.R;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * CustomTitleActivity of xhome-camera.
 * @author Evilsylvana
 *
 */

public class CustomTitleActivity extends Activity {

    private FrameLayout contentFrameLayout;
    private RelativeLayout titleLayout;
    private TextView titleTextView;
    private Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.custom_title);
        initContentView();
    }

    private void initContentView() {
        titleTextView = (TextView) findViewById(R.id.titleName);
        contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
        titleLayout = (RelativeLayout) findViewById(R.id.titleLayout);
        addButton = (Button)findViewById(R.id.btn_add);
    }

    @Override
    public void setContentView(View view) {
        contentFrameLayout.removeAllViews();
        contentFrameLayout.addView(view);
        onContentChanged();
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
        contentFrameLayout.removeAllViews();
        contentFrameLayout.addView(view, params);
        onContentChanged();
    }

    @Override
    public void setContentView(int layoutResID) {
        contentFrameLayout.removeAllViews();
        View.inflate(this, layoutResID, contentFrameLayout);
        onContentChanged();
    }

    public void setCustomTitle(CharSequence charSequence) {
        titleTextView.setText(charSequence);
    }

    public void setCustomTitle(int id) {
        titleTextView.setText(getResources().getString(id));
    }

    public void setCustomTitleColor(int colorID) {
        titleTextView.setTextColor(colorID);
    }

    public void setCustomTitleSize(int size) {
        titleTextView.setTextSize(size);
    }
    public void setCustomTitleColor(ColorStateList charSequence) {
        titleTextView.setTextColor(charSequence);
    }
    public void setTitleBackgroundColor(int color) {
        titleLayout.setBackgroundColor(color);
    }
    public void setTitleBackground(int resouceID) {
        titleLayout.setBackgroundResource(resouceID);
    }
    public void setTitleBackground(Drawable drawable) {
        titleLayout.setBackgroundDrawable(drawable);
    }
}
