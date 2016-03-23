package com.uacs.notificationmanager;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class HistoryFragment extends Fragment {
	private ListView list;
	MyAdapter adapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.history_fragment, container, false);  
		list=(ListView)view.findViewById(R.id.list);
		Context context = this.getActivity().getApplicationContext();

		// get the history notifications
		List<StatusBarNotification> recentNotifications = NotificationHistoryHolder.getInstance()
				.getRecentNotifications();
		
        adapter=new MyAdapter(context,recentNotifications);
        list.setAdapter(adapter);
 
        // Click event 
        list.setOnItemClickListener(new OnItemClickListener() {
 
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
            }
			});
        
		return view;
	}
}
