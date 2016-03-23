package com.uacs.notificationmanager;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.RelativeLayout;

public class ChartsFragment extends Fragment {
	private final String TAG = "Notification";
	private WebView webView;
	private Map<String, Integer> notificationPerDay = new HashMap<String, Integer>();
	private int appCount = 0;
	//private TreeMap<String, Integer> sortedNotificationPerDay;
	private int passedDays = 1;
	private Map<String, Integer> sortedNotificationPerDay =notificationPerDay;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.charts_fragment, container, false);
		webView = (WebView) view.findViewById(R.id.web);
		Context context = this.getActivity().getApplicationContext();

		// get the history notifications
		List<StatusBarNotification> recentNotifications = NotificationHistoryHolder.getInstance()
				.getRecentNotifications();

		// derive the NotificationPerDay HashMap from recent Notification
		for (StatusBarNotification sbn : recentNotifications) {
			String applicationName = Utils.getApplicationName(context,sbn.getPackageName());
			Log.d("sbn", applicationName);

			if (notificationPerDay.containsKey(applicationName)) {
				int count = notificationPerDay.get(applicationName) + 1;
				notificationPerDay.put(applicationName, count);
			} else {
				notificationPerDay.put(applicationName, 1);
			}
		}
		
		appCount = notificationPerDay.size();
		Log.d("sbn", "total no. of app:"+ appCount);
		//debug
		if(appCount != 0) {
			float result = getCount(0);
			Log.d("sbn", "the first app post "+ result + "notification per day");
		}

		
		// sort NotificationPerDay
//		ValueComparator bvc = new ValueComparator(notificationPerDay);
//		sortedNotificationPerDay = new TreeMap<String, Integer>(bvc);
//		appCount = sortedNotificationPerDay.size();

		// get the passedDays
		SharedPreferences sp = getActivity().getSharedPreferences("default", 0);
		long startTime = sp.getLong("history_start_time", 0);
		long curTime = new Date().getTime();
		
		if (!(startTime == 0)) {
			passedDays = (int) Math.ceil((curTime - startTime) / (double) (24 * 60 * 60 * 1000));
		}
		Log.d("sbn", "passedDays:" + passedDays);

		webView.addJavascriptInterface(new WebAppInterface(), "Android");

		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl("file:///android_asset/chart.html");
		return view;
	}


	class ValueComparator implements Comparator {
		Map<String, Integer> base;

		public ValueComparator(Map<String, Integer> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with
		// equals.
		public int compare(Object a, Object b) {
			if (base.get(a) >= base.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	}

	public class WebAppInterface {

		@JavascriptInterface
		public int getAppCount() {
			Log.d("sbn", "javascript called getAppCount, return :" + appCount);
			return appCount;			
		}

		@JavascriptInterface
		public String getName(int i) {
			String result="";
			if (sortedNotificationPerDay != null) {
				result = (String) sortedNotificationPerDay.keySet().toArray()[i];				
			}
			Log.d("sbn", "javascript called getName, return :" + result);
			return result;
		}

		@JavascriptInterface
		public float getCount(int i) {
			Log.d("sbn", "javascript called getCount, started....");
			float result = 0.0f;
			if (sortedNotificationPerDay != null) {
				int totalCount = (Integer)sortedNotificationPerDay.values().toArray()[i];
				float count = totalCount / (float) passedDays;
			    BigDecimal b = new BigDecimal(count);  
				result = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
 			}
			Log.d("sbn", "javascript called getCount, return :" + result);
			return result;
		}
	}
	
	public float getCount(int i) {
		Log.d("sbn", "javascript called getCount, started....");
		float result = 0.0f;
		if (sortedNotificationPerDay != null) {
			int totalCount =(Integer) sortedNotificationPerDay.values().toArray()[i];
			float count = totalCount / (float) passedDays;
		    BigDecimal b = new BigDecimal(count);  
			result = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
			}
		Log.d("sbn", "javascript called getCount, return :" + result);
		return result;
	}

}
