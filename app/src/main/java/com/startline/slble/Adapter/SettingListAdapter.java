package com.startline.slble.Adapter;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.startline.slble.R;

import java.util.List;
import java.util.Map;

/**
 * Created by terry on 2016/5/9.
 */
public class SettingListAdapter extends BaseAdapter
{
    public static final int TYPE_GROUP = 0;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_CHECKBOX = 2;
    public static final int TYPE_COUNT = 3;

    private Context context;
    private boolean dataEnabled;

    private List<Map<String,Object>> dataList;
    private List<Integer> typeList;

    private View.OnClickListener onTitleClick = null;
    private View.OnClickListener onValueClick = null;
    private View.OnClickListener onButtonClick = null;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = null;

    public class SettingItem
    {
        public TextView title;
        public TextView description;
        public CheckBox checkBox;
        public TextView value;
    }

    public SettingListAdapter(final Context context, final List<Map<String,Object>> list, boolean enabled, final CompoundButton.OnCheckedChangeListener onCheckedChangeListener, final View.OnClickListener onValueClick)
    {
        this.context = context;
        dataList = list;
        dataEnabled = enabled;
        this.onButtonClick = onButtonClick;
        this.onCheckedChangeListener = onCheckedChangeListener;
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

    public void setTypeList(final List list)
    {
        typeList = list;
    }

    public void setDataEnabled(final boolean enabled)
    {
        dataEnabled = enabled;
    }

    @Override
    public int getItemViewType(int position)
    {
        if(typeList == null || typeList .size() == 0 || position > typeList.size())
            return TYPE_TEXT;
        else
        {
            return typeList.get(position);
        }
    }

    @Override
    public int getViewTypeCount()
    {
        return TYPE_COUNT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        SettingItem holder;
        Map<String,Object> item;
        int type = getItemViewType(position);
        if (convertView == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            holder = new SettingItem();
            switch (type)
            {
                case TYPE_GROUP:
                    convertView = layoutInflater.inflate(R.layout.list_item_group, null);
                    holder.title = (TextView)convertView.findViewById(R.id.txt_title);
                break;

                case TYPE_CHECKBOX:
                case TYPE_TEXT:
                    convertView = layoutInflater.inflate(R.layout.list_item_setting, null);
                    holder.title = (TextView)convertView.findViewById(R.id.txt_title);
                    holder.description = (TextView)convertView.findViewById(R.id.txt_description);
                    holder.value = (TextView)convertView.findViewById(R.id.txt_value);
                    holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
                    break;
            }
            convertView.setTag(holder);
        }
        else
        {
            holder = (SettingItem)convertView.getTag();
        }

        item = (Map<String, Object>) getItem(position);

        holder.title.setText((String)item.get("title"));
        holder.title.setEnabled(dataEnabled);

        if(onTitleClick != null)
            holder.title.setOnClickListener(onTitleClick);

        if(type != TYPE_GROUP)
        {
            holder.description.setText((String)item.get("description"));
            holder.description.setEnabled(dataEnabled);

            if(type == TYPE_TEXT)
            {
                String value = (int)item.get("value")+"";
                holder.value.setText(value);
                holder.value.setEnabled(dataEnabled);
                holder.checkBox.setVisibility(View.INVISIBLE);
                if(onValueClick != null)
                {
                    holder.value.setOnClickListener(onValueClick);
                }
            }
            else if(type == TYPE_CHECKBOX)
            {
                holder.checkBox.setTag(holder.title.getText().toString());
                holder.value.setVisibility(View.INVISIBLE);
                int value = (int)item.get("value");
                if(value == 0)
                    holder.checkBox.setChecked(false);
                else
                    holder.checkBox.setChecked(true);

                holder.checkBox.setEnabled(dataEnabled);
                if(onButtonClick != null)
                    holder.checkBox.setOnClickListener(onButtonClick);
                if(onCheckedChangeListener != null)
                    holder.checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            }
        }

        return convertView;
    }
}
