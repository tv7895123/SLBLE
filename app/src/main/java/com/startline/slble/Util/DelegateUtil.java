package com.startline.slble.Util;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 2014/5/28
 * Time: ?? 9:32
 * To change this template use File | Settings | File Templates.
 */
public class DelegateUtil
{
	public static void adjustTouchDelegate(final View view, final Rect expandRect)
    {
        final View parent = (View) view.getParent();
        parent.post
        (
            new Runnable()
            {
                public void run()
                {
                    final Rect r = new Rect();
                    view.getHitRect(r);
                    if(expandRect == null)
                    {
                        r.top -= 20;
                        r.bottom += 20;
                        r.left -= 20;
                        r.right += 20;
                    }
                    else
                    {
                        r.top -= expandRect.top;
                        r.bottom += expandRect.bottom;
                        r.left -= expandRect.left;
                        r.right += expandRect.right;
                    }

                    parent.setTouchDelegate( new TouchDelegate( r , view));
                }
            }
        );
    }
}
