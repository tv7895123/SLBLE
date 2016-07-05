package com.startline.slble.Fragment;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.startline.slble.Adapter.TabFragmentPagerAdapter;
import com.startline.slble.Interface.OnProgramDataChangedListener;
import com.startline.slble.Pager.CustomViewPager;
import com.startline.slble.R;
import com.startline.slble.view.SlidingTabLayout;

public class TabFragment extends Fragment
{
    private SlidingTabLayout tabs;
    private CustomViewPager pager;
    private FragmentPagerAdapter adapter;
    private View viewMask;
    private OnProgramDataChangedListener onProgramDataChangedListener;

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener)
    {
        this.onPageChangeListener = onPageChangeListener;
    }

    private ViewPager.OnPageChangeListener onPageChangeListener;

    public OnProgramDataChangedListener getOnProgramDataChangedListener()
    {
        return onProgramDataChangedListener;
    }

    public void setOnProgramDataChangedListener(OnProgramDataChangedListener onProgramDataChangedListener)
    {
        this.onProgramDataChangedListener = onProgramDataChangedListener;
    }


    public static Fragment newInstance()
    {
        TabFragment f = new TabFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.program_table_tab, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        //adapter
        final LinkedList<BaseFragment> fragments = getFragments();
        adapter = new TabFragmentPagerAdapter(getFragmentManager(), fragments);

        //pager
        pager = (CustomViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        //pager.addOnPageChangeListener(onPageChangeListener);

        //tabs
        tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer()
        {

            @Override
            public int getIndicatorColor(int position)
            {
                return fragments.get(position).getIndicatorColor();
            }

            @Override
            public int getDividerColor(int position)
            {
                return fragments.get(position).getDividerColor();
            }
        });

        tabs.setBackgroundResource(R.color.md_blue_500);
        tabs.setCustomTabView(R.layout.table_title, R.id.txtTabTitle);
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(onPageChangeListener);

        viewMask = view.findViewById(R.id.view_mask);
        viewMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private LinkedList<BaseFragment> getFragments()
    {
        //int indicatorColor = Color.parseColor(this.getResources().getString(R.color.color_accent));
        int indicatorColor = R.color.md_red_500;
        int dividerColor = Color.TRANSPARENT;

        LinkedList<BaseFragment> fragments = new LinkedList<BaseFragment>();
        fragments.add(AfFragment.newInstance(0,"AF", indicatorColor, dividerColor,onProgramDataChangedListener));
        fragments.add(SfFragment.newInstance(1,"SF", indicatorColor, dividerColor,onProgramDataChangedListener));
        fragments.add(LntFragment.newInstance(2,"LNT", indicatorColor, dividerColor,onProgramDataChangedListener));
        fragments.add(ChFragment.newInstance(3,"CH", indicatorColor, dividerColor,onProgramDataChangedListener));
        return fragments;
    }

    public void setCurrentIndex(final int index)
    {
        if(pager == null)
        {
            return ;
        }

        pager.setCurrentItem(index,false);
        tabs.setViewPager(pager);
        adapter.notifyDataSetChanged();
    }

    public int getCurrentIndex()
    {
        if(pager == null)
        {
            return -1;
        }

        return pager.getCurrentItem();
    }

    public FragmentPagerAdapter getTabFragmentAdapter()
    {
        return adapter;
    }

    public SlidingTabLayout getTab()
    {
        return tabs;
    }

    public CustomViewPager getPager()
    {
        return pager;
    }

    public void setMaskVisible(final boolean visible)
    {
        viewMask.setVisibility(visible?View.VISIBLE:View.GONE);
    }

}
