package com.stringee.kit.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.stringee.apptoappcallsample.R;

import java.util.List;

/**
 * Created by reytum on 18/3/16.
 */
public class StringeeMultimediaPopupAdapter extends BaseAdapter {
    Context context;
    List<String> multimediaIcons;
    List<String> multimediaText;
    Typeface iconTypeface;

    public StringeeMultimediaPopupAdapter(Context context, List<String> multimediaIcons, List<String> multimediaText) {
        this.context = context;
        this.multimediaIcons = multimediaIcons;
        this.multimediaText = multimediaText;
        iconTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/fontawesome-webfont.ttf");
    }

    @Override
    public int getCount() {
        return multimediaText.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.stringee_multimedia_option_item, null);

        TextView icon = (TextView) convertView.findViewById(R.id.stringee_multimedia_icon);
        icon.setTypeface(iconTypeface);
        TextView text = (TextView) convertView.findViewById(R.id.stringee_multimedia_text);
        icon.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        icon.setText(multimediaIcons.get(position));
        text.setText(multimediaText.get(position));
        return convertView;
    }

}
