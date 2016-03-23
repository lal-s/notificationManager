package com.uacs.notificationmanager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotificationService extends NotificationListenerService {
	Context context;
	private final static String TAG = "Notification";
	private final String PACKAGE_NAME = "com.uacs.notificationmanager";
	private final static long COUNT_DOWN_TIME = 20 * 1000; //If user doesn't check notification in 5 minutes, shut down the radio
	private final static long COUNT_DOWN_INTERVAL = 1 * 1000; // change to 1 minute
	private final static long RECENT_HISTORY_THRESH = 7 * 24 * 60 * 60 * 1000; // only keep track of the notifications in the passed week 
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private NotificationManager mNM = null;
	private int NOTIFICATION_ID = 10082;
	private BroadcastReceiver mReceiver = null;
	private MyCountDownTimer timer = null;
	private List<StatusBarNotification> recentNotifications = new ArrayList<StatusBarNotification> ();
    private Alarm alarm = new Alarm();
    private List<StatusBarNotification> pendingNotifications = new ArrayList<StatusBarNotification> ();
	 

	@Override

	public void onCreate() {

		super.onCreate();
		context = getApplicationContext();
		Log.i(TAG, "NotificationService started");

		showNotification();
		
		SharedPreferences sp = getSharedPreferences(
				"default", 0);
		long startTime = sp.getLong("history_start_time", 0);
		if(startTime == 0) {
			Editor editor = sp.edit();
			editor.putLong("history_start_time", new Date().getTime());
			editor.commit();
		}

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		mReceiver = new ScreenOnOffReceiver();
		registerReceiver(mReceiver, filter);
		

	}

	@Override
    public void onStart(Intent intent, int startId) {
		        //set alarm
		        alarm.SetAlarm(this);
				boolean isScreenOn = false;

				try {
					isScreenOn = intent.getBooleanExtra("screen_state", false);

				} catch (Exception e) {
				}

				if (!isScreenOn) {
					Date date = new Date();
					long milliseconds = date.getTime();
					String line = milliseconds + "|ScreenOff\n";
					Utils.writeToLog(line);
					Log.d(TAG, "Screen off");

				} else {
					Date date = new Date();
					long milliseconds = date.getTime();
					String line = milliseconds + "|ScreenOn\n";
					Utils.writeToLog(line);
					Log.d(TAG, "Screen on");
					
					//smart notification processing
					SharedPreferences sp = getSharedPreferences(
							"default", 0);
					boolean isSmart = sp.getBoolean("smart", false);
					boolean isBlocking = sp.getBoolean("isblocking", false);
					if(isSmart) {
						if(isBlocking) {
							Log.d(TAG, "recover internet status");
							boolean wifiStatus = sp.getBoolean("wifi", false);
							boolean dataStatus = sp.getBoolean("data", false);

							if(wifiStatus) {
								Log.d(TAG, "turn on wifi");
								Utils.setWifi(context, true);
							}
							if(dataStatus) {
								Log.d(TAG, "turn on data");
								Utils.setData(context, true);
							}
							
							Editor editor = sp.edit();
							editor.putBoolean("isblocking", false);
							editor.commit();
						} else {
							if(timer != null) {
								Log.d(TAG, "cancel the timer");
								timer.cancel();
								timer = null;
							}
						}
					}
				}
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		 //android.os.Debug.waitForDebugger();

		int id = sbn.getId();
		String pack = sbn.getPackageName();
		long postTime = sbn.getPostTime();
		Date date = new Date(postTime);
		String postTimeFormatted = formatter.format(date);
		boolean isClearable = sbn.isClearable();
		if (isClearable && !pack.startsWith("com.android") && !pack.trim().equalsIgnoreCase("android")) {
			
			String line = postTime + "|post|" + id + "|" + pack + "|\n";
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// boolean isIdle = pm.isDeviceIdleMode();
			boolean isScreenOn = pm.isScreenOn();
			Utils.writeToLog(line);
            
			//add to recent notifications
			addToRecentNotifications(sbn);
			
			//add to pending notifications
			//addToPendingNotifications(sbn);
			
			Log.i(TAG, "Notification Posted");
			// Log.i(TAG, "is idle:" + isIdle);
			Log.i(TAG, "is screen on:" + isScreenOn);
			Log.i(TAG, "id:" + id);
			Log.i(TAG, "Package: " + pack);
			Log.i(TAG, "postTime:" + postTimeFormatted);
			Log.i(TAG, "is Clearable:" + sbn.isClearable());

			// print some additional info for debug
			// String ticker = sbn.getNotification().tickerText.toString();
			// Bundle extras = sbn.getNotification().extras;
			// String title = extras.getString("android.title");
			// String text = extras.getCharSequence("android.text").toString();
			// Log.i(TAG, "Ticker: " + ticker);
			// Log.i(TAG, "Title: " + title);
			// Log.i(TAG, "Text: " + text);
			// Log.i(TAG, "\n");
			
			
			//smart notification processing
			SharedPreferences sp = getSharedPreferences(
					"default", 0);
			boolean isSmart = sp.getBoolean("smart", false);
			boolean isBlocking = sp.getBoolean("isblocking", false);
			if(isSmart) {
				if(!isBlocking && !isScreenOn) {
					new TimerThread().start();
				}
			}
		}

	}

	@Override

	public void onNotificationRemoved(StatusBarNotification sbn) {
		int id = sbn.getId();
		String pack = sbn.getPackageName();
		long postTime = sbn.getPostTime();
		Date date = new Date(postTime);
		Date curDate = new Date();
		long removeTime = curDate.getTime();
		String postTimeFormatted = formatter.format(date);
		boolean isClearable = sbn.isClearable();

		if (isClearable && !pack.startsWith("com.android") && !pack.trim().equalsIgnoreCase("android")) {
			String line = removeTime + "|remove|" + id + "|" + pack + "\n";
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// boolean isIdle = pm.isDeviceIdleMode();
			boolean isScreenOn = pm.isScreenOn();

			Utils.writeToLog(line);
			Log.i(TAG, "Notification Removed");
			// Log.i(TAG, "is idle:" + isIdle);
			Log.i(TAG, "is screen on:" + isScreenOn);
			Log.i(TAG, "id:" + id);
			Log.i(TAG, "Package: " + pack);
			Log.i(TAG, "postTime:" + postTimeFormatted);
			Log.i(TAG, "is Clearable:" + sbn.isClearable());
			
			//get unchecked duration and update the pending interests
			//getDuration(id, removeTime);

			// print some additional info for debug
			// String ticker = sbn.getNotification().tickerText.toString();
			// Bundle extras = sbn.getNotification().extras;
			// String title = extras.getString("android.title");
			// String text = extras.getCharSequence("android.text").toString();
			// Log.i(TAG, "Ticker: " + ticker);
			// Log.i(TAG, "Title: " + title);
			// Log.i(TAG, "Text: " + text);
			// Log.i(TAG, "\n");
		}

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (mReceiver != null)
			unregisterReceiver(mReceiver);
		if (mNM != null)
			mNM.cancel(NOTIFICATION_ID);
		Log.d(TAG, "Service destroyed");
	}

	private void showNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher_1)
				.setContentTitle("Smart Notification").setContentText("Smart Notification is running").setOngoing(true);

		Intent targetIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentIntent);
		NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		//nManager.notify(NOTIFICATION_ID, builder.build());
		nManager.notify(NOTIFICATION_ID, builder.getNotification());
	}
	
	
	private void addToPendingNotifications(StatusBarNotification sbn){
		int curNotificationId = sbn.getId();
		boolean isFound = false;
		for(StatusBarNotification n : pendingNotifications) {
			if(n.getId() == curNotificationId) {
				isFound = true;
			}
		}
		if(!isFound) {
			pendingNotifications.add(sbn);
		}
	}
	
	private long getDuration(int notificationId, long removeTime) {
		long duration = 0;
		for(StatusBarNotification n: pendingNotifications) {
			
		}
		
		return duration;
		
	}
	private void addToRecentNotifications(StatusBarNotification sbn) {
		
		SharedPreferences sp = getSharedPreferences(
				"default", 0);
		long startTime = sp.getLong("history_start_time", 0);
		if(startTime == 0) {
			Editor editor = sp.edit();
			long curTime = new Date().getTime();
			editor.putLong("history_start_time", curTime);
			editor.commit();
			startTime = curTime;
		}
		long postTime = sbn.getPostTime();
		if(postTime - startTime > RECENT_HISTORY_THRESH) {
			// kick out old notifications
			Log.d(TAG, "kicking out old notifications...");
			Iterator<StatusBarNotification> i = recentNotifications.iterator();
			while (i.hasNext()) {
				StatusBarNotification s = i.next(); 
				if(postTime - s.getPostTime() > RECENT_HISTORY_THRESH) {
					i.remove();
				} else {
					Editor editor2 = sp.edit();
					editor2.putLong("history_start_time", s.getPostTime());
					editor2.commit();
					break;
				}
				
			}
			
		} 
	    //add the new notification into list
		recentNotifications.add(sbn);
		NotificationHistoryHolder.getInstance().setRecentNotifications(recentNotifications);
		//for debug, remove later
		Log.d(TAG, "recent Notifications\n");
		outputRecentNotifications();
    	Log.d(TAG, "\n");

		
	}
	
	public List<StatusBarNotification> getRecentNotifications( ) {
		return  recentNotifications;
	}
	
	public void outputRecentNotifications( ) {
        for(StatusBarNotification sbn:recentNotifications) {
        	Log.d(TAG, formatter.format(new Date(sbn.getPostTime())) + "|" + sbn.getPackageName());
        }
	}
	
	
	class TimerThread extends Thread {
	    
	    @Override
	    public void run() {
	        Looper.prepare();
			Log.d(TAG, "start timer....");
			timer = new MyCountDownTimer(COUNT_DOWN_TIME, COUNT_DOWN_INTERVAL);         
			timer.start();
			Looper.loop();
	    }

	}
	
	public class MyCountDownTimer extends CountDownTimer
	{

		public MyCountDownTimer(long startTime, long interval)
			{
				super(startTime, interval);
			}

		@Override
		public void onFinish()
			{
			
				SharedPreferences sp = getSharedPreferences(
					"default", 0);
				Editor editor = sp.edit();
				Log.d(TAG, "time's up. Shutting down the radio....");
				//Check wifi status				
				boolean isWifiOn = Utils.isWifiOn(context);
				
				//Shut down wifi
				if(isWifiOn) {
					editor.putBoolean("wifi", true);
					Log.d(TAG, "wifi is on: turning off");
					Utils.setWifi(context, false);
				}
				
				//Check data connection status
				boolean isDataOn = Utils.isDataOn(context);

				//Shut down data
				if(isDataOn) {
					 editor.putBoolean("data", true);
					 Log.d(TAG, "data is on: turning off");
					 Utils.setData(context,false); // turning off data is not working on android 5
				}

				//set isBlocking to true.			
				editor.putBoolean("isblocking", true);
				editor.commit();
			}

		@Override
		public void onTick(long millisUntilFinished) {
				long timeElapsed = COUNT_DOWN_TIME - millisUntilFinished;
				Log.d(TAG, "timer: " + String.valueOf(timeElapsed) + " milliseconds elapsed.");
		}
				
	}
	
	Handler mHandler=new Handler();

    public void runa() throws Exception{
        mHandler.post(new Runnable(){
            public void run(){
            	
            }
        });
    }    
}
