package com.example.veniceexplorer;

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.view.LayoutInflater;

	public class ProjectsAdapter extends BaseAdapter {
    private LayoutInflater mInflater;

    public ProjectsAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return 0;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
        	/*
            convertView = mInflater.inflate(R.layout.row, null);
            holder = new ViewHolder();
            holder.text1 = (TextView) convertView
                    .findViewById(R.id.TextView01);
            holder.text2 = (TextView) convertView
                    .findViewById(R.id.TextView02);

            convertView.setTag(holder);
            */
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //holder.text1.setText(CountriesList.abbreviations[position]);
        //holder.text2.setText(CountriesList.countries[position]);

        return convertView;
    }

    static class ViewHolder {
        TextView text1;
        TextView text2;
    }
}