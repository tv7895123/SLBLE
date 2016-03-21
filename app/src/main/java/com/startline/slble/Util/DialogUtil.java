package com.startline.slble.Util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.*;
import android.widget.ListAdapter;
import com.startline.slble.R;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 2014/7/24
 * Time: 下午 7:03
 * To change this template use File | Settings | File Templates.
 */
public class DialogUtil
{
	private static String TAG = DialogUtil.class.getName();
	private static String mPackageName = "";
	private static final int DIALOG_MAX_WIDTH = 720; // 1440*0.5
	private static final int DIALOG_MAX_HEIGHT = 1280;// 2560*0.5
	private static final int DIALOG_MIN_WIDTH = 192; // 320*0.6
	private static final int DIALOG_MIN_HEIGHT = 288;// 480*0.6

	private static final float DEFAULT_DIALOG_HEIGHT = 0.6f;
	public static final int MAXIMUM_DIALOG_NUMBER = 10;
	public static int dialogPointer = -1;
	public static float limitMaxHeight = DEFAULT_DIALOG_HEIGHT;
	public static CustomDialog[] dialogQueue = new CustomDialog[MAXIMUM_DIALOG_NUMBER];
	public static DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener()
	{
		@Override
		public void onDismiss(DialogInterface dialog)
		{
			dismissDialog();
		}
	};

	static class CustomDialog
	{
		public CustomDialog()
		{

		}
		public Context context;
		public AlertDialog alertDialog;
	}

	public static void setLimitMaxHeight(final float limitHeight)
	{
		if(limitHeight >= 0)
			limitMaxHeight = limitHeight;
	}

	public static void resetLimitMaxHeight()
	{
		limitMaxHeight = DEFAULT_DIALOG_HEIGHT;
	}

	public static View limitViewMaxHeight(final Context context,final View view)
	{
		final int minWidth = Math.min(Math.max((int)(context.getResources().getDisplayMetrics().widthPixels*0.7),DIALOG_MIN_WIDTH),DIALOG_MAX_WIDTH);
		final int minHeight = Math.min(Math.max((int)(context.getResources().getDisplayMetrics().heightPixels*0.5),DIALOG_MIN_HEIGHT),DIALOG_MAX_HEIGHT);
		view.setMinimumWidth(minWidth);
		view.setMinimumHeight(minHeight);

		return view;
	}

	public static void limitMaxHeight(final Context context,final Window dialogWindow)
	{
		if(limitMaxHeight > 0)
		{
			final WindowManager.LayoutParams lp = dialogWindow.getAttributes();
			if(limitMaxHeight < 1)
			{
				lp.height = (int)(context.getResources().getDisplayMetrics().heightPixels*limitMaxHeight);
			}
			else
			{
				lp.height = (int)limitMaxHeight;
			}
			dialogWindow.setAttributes(lp);
		}
	}

//	public static void limitContentMaxHeight(final Context context,final View view)
//	{
//		if(limitMaxHeight > 0)
//		{
//			final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//			if(limitMaxHeight < 1)
//			{
//				lp.height = (int)(context.getResources().getDisplayMetrics().heightPixels*limitMaxHeight);
//			}
//			else
//			{
//				lp.height = (int)limitMaxHeight;
//			}
//			view.setLayoutParams(lp);
//		}
//	}

