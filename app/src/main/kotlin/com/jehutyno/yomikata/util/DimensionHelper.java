package com.jehutyno.yomikata.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

/**
 * Helper for Dimension conversions
 */
public class DimensionHelper {

	/*********************
	 * Static fields
	 *********************/

	private static final int STANDARD_HDPI_WIDTH = 480;
	private static final int STANDARD_HDPI_HEIGHT = 800;
	private static int mScreenWidth, mScreenHeight;
	public static final int NO_CHANGE = Integer.MAX_VALUE;

	/*************************
	 * Helper Static Methods
	 ************************/

	public static void setMarginsLayout(View layout, int left, int top, int right, int bottom) {
		if (layout == null) return;
		MarginLayoutParams mlp = (MarginLayoutParams) layout.getLayoutParams();
		if (left == NO_CHANGE) left = mlp.leftMargin;
		if (right == NO_CHANGE) right = mlp.rightMargin;
		if (top == NO_CHANGE) top = mlp.topMargin;
		if (bottom == NO_CHANGE) bottom = mlp.bottomMargin;
		mlp.setMargins(left, top, right, bottom);
	}

	public static void setPaddingsLayout(View layout, int left, int top, int right, int bottom) {
		if (layout == null) return;
		if (left == NO_CHANGE) left = layout.getPaddingLeft();
		if (right == NO_CHANGE) right = layout.getPaddingRight();
		if (top == NO_CHANGE) top = layout.getPaddingTop();
		if (bottom == NO_CHANGE) bottom = layout.getPaddingBottom();
		layout.setPadding(left, top, right, bottom);
	}

	public static void setDimensionLayout(View layout, int width, int height) {

		if (layout == null) return;

		if (width < 0) width = ViewGroup.LayoutParams.MATCH_PARENT;
		if (width == 0) width = ViewGroup.LayoutParams.WRAP_CONTENT;
		if (height < 0) height = ViewGroup.LayoutParams.MATCH_PARENT;
		if (height == 0) height = ViewGroup.LayoutParams.WRAP_CONTENT;

		ViewGroup.LayoutParams params = layout.getLayoutParams();
		params.width = width;
		params.height = height;
		layout.setLayoutParams(params);
	}

	public static int getPixelFromSp(Context context, int sp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
	}

	public static int getDipFromPixel(Context context, int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px, context.getResources().getDisplayMetrics());
	}

	public static int getPixelFromDip(Context context, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

	public static int getPixelFromDimen(Context context, int res) {
		return context.getResources().getDimensionPixelSize(res);
	}

	public static boolean isXLargeScreen(Context context) {
		int api_level = Build.VERSION.SDK_INT;
		if(api_level <=Build.VERSION_CODES.GINGERBREAD) return false;
		
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	// mdpi & ldpi
	public static boolean isLowDPI(Context context) {
		return context.getResources().getDisplayMetrics().densityDpi < DisplayMetrics.DENSITY_MEDIUM;
	}

	// hdpi
	public static boolean isHDPI(Context context) {
		return context.getResources().getDisplayMetrics().densityDpi == DisplayMetrics.DENSITY_HIGH;
	}

	// xhdpi
	public static boolean isXHDPI(Context context) {
		int api_level = Build.VERSION.SDK_INT;
		if(api_level <=Build.VERSION_CODES.GINGERBREAD) return false;
		
		return context.getResources().getDisplayMetrics().densityDpi == DisplayMetrics.DENSITY_XHIGH;
	}

	// big hdpi (540*960)
	public static boolean isBigHDPI(Context context) {
		return isHDPI(context) && (getScreenWidth(context) > STANDARD_HDPI_WIDTH) && !isXHDPI(context);
	}

	public static int getScreenWidth(Context context) {
		if (mScreenWidth <= 0) setScreenDimensions(context);
		return mScreenWidth;
	}

	public static int getScreenHeight(Context context) {
		if (mScreenHeight <= 0) setScreenDimensions(context);
		return mScreenHeight;
	}

	/**************************
	 * Private Static Methods
	 **************************/

	private static void setScreenDimensions(Context context) {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		mScreenWidth = displaymetrics.widthPixels;
		mScreenHeight = displaymetrics.heightPixels;
	}

}
