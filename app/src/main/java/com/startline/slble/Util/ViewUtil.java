package com.startline.slble.Util;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by terry on 2014/12/26.
 */
public class ViewUtil
{
	public static final int MARGIN_TOP = 0;
	public static final int MARGIN_LEFT = 1;
	public static final int MARGIN_BOTTOM = 2;
	public static final int MARGIN_RIGHT = 3;

	// headView的width以及height
    public static void measureView(View child)
    {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null)
        {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

	public static void changeMarginAsAnimation(final View v,final float startMargin,final float endMargin,final int direction,final int duration,final Runnable callbackRunnable)
	{
		final Animation animation = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				final int newMargin = interpolatedTime == 1? (int)endMargin : (int)((endMargin-startMargin) * interpolatedTime+startMargin);
				final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)v.getLayoutParams();

				setMargin(layoutParams,direction,newMargin);
				v.setLayoutParams(layoutParams);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds()
			{
				return true;
			}
		};

		// 1dp/ms
		//animation.setDuration((int)((endMargin-startMargin)*3 / v.getContext().getResources().getDisplayMetrics().density));
		animation.setDuration(duration);
		v.startAnimation(animation);
	}

	public static void expandHeight(final View v,final int startHeight,final int targetHeight,final int duration,final Runnable callbackRunnable)
	{
		v.setVisibility(View.VISIBLE);
		v.measure(View.MeasureSpec.AT_MOST, View.MeasureSpec.UNSPECIFIED);
		final int measureHeight = v.getMeasuredHeight();
		final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)v.getLayoutParams();
		final Animation animation = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				final int newHeight = interpolatedTime == 1? targetHeight :(int)((targetHeight-startHeight) * interpolatedTime+startHeight);

				layoutParams.height = newHeight;
				v.setLayoutParams(layoutParams);

				if(interpolatedTime == 1)
				{
					if(callbackRunnable != null)
						callbackRunnable.run();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		//animation.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
		animation.setDuration(duration);
		v.startAnimation(animation);
	}

	public static void collapseHeight(final View v,final int startHeight,final int targetHeight,final int visible,final int duration,final Runnable callbackRunnable)
	{
		final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)v.getLayoutParams();
		final Animation animation = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				final int newHeight = interpolatedTime == 1? targetHeight:startHeight-((int)((startHeight-targetHeight)*interpolatedTime));

				layoutParams.height = newHeight;
				v.setLayoutParams(layoutParams);

				if(interpolatedTime == 1)
				{
					v.setVisibility(visible);
					if(callbackRunnable != null)
						callbackRunnable.run();
				}
			}

			@Override
			public boolean willChangeBounds()
			{
				return true;
			}
		};

		// 1dp/ms
		//animation.setDuration((int)((startHeight - targetHeight) / v.getContext().getResources().getDisplayMetrics().density));
		animation.setDuration(duration);
		v.startAnimation(animation);
	}

	private static void setMargin(final ViewGroup.MarginLayoutParams layoutParams,final int direction,final int margin)
	{
		switch(direction)
		{
			case MARGIN_TOP:
				layoutParams.topMargin = margin;
				break;
			case MARGIN_LEFT:
				layoutParams.leftMargin = margin;
				break;
			case MARGIN_BOTTOM:
				layoutParams.bottomMargin = margin;
				break;
			case MARGIN_RIGHT:
				layoutParams.rightMargin = margin;
				break;
		}
	}

}
