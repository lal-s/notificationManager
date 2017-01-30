package com.uacs.notificationmanager;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;

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
	private Map<String, ArrayList<Integer>> notificationPerDay = new HashMap<String, ArrayList<Integer>>();
	private int appCount = 0;
	//private TreeMap<String, Integer> sortedNotificationPerDay;
	private int passedDays = 1;
	private Map<String, ArrayList<Integer>> sortedNotificationPerDay =notificationPerDay;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.charts_fragment, container, false);
		webView = (WebView) view.findViewById(R.id.web);
		Context context = this.getActivity().getApplicationContext();
		
		
		//--------
		
//		 File[] files= Utils.getFilesToUpload();
//		 new LoggerTask().execute(files[0].getPath(), "16f9a13b67322955f28248af218b5095");
		
		
		
	
//		 File[] files= Utils.getFilesToUpload();
//	        if(files !=null) {
//	        	for(File file : files) {
//	        		new LoggerTask().execute(files[0].getPath(), "16f9a13b67322955f28248af218b5095");
//	        	}
//	        }
	        
	        
	        
	
	     //File testFile = new File("2016-03-31.txt");
//		 File testFile= new File(files[0].getPath());
//		 
//		 try {
//			ServerMiddleware.sendMultiDataToServer("16f9a13b67322955f28248af218b5095", testFile);
//		}
//		catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		 
		 //-----

		// get the history notifications
		List<StatusBarNotification> recentNotifications = NotificationHistoryHolder.getInstance()
				.getRecentNotifications();
		
		List<StatusBarNotification> suppressedNotifications = NotificationHistoryHolder.getInstance()
				.getSuppressedNotifications();
		
		

		// derive the NotificationPerDay HashMap from recent Notification
		for (StatusBarNotification sbn : recentNotifications) {
			String applicationName = Utils.getApplicationName(context,sbn.getPackageName());
			Log.d("sbn", applicationName);

			if (notificationPerDay.containsKey(applicationName)) {
				ArrayList<Integer> count = notificationPerDay.get(applicationName);
				count.set(0, count.get(0)+1);
				notificationPerDay.put(applicationName, count);
			} else {
				ArrayList<Integer> count = new ArrayList<Integer>();
				count.add(1);
				count.add(0);
				notificationPerDay.put(applicationName, count);
			}
		}
		
		for (StatusBarNotification sbn : suppressedNotifications) {
			String applicationName = Utils.getApplicationName(context,sbn.getPackageName());
			Log.d("sbn", applicationName);

			if (notificationPerDay.containsKey(applicationName)) {
				ArrayList<Integer> count = notificationPerDay.get(applicationName);
				count.set(1, count.get(1)+1);
				notificationPerDay.put(applicationName, count);
			} else {
				ArrayList<Integer> count = new ArrayList<Integer>();
				count.set(1, 1);
				notificationPerDay.put(applicationName, count);
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
			if (notificationPerDay != null) {
				result = (String) notificationPerDay.keySet().toArray()[i];				
			}
			Log.d("sbn", "javascript called getName, return :" + result);
			return result;
		}

		@JavascriptInterface
		public float getCount(int i) {
			Log.d("sbn", "javascript called getCount, started....");
			float result = 0.0f;
			if (notificationPerDay != null) {
				ArrayList<Integer> totalCount = (ArrayList<Integer>) notificationPerDay.values().toArray()[i];
				float count = totalCount.get(0) / (float) passedDays;
			    BigDecimal b = new BigDecimal(count);  
				result = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
 			}
			Log.d("sbn", "javascript called getCount, return :" + result);
			return result;
		}
		
		@JavascriptInterface
		public float getSuppressedCount(int i) {
			Log.d("sbn", "javascript called getCount, started....");
			float result = 0.0f;
			if (sortedNotificationPerDay != null) {
				ArrayList<Integer> totalCount = (ArrayList<Integer>) notificationPerDay.values().toArray()[i];
				float count = totalCount.get(1) / (float) passedDays;
			    BigDecimal b = new BigDecimal(count);  
				result = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
 			}
			Log.d("sbn", "javascript called getCount, return :" + result);
			return result;
		}
		
		@JavascriptInterface
		public boolean isSmart(){
			SharedPreferences sp = getActivity().getSharedPreferences("default", 0);
			return sp.getBoolean("smart", false);
		}
	}
	
	
	public float getCount(int i) {
		Log.d("sbn", "javascript called getCount, started....");
		float result = 0.0f;
		if (notificationPerDay != null) {
			ArrayList<Integer> totalCount = (ArrayList<Integer>) notificationPerDay.values().toArray()[i];
			float count = totalCount.get(0) / (float) passedDays;
		    BigDecimal b = new BigDecimal(count);  
			result = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
			}
		Log.d("sbn", "javascript called getCount, return :" + result);
		return result;
	}
	
	/*public float getCount(int i) {
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
	} */

}
