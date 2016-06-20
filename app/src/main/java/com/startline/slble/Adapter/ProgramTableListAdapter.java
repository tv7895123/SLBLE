package com.startline.slble.Adapter;

import android.content.Context;
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
public class ProgramTableListAdapter extends BaseAdapter
{
    public static final int TYPE_GROUP = 0;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_CHECKBOX = 2;
    public static final int TYPE_RADIO = 3;
    public static final int TYPE_COUNT =43;

    private Context context;
    private boolean dataEnabled;

    private List<Map<String,Object>> dataList;
    private List<Integer> typeList;

    private View.OnClickListener onTitleClick = null;
    private View.OnClickListener onValueClick = null;
    private View.OnClickListener onButtonClick = null;
    private RadioGroup.OnCheckedChangeListener onCheckedChangeListener = null;

    public class SettingItem
    {
        public TextView index;
        public TextView title;
        public TextView initValue;
        public TextView value;
        public RadioGroup radioGroup;
    }

    public ProgramTableListAdapter(final Context context, final List<Map<String,Object>> list, boolean enabled, final RadioGroup.OnCheckedChangeListener onCheckedChangeListener, final View.OnClickListener onValueClick)
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
                case TYPE_RADIO:
                {
                    convertView = layoutInflater.inflate(R.layout.list_item_program_radio, null);
                    holder.title = (TextView)convertView.findViewById(R.id.txt_title);
                    holder.initValue = (TextView)convertView.findViewById(R.id.txt_init_value);
                    holder.value = (TextView)convertView.findViewById(R.id.txt_value);
                    holder.index = (TextView)convertView.findViewById(R.id.txt_index);
                    holder.radioGroup = (RadioGroup) convertView.findViewById(R.id.radio_group);
                }
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
            if(type == TYPE_TEXT)
            {
                String index = (String)item.get("index");
                holder.index.setText(index);

                String value = (String)item.get("value");
                holder.value.setTag(holder.title.getText().toString());
                holder.value.setText(value);
                holder.value.setEnabled(dataEnabled);
                if(onValueClick != null)
                {
                    holder.value.setOnClickListener(onValueClick);
                }
            }
            else if(type == TYPE_RADIO)
            {
                int index = (int)item.get("index");
                int initValue = (int)item.get("init_value");
                int modifiedValue = (int)item.get("modified_value");
                String[] itemArray = (String[])item.get("items");
                boolean expand = (boolean)item.get("expand");

                holder.index.setText(String.format("%2d",index)+".");
                holder.radioGroup.removeAllViews();
                holder.value.setText("");
                holder.initValue.setText("");

                if(initValue == modifiedValue)
                {
                    holder.initValue.setText(itemArray[modifiedValue]);
                    holder.value.setVisibility(View.GONE);

                }
                else
                {
                    holder.initValue.setText(String.format("%s --> ",itemArray[initValue]));
                    holder.value.setText(itemArray[modifiedValue]);
                    holder.value.setVisibility(View.VISIBLE);
                }

                if(expand)
                {
                    RadioButton radioButton;
                    for (int i = 0; i < itemArray.length; i++)
                    {
                        radioButton = new RadioButton(context);
                        radioButton.setFocusable(false);
                        radioButton.setButtonDrawable(android.R.drawable.btn_radio);
                        radioButton.setId((index * 100) + i);
                        radioButton.setText(itemArray[i]);
                        if(i == modifiedValue)
                        {
                            radioButton.setChecked(true);
                        }
                        else
                        {
                            radioButton.setChecked(false);
                        }


                        holder.radioGroup.addView(radioButton);
                    }

                    holder.radioGroup.setOnCheckedChangeListener(onCheckedChangeListener);
                }

            }
        }
        else
        {

        }
        return convertView;
    }
}
