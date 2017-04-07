package com.startline.slble.Adapter;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.startline.slble.R;
import com.startline.slble.Util.DelegateUtil;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 2017/04/06
 * Time: ?? 2:37
 * To change this template use File | Settings | File Templates.
 */
public class AutoStartSettingAdapter extends BaseAdapter
{
	private Context mContext;
	private List<Map<String,String>> dataList;
	private boolean dataEnabled;

	private View.OnClickListener onTitleClick = null;
	private View.OnClickListener onValueClick = null;

	public class SettingItem
    {
		public TextView title;
        public TextView value;
    }

	public AutoStartSettingAdapter(Context mContext, List<Map<String,String>> list, boolean enabled, final View.OnClickListener onValueClick)
    {
        this.mContext = mContext;
        dataList = list;
        dataEnabled = enabled;
		this.onValueClick = onValueClick;
    }

	@Override
	public int getCount()
	{
		return dataList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	public void setDataList(final List list)
	{
		dataList = list;
	}

	public void setDataEnabled(final boolean dataEnabled)
	{
		this.dataEnabled = dataEnabled;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
        SettingItem holder;
        Map<String,String> item;
		int type = getItemViewType(position);
		if (convertView == null)
		{
			LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			holder = new SettingItem();
			convertView = layoutInflater.inflate(R.layout.list_item_auto_start, null);
			holder.title = (TextView)convertView.findViewById(R.id.txt_title);
			holder.value = (TextView)convertView.findViewById(R.id.txt_value);
			convertView.setTag(holder);
		}
		else
		{
			holder = (SettingItem)convertView.getTag();
		}

        item = (Map<String, String>) getItem(position);

        holder.title.setText((String)item.get("title"));
        holder.title.setEnabled(dataEnabled);

		if(onTitleClick != null)
			holder.title.setOnClickListener(onTitleClick);

		String value = item.get("value");
		String unit =  "";//item.get("unit");

		holder.value.setText(value + " " +unit);
		holder.value.setEnabled(dataEnabled);

		if(onValueClick != null)
		{
			DelegateUtil.adjustTouchDelegate(holder.value, new Rect(100, 10, 200, 10));
			holder.value.setOnClickListener(onValueClick);
		}

		return convertView;
	}
}
