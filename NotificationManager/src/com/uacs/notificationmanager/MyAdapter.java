package com.uacs.notificationmanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
 
public class MyAdapter extends BaseAdapter {
 
    private Context context;
    private List<StatusBarNotification> notificationList;
    private static LayoutInflater inflater=null;
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MyAdapter(Context context, List<StatusBarNotification> list) {
        this.context = context;
        notificationList = list;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() {
        return notificationList.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row, null);
 
        TextView tvAppName = (TextView)vi.findViewById(R.id.tv_app_name);
        TextView tvTicker = (TextView)vi.findViewById(R.id.tv_ticker);
        TextView tvTitle= (TextView)vi.findViewById(R.id.tv_title);
        TextView tvPostTime= (TextView)vi.findViewById(R.id.tv_posttime);
        ImageView imAppIcon=(ImageView)vi.findViewById(R.id.app_icon); 
 
        StatusBarNotification sbn = notificationList.get(notificationList.size() - 1 - position);
        String packageName = sbn.getPackageName();
        //get application name
        String appName = Utils.getApplicationName(context, packageName);
        
        //get application icon
        Drawable appIcon = Utils.getApplicationIcon(context, packageName);
     
        //get ticker, title, post time
        CharSequence tickerCs = sbn.getNotification().tickerText;
        String ticker ="";
        if(tickerCs != null) {
        	ticker = tickerCs.toString();
        }
      
        long postTime = sbn.getPostTime();
        Bundle extras = sbn.getNotification().extras;
        String title="";
        if(extras != null) {
            title = extras.getString("android.title");
        }
        
        
        // Setting all values in listview
        tvAppName.setText(appName);
        if(appIcon != null) {
        	imAppIcon.setImageDrawable(appIcon);
        }
        tvTicker.setText(ticker);
        tvTitle.setText(title);
        Date date = new Date(postTime);
        tvPostTime.setText(formatter.format(date));
        return vi;
    }
}
