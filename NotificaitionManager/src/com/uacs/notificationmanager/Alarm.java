package com.uacs.notificationmanager;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

public class Alarm extends BroadcastReceiver 
{    
	private static boolean needToSendLog = false;
	private final static String TAG = "Notification";
	private static boolean isSet = false;
	//private final static String TAG = "alarm";

    @Override
    public void onReceive(Context context, Intent intent) 
    {   
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        // Put here YOUR code.
		Log.d(TAG, "alarm triggered");
		
				
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
	        
	        needToSendLog = false;
		} else {
			Log.d(TAG, "no network, postpone uploading..");
			needToSendLog = true;
		}
		
	
        wl.release();
    }

    public void SetAlarm(Context context)
    {
    	if(isSet){
    		return;
    	}
    	else{
    		isSet = true;
    	}
    	Calendar calendar = Calendar.getInstance();
    	//calendar.setTimeInMillis(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    	calendar.setTimeInMillis(System.currentTimeMillis());
    	//calendar.setTimeInMillis(System.currentTimeMillis() +  60*1000);
    	calendar.set(Calendar.HOUR_OF_DAY, 00);
    	calendar.set(Calendar.MINUTE,new Random().nextInt(60));
    	//calendar.set(Calendar.MINUTE, 52);
    	
    	if(Calendar.getInstance().after(calendar)){
    		calendar.add(Calendar.DATE, 1);
    		
    	}
    	
    	
    	//debug 
    	//calendar.setTimeInMillis(System.currentTimeMillis() + 10*1000); // triggered after 10 seconds
    	
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
       // Intent i = new Intent(context, Alarm.class);
        Intent i = new Intent("com.uacs.notificationmanager.ACTION_UPLOAD");
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi); // Millisec * Second * Minute
        //for debug
        //am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 30*1000, pi); // Millisec * Second * Minute
        Log.d(TAG, "alarm set");
        
       
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        isSet = false;
    }
    
    public boolean needToSend(){
    	return needToSendLog;
    }
    
    public void setNeedToSend(boolean needToSend){
    	needToSendLog = needToSend;
    }
    
}