	public static void showDialog()
	{
		if(dialogPointer != -1 && dialogQueue[dialogPointer] != null) return;

		boolean catchError;

		do
		{
			catchError = false;
			try
			{
				final CustomDialog customDialog = dialogQueue[(dialogPointer+1)%MAXIMUM_DIALOG_NUMBER];
				if(customDialog != null && customDialog.alertDialog != null)
				{
					dialogPointer = (dialogPointer+1) % MAXIMUM_DIALOG_NUMBER;
					customDialog.alertDialog.show();

					final Window dialogWindow = customDialog.alertDialog.getWindow();
					limitMaxHeight(customDialog.context,dialogWindow);

					break;
				}
			}
			catch (Exception e)
			{
				catchError = true;
				dialogQueue[dialogPointer] = null;
				LogUtil.e(getPackageName(), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
			}
		}
		while (catchError);
	}

	public static void dismissDialog()
	{
		try
		{
			final CustomDialog customDialog = dialogQueue[dialogPointer];
			if(customDialog.alertDialog != null)
			{
				customDialog.alertDialog.dismiss();
				dialogQueue[dialogPointer] = null;
				showDialog();
			}
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}
	}

	public static boolean addDialogWindow(final Context context,final AlertDialog dialog)
	{
//		if(dialogPointer == -1)
//		{
//			final CustomDialog customDialog = new CustomDialog();
//			customDialog.context = context;
//			customDialog.alertDialog = dialog;
//			dialogQueue[0] = customDialog;
//			return true;
//		}
		try
		{
			for(int i = (dialogPointer+1)%MAXIMUM_DIALOG_NUMBER; i!=dialogPointer; i=(i+1)%MAXIMUM_DIALOG_NUMBER)
			{
				if(dialogQueue[i] == null)
				{
					final CustomDialog customDialog = new CustomDialog();
					customDialog.context = context;
					customDialog.alertDialog = dialog;
					dialogQueue[i] = customDialog;
					return true;
				}
			}
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}

		return false;
	}

	// Show dialog with single choice item
	public static void singleChoiceDialog(final Context context,final String title,final String[] items,final int defaultIndex
										,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
										,final DialogInterface.OnClickListener onItemClick
										,final int positiveStringId,final int negativeStringId)
	{
		try
		{
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));

			if(title.length() > 0)
				dialogBuilder.setTitle(title);

			if(positiveStringId > 0)
			{
				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
			}

			if(negativeStringId > 0)
			{
				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
			}

			dialogBuilder.setSingleChoiceItems(items, defaultIndex, onItemClick);
			final AlertDialog alertDialog = dialogBuilder.create();
			alertDialog.show();

			final Window dialogWindow = alertDialog.getWindow();
			limitMaxHeight(context,dialogWindow);
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}
	}


	// Show dialog with single choice item
	public static void customSingleChoiceDialog(final Context context,final String title,final View view,final ListAdapter listAdapter,final int defaultIndex
										,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
										,final DialogInterface.OnClickListener onItemClick
										,final int positiveStringId,final int negativeStringId)
	{
		try
		{
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));

			if(title.length() > 0)
				dialogBuilder.setTitle(title);

			if(positiveStringId > 0)
			{
				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
			}

			if(negativeStringId > 0)
			{
				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
			}

			dialogBuilder.setView(view);
			dialogBuilder.setSingleChoiceItems(listAdapter, defaultIndex, onItemClick);
			final AlertDialog alertDialog = dialogBuilder.create();
			alertDialog.show();

			final Window dialogWindow = alertDialog.getWindow();
			limitMaxHeight(context,dialogWindow);
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}
	}

	// Set a custom description view to dialog
