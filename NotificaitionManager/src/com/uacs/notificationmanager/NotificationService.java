package com.uacs.notificationmanager;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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
	private final static long WIFI_INTERVAL = 60*1000;
	private final static long COUNT_DOWN_TIME = 2*60*1000;//5 * 60 * 1000; //If user doesn't check notification in 5 minutes, shut down the radio
	private final static long COUNT_DOWN_INTERVAL = 1*60*1000;//1 * 60 * 1000; // change to 1 minute
	private final static long RECENT_HISTORY_THRESH = 7 * 24 * 60 * 60 * 1000; // only keep track of the notifications in the passed week 
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private NotificationManager mNM = null;
	private int NOTIFICATION_ID = 10082;
	private BroadcastReceiver mReceiver = null;
	private MyCountDownTimer timer = null;
	private List<StatusBarNotification> recentNotifications = new ArrayList<StatusBarNotification> ();
	private List<StatusBarNotification> suppressedNotifications= new ArrayList<StatusBarNotification>();
    private Alarm alarm = new Alarm();
    private List<StatusBarNotification> pendingNotifications = new ArrayList<StatusBarNotification> ();
    
    private boolean wifiFlag_off=false;
    private boolean predictOff = false;
    
	Date wifion_date=new Date();
	long wifion_time=wifion_date.getTime();
	 

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
		
		alarm.SetAlarm(this);

	}

	@Override
    public void onStart(Intent intent, int startId) {
		
			
		        //set alarm
		        alarm.SetAlarm(this);
				boolean isScreenOn = false;
				
				
				//Check if last alarm failed to send log.
				if(alarm.needToSend()){
			        //Try to send logs
					if(Utils.isNetworkAvailable(context)) {//check if there is internet connection		
						//get user hash id 
						String hashId = "unknown";	   
						try {
							hashId = Utils.getHashId(context);
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
				        File[] files= Utils.getFilesToUpload();
				        if(files !=null) {
				        	for(File file : files) {
				        		new LoggerTask().execute(file.getPath(), hashId);
				        	}
				        }
				        
				        alarm.setNeedToSend(false);
					} else {
						Log.d(TAG, "no network, postpone uploading..");
						alarm.setNeedToSend(true);
					}
				} 

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
								
								date = new Date();
								milliseconds = date.getTime();
							    line = milliseconds + "|WifiOn\n";
							    
							    wifion_time=milliseconds;
								Utils.writeToLog(line);
								Log.d(TAG, "WifiOn");
								
								//Check if last alarm failed to send log.
							/*	if(alarm.needToSend()){
							        //Try to send logs
									if(Utils.isNetworkAvailable(context)) {//check if there is internet connection		
										//get user hash id 
										String hashId = "unknown";	   
										try {
											hashId = Utils.getHashId(context);
										} catch (NoSuchAlgorithmException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (UnsupportedEncodingException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										
								        File[] files= Utils.getFilesToUpload();
								        if(files !=null) {
								        	for(File file : files) {
								        		new LoggerTask().execute(file.getPath(), hashId);
								        	}
								        }
								        
								        alarm.setNeedToSend(false);
									} else {
										Log.d(TAG, "no network, postpone uploading..");
										alarm.setNeedToSend(false);
									}
								} */ 
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
					else{ //isSmart == false
						if(predictOff){
							date = new Date();
							milliseconds = date.getTime();
						    line = milliseconds + "|PredictOn\n";
							Utils.writeToLog(line);
							Log.d(TAG, "PredictOn");
							predictOff = false;
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
			
			//String line = postTime + "|post|" + id + "|" + pack + "|\n";
			String line = postTimeFormatted + "|post|" + id + "|" + pack + "|\n";
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
			//if(isSmart) {
				if(!isBlocking && !isScreenOn) {
					new TimerThread().start();
				}
			//}
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
				.setContentTitle("Green Notification").setContentText("Smart Notification is running").setOngoing(true);

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
			
			Iterator<StatusBarNotification> i_suppressed = suppressedNotifications.iterator();
			while (i_suppressed.hasNext()) {
				StatusBarNotification s_suppressed = i_suppressed.next(); 
				if(postTime - s_suppressed.getPostTime() > RECENT_HISTORY_THRESH) {
					i_suppressed.remove();
				} else {
					Editor editor2 = sp.edit();
					editor2.putLong("history_start_time", s_suppressed.getPostTime());
					editor2.commit();
					break;
				}
				
			}
			
		} 
	    //add the new notification into list
		recentNotifications.add(sbn);
		NotificationHistoryHolder.getInstance().setRecentNotifications(recentNotifications);
			
		boolean isSmart = sp.getBoolean("smart", false);
		
		if(isSmart){
			if((postTime-wifion_time)<=WIFI_INTERVAL && wifiFlag_off)
			{
				suppressedNotifications.add(sbn);
				NotificationHistoryHolder.getInstance().setSuppressedtNotifications(suppressedNotifications);
				
			}
			//for debug, remove later
			Log.d(TAG, "recent Notifications\n");
			outputRecentNotifications();
	    	Log.d(TAG, "\n");
		}
		else{
			if(predictOff){
				suppressedNotifications.add(sbn);
				NotificationHistoryHolder.getInstance().setSuppressedtNotifications(suppressedNotifications);
			}
		}
		

		
	}
	
	public List<StatusBarNotification> getRecentNotifications( ) {
		return  recentNotifications;
	}
	
	public List<StatusBarNotification> getSuppressedNotifications() {
		return suppressedNotifications;
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
			
			
		/*	NotificationCompat.Builder mBuilder =new NotificationCompat.Builder(new NotificationService())
					.setSmallIcon(R.drawable.ic_launcher_1)
					.setContentTitle("My notification")
					.setContentText("Warning: Notifications will soon be suppressed. Touch on the screen to abort"); 


				int mNotificationId = 001;
				NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				// mId allows you to update the notification later on.
				mNotificationManager.notify(mNotificationId, mBuilder.getNotification());  */

			
			
				SharedPreferences sp = getSharedPreferences(
					"default", 0);
				
				boolean isSmart = sp.getBoolean("smart", false);
			    if (!isSmart){
			    	Date date = new Date();
					long milliseconds = date.getTime();
					String line = milliseconds + "|PredictOff\n";
					Utils.writeToLog(line);
					Log.d(TAG, "PredictOff");
					predictOff = true;
					return;
			    }
				
				Editor editor = sp.edit();
				Log.d(TAG, "time's up. Shutting down the radio....");
				//Check wifi status				
				boolean isWifiOn = Utils.isWifiOn(context);
				
				//Shut down wifi
				if(isWifiOn) {
					editor.putBoolean("wifi", true);
					Log.d(TAG, "wifi is on: turning off");
					Utils.setWifi(context, false);
					wifiFlag_off=true;
				}
				
				
				Date date = new Date();
				long milliseconds = date.getTime();
				String line = milliseconds + "|WifiOff\n";
				Utils.writeToLog(line);
				Log.d(TAG, "WifiOff");
				
				//Check data connection status
				boolean isDataOn = Utils.isDataOn(context);

				//set isBlocking to true.			
				editor.putBoolean("isblocking", true);
				editor.commit();
				
				//Shut down data
				if(isDataOn) {
					 editor.putBoolean("data", true);
					 Log.d(TAG, "data is on: turning off");
					// Utils.setData(context,false); // turning off data is not working on android 5
				}
				
				

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
