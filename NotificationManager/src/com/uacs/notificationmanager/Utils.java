package com.uacs.notificationmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Utils {
	private static String folderName = "myNotifications";
	private static String TAG = "Notification";

	
	/***
	 * get application name from package name
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static String getApplicationName(Context context, String packageName){
		PackageManager pm = context.getPackageManager();
		ApplicationInfo ai;
		try {
		    ai = pm.getApplicationInfo(packageName, 0);
		} catch (final NameNotFoundException e) {
		    ai = null;
		}
		String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
		return applicationName;		
	}
	
	/***
	 * get application icon from package name
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static Drawable getApplicationIcon(Context context, String packageName) {
		Drawable icon = null;
		try {
			icon = context.getPackageManager().getApplicationIcon(packageName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return icon;
	}
	
	/***
	 * write a line to the log file
	 * 
	 * @param line
	 */
	public static void writeToLog(String line) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + folderName);
		boolean success = true;
		if (!f.exists()) {
			success = f.mkdir();
		}
		if (success) {
			// Do something on success
			File fileDir = new File(f, dateFormat.format(date) + ".txt");
			try {
				FileOutputStream os = new FileOutputStream(fileDir, true);
				os.write(line.getBytes());
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * get all the log files to upload
	 * 
	 * @return
	 */
	public static File[] getFilesToUpload() {
		File[] files = null;
		File directory = new File(Environment.getExternalStorageDirectory() + File.separator + folderName);
		if (directory.exists()) {
			files = directory.listFiles();
		}
		return files;
	}

	/***
	 * delete all logs
	 */
	public static void deleteAllFiles() {
		File directory = new File(Environment.getExternalStorageDirectory() + File.separator + folderName);
		if (directory.exists()) {
			for (File file : directory.listFiles())
				file.delete();
		}
	}

	/**
	 * delete a single log
	 * 
	 * @param file:
	 *            the log to delete
	 */
	public static boolean deleteFile(File file) {
		return file.delete();
	}

	/**
	 * get hashed unique id of the device
	 * 
	 * @param context
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	public static String getHashId(Context context) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		return SHA1(androidId);

	}

	/***
	 * compute hash for text
	 * 
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}

	/**
	 * convert bytes to hex
	 * 
	 * @param data
	 * @return
	 */
	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9)) {
					buf.append((char) ('0' + halfbyte));
				} else {
					buf.append((char) ('a' + (halfbyte - 10)));
				}
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	/***
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isNetworkAvailable(Context context) {
		final ConnectivityManager connectivityManager = ((ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE));
		return connectivityManager.getActiveNetworkInfo() != null
				&& connectivityManager.getActiveNetworkInfo().isConnected();
	}

	/**
	 * check if wifi is on or not
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isWifiOn(Context context) {
		boolean result = false;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
		if (activeNetwork != null) { // connected to the internet
			if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
				result = true;
			}
		}
		return result;
	}

	/***
	 * set wifi to on/off
	 * 
	 * @param context
	 * @param status
	 */
	public static void setWifi(Context context, boolean status) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		wifiManager.setWifiEnabled(status);
	}

	/**
	 * switch data to on/off
	 * 
	 * @param context
	 * @param status
	 */
	public static void setData(Context context, boolean status) {
		ConnectivityManager dataManager;
		dataManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Method dataMtd = null;
		try {
			dataMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dataMtd.setAccessible(true);
		try {
			dataMtd.invoke(dataManager, status);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/***
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isDataOn(Context context) {

		boolean mobileDataEnabled = false; // Assume disabled
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			Class cmClass = Class.forName(cm.getClass().getName());
			Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
			method.setAccessible(true); // Make the method callable
			// get the setting for "mobile data"
			mobileDataEnabled = (Boolean) method.invoke(cm);
		} catch (Exception e) {
			e.printStackTrace();
			// Some problem accessible private API
			// TODO do whatever error handling you want here
		}
		return mobileDataEnabled;

		// try
		// {
		// TelephonyManager telephonyService = (TelephonyManager)
		// context.getSystemService(Context.TELEPHONY_SERVICE);
		//
		// Method getMobileDataEnabledMethod =
		// telephonyService.getClass().getDeclaredMethod("getDataEnabled");
		//
		// if (null != getMobileDataEnabledMethod)
		// {
		// boolean mobileDataEnabled = (Boolean)
		// getMobileDataEnabledMethod.invoke(telephonyService);
		//
		// return mobileDataEnabled;
		// }
		// }
		// catch (Exception ex)
		// {
		// Log.e(TAG, "Error getting mobile data state", ex);
		// }

		// return false;
	}
}