//	public static Dialog descriptionDialog(final Context context,final String title,final String message,final String remark
//									,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
//									,final int positiveStringId,final int negativeStringId)
//	{
//		try
//		{
//			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));
//			final View view = View.inflate(context,R.layout.dialog_explanation_window,null);
//
//			if(view != null)
//			{
//				dialogBuilder.setView(limitViewMaxHeight(context,view));
//				if(title!= null && title.length() > 0)
//				{
//					((TextView)(view.findViewById(R.id.txt_dialog_title))).setText(title);
//				}
//
//				if(message != null && message.length() > 0)
//				{
//
//					((TextView)(view.findViewById(R.id.txt_dialog_message))).setText(Html.fromHtml(message.replace("\n","<BR/>")));
//				}
//
//				if(remark != null && remark.length() > 0)
//				{
//					((TextView)(view.findViewById(R.id.txt_dialog_message_remark))).setText(remark);
//				}
//			}
//
//
//			if(positiveStringId > 0)
//			{
//				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
//			}
//
//			if(negativeStringId > 0)
//			{
//				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
//			}
//
//
//
//			final AlertDialog alertDialog = dialogBuilder.create();
//			alertDialog.getWindow().setWindowAnimations(R.style.DialogNoAnimation);
//			alertDialog.show();
//
//			final Button b = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
//			if(b != null)
//			{
//				b.setBackgroundResource(R.drawable.btn_description_window_positive_background_selector);
//				b.setTextColor(context.getResources().getColorStateList(R.color.txt_description_window_positive_color_selector));
//			}
//			return alertDialog;
//		}
//		catch (Exception e)
//		{
//			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
//		}
//
//		return null;
//	}

	// Set a custom view to dialog
	public static Dialog customViewDialog(final Context context,final String title,final View view
									,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
									,final int positiveStringId,final int negativeStringId)
	{
		try
		{
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));

			if(title.length() > 0)
				dialogBuilder.setTitle(title);

			if(view != null)
				dialogBuilder.setView(view);

			if(positiveStringId > 0)
			{
				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
			}

			if(negativeStringId > 0)
			{
				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
			}

			final AlertDialog alertDialog = dialogBuilder.create();
			alertDialog.show();

			final Window dialogWindow = alertDialog.getWindow();
			limitMaxHeight(context,dialogWindow);

			return alertDialog;
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}

		return null;
	}

	// Simple title and message dialog
	public static void  messageDialog(final Context context,final String title,final String message
									,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
									,final int positiveStringId,final int negativeStringId)
	{
		try
		{
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));

			if(title.length() > 0)
				dialogBuilder.setTitle(title);

			dialogBuilder.setMessage("\n"+message+"\n");

			if(positiveStringId > 0)
			{
				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
			}

			if(negativeStringId > 0)
			{
				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
			}

			final AlertDialog alertDialog = dialogBuilder.create();
			alertDialog.show();

		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}
	}


	// Set a custom view to dialog
	public static Dialog customViewSystemMessageDialog(final Context context,final String title,final View view
									,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
									,final int positiveStringId,final int negativeStringId)
	{
		try
		{
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));

			if(title.length() > 0)
				dialogBuilder.setTitle(title);

			if(view != null)
				dialogBuilder.setView(view);

			if(positiveStringId > 0)
			{
				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
			}

			if(negativeStringId > 0)
			{
				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
			}

			final AlertDialog alertDialog = dialogBuilder.create();
			final Window dialogWindow = alertDialog.getWindow();
			dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			alertDialog.show();
			dialogWindow.setGravity(Gravity.CENTER);
			limitMaxHeight(context, dialogWindow);

			return alertDialog;
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}

		return null;
	}

	// For service showing dialog
	public static void  systemMessageDialog(final Context context,final String title,final String message
									,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
									,final int positiveStringId,final int negativeStringId)
	{
		try
		{
			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));

			if(title.length() > 0)
				dialogBuilder.setTitle(title);

			dialogBuilder.setMessage("\n"+message+"\n");

			if(positiveStringId > 0)
			{
				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
			}

			if(negativeStringId > 0)
			{
				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
			}

			final AlertDialog alertDialog = dialogBuilder.create();
			final Window dialogWindow = alertDialog.getWindow();
			dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			alertDialog.show();
			dialogWindow.setGravity(Gravity.CENTER);

			limitMaxHeight(context,dialogWindow);
		}
		catch (Exception e)
		{
			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
		}
	}

	// Set a custom view to dialog
	// Only one dialog can show on window at one time
//	public static Dialog customViewDialogBlock(final Context context,final String title,final View view
//									,final DialogInterface.OnClickListener onOkClickListener,final DialogInterface.OnClickListener onCancelClickListener
//									,final int positiveStringId,final int negativeStringId)
//	{
//		try
//		{
//			final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.dialog_theme));
//
//			if(title.length() > 0)
//				dialogBuilder.setTitle(title);
//
//			if(view != null)
//				dialogBuilder.setView(view);
//
//			if(positiveStringId > 0)
//			{
//				dialogBuilder.setPositiveButton(context.getString(positiveStringId), onOkClickListener);
//			}
//
//			if(negativeStringId > 0)
//			{
//				dialogBuilder.setNegativeButton(context.getString(negativeStringId), onCancelClickListener);
//			}
//
//			final AlertDialog alertDialog = dialogBuilder.create();
//			alertDialog.setOnDismissListener(onDismissListener);
//			addDialogWindow(context,alertDialog);
//			showDialog();
//
//			return alertDialog;
//		}
//		catch (Exception e)
//		{
//			LogUtil.e(getPackageName(context), String.format("[%s] - %s", TAG,e.toString()), Thread.currentThread().getStackTrace());
//		}
//
//		return null;
//	}

	private static String getPackageName()
	{
		return mPackageName;
	}

	private static String getPackageName(final Context context)
	{
		mPackageName = context.getPackageName();
		return mPackageName;
	}
}
