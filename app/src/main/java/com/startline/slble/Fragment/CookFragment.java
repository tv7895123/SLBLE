package com.startline.slble.Fragment;

/**
 * Created by terry on 2016/6/8.
 */
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.startline.slble.R;


public class CookFragment extends BaseFragment
{

    private static final String DATA_NAME = "name";

    private String title = "";

    public static CookFragment newInstance(String title, int indicatorColor,
                                           int dividerColor) {
        CookFragment f = new CookFragment();
        f.setTitle(title);
        f.setIndicatorColor(indicatorColor);
        f.setDividerColor(dividerColor);

        //pass data
        Bundle args = new Bundle();
        args.putString(DATA_NAME, title);
        f.setArguments(args);

        return f;
    }

    public static CookFragment newInstance(String title, int indicatorColor,
                                           int dividerColor, int iconResId) {
        CookFragment f = new CookFragment();
        f.setTitle(title);
        f.setIndicatorColor(indicatorColor);
        f.setDividerColor(dividerColor);
        f.setIconResId(iconResId);

        //pass data
        Bundle args = new Bundle();
        args.putString(DATA_NAME, title);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get data
        title = getArguments().getString(DATA_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //layout
        View view = inflater.inflate(R.layout.mobile_number, container, false);

        //view
        TextView txtName = (TextView) view.findViewById(R.id.txt_mobile_number_value);
        txtName.setText(title);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
