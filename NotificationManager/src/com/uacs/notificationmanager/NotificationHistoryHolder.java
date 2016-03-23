package com.uacs.notificationmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationHistoryHolder {
	  private static final String TAG="Notification";
      private List<StatusBarNotification> recentNotifications = new ArrayList<StatusBarNotification> ();
      private Map<String, Long> uncheckedDurationMap = new HashMap<String, Long>();
      private Map<String, Integer> checkedTimesMap = new HashMap<String, Integer>();
	 
      public List<StatusBarNotification> getRecentNotifications() {
		  return recentNotifications;
	  }
	  
	  public void setRecentNotifications(List<StatusBarNotification> recentNotifications) {
		  this.recentNotifications = recentNotifications;
	  }
	  
      public Map<String, Long> getUncheckedDurationMap() {
		  return uncheckedDurationMap;
	  }
	  
	  public void addToUncheckdDurationMap(String packageName, long duration) {		  
		  if(checkedTimesMap.containsKey(packageName)){			  
			  int oldTimes =checkedTimesMap.get(packageName) ;
			  if(uncheckedDurationMap.containsKey(packageName)) {
				  Long oldDuration = uncheckedDurationMap.get(packageName);
				  //average
				  Long newDuartion = (oldDuration * oldTimes + duration)/(oldTimes + 1);
				  uncheckedDurationMap.put(packageName, newDuartion);
			  }else {
				  Log.d(TAG, "inconsistent unchecked duration map!");
			  }
			  
			  checkedTimesMap.put(packageName, oldTimes + 1);
		  }else {
			  
		  }
	  }


	  private static final NotificationHistoryHolder holder = new NotificationHistoryHolder();
	  public static NotificationHistoryHolder getInstance() {return holder;}
}
