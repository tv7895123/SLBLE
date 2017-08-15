package com.startline.slble.view;


import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.View;

import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.startline.slble.R;


public class MyProgressDialog extends ProgressDialog
{
    private ImageView progressImage = null;
    private TextView txtProgressMessage = null;
    private ProgressBar progressBar;
    private CheckedTextView txtButton = null;
	private int layout = R.layout.my_progress_dialog;
	private int mImageSrc = 0;


    public MyProgressDialog(Context context, int theme, final int layout)
    {
        super(context, theme);
		this.layout = layout;
    }

    public MyProgressDialog(Context context, int theme)
    {
        super(context, theme);
    }

    public MyProgressDialog(Context context)
    {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(layout);

        progressBar = (ProgressBar)findViewById(R.id.progress_bar);
        progressImage = (ImageView) findViewById(R.id.img_progress);
        txtProgressMessage = (TextView)findViewById(R.id.txt_progress_message);
        txtButton = (CheckedTextView)findViewById(R.id.txt_button);
        txtButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });
    }


    @Override
    public void setMessage(CharSequence message)
    {

        if(txtProgressMessage != null)
        {
            txtProgressMessage.setText(message);
        }
    }

    public void showProgressMessage(final boolean show)
    {
        if(show)
        {
            txtProgressMessage.setVisibility(View.VISIBLE);
        }
        else
        {
            txtProgressMessage.setVisibility(View.GONE);
        }
    }

    public void showProgressBar(final boolean show)
    {
        if(show)
        {
            progressBar.setVisibility(View.VISIBLE);
            showImage(false);
        }
        else
        {
            progressBar.setVisibility(View.GONE);
        }
    }

    public void setImageSrc(final int src)
    {
        mImageSrc = src;
        progressImage.setImageResource(src);
    }

    public void showImage(final boolean show)
    {
        if(mImageSrc == 0)
            return;

        if(show)
        {
            progressImage.setVisibility(View.VISIBLE);
            showProgressBar(false);
            //((Animatable) progressImage.getDrawable()).start();
        }
        else
        {
            progressImage.setVisibility(View.GONE);
        }
    }

    public void setButtonText(final String text)
    {
        txtButton.setText(text);
    }

    public void showButton(final boolean show)
    {
        if(show)
        {
            txtButton.setVisibility(View.VISIBLE);
        }
        else
        {
            txtButton.setVisibility(View.INVISIBLE);
        }
    }

    public void setOnButtonClick(final View.OnClickListener onClick)
    {
        txtButton.setOnClickListener(onClick);
    }
}
