package com.uacs.notificationmanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final String TAG = "Notification";
	private CheckBox _checkbox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_checkbox = (CheckBox) findViewById(R.id.cb_smart_notification);
		
//		boolean isWifiOn = Utils.isWifiOn(this);
//		if (isWifiOn) {
//			Log.d(TAG, "wifi is on, turning off..");
//			Utils.setWifi(this, false);
//		}
//		boolean isDataOn = Utils.isDataOn(this);
//        if (isDataOn) {
//        	Log.d(TAG, "data is on, turning off...");
//        	Utils.setData(this, false);
//        }
		
		SharedPreferences sp = getSharedPreferences("default", 0);
		boolean isSmart = sp.getBoolean("smart", false);
		_checkbox.setChecked(isSmart);

		_checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (_checkbox.isChecked()) {
					SharedPreferences sp = getSharedPreferences("default", 0);
					SharedPreferences.Editor editor = sp.edit();
					editor.putBoolean("smart", true);
					editor.commit();
				} else {
					SharedPreferences sp = getSharedPreferences("default", 0);
					SharedPreferences.Editor editor = sp.edit();
					editor.putBoolean("smart", false);
					editor.commit();
				}
			}
		});

	}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sp = this.getSharedPreferences("default", 0);
		boolean isFirstLaunch = sp.getBoolean("isfirstlaunch", true);
		if (isFirstLaunch) {
			Toast.makeText(getApplicationContext(), "Please check Notification Manager", Toast.LENGTH_LONG).show();
			Log.d(TAG, "first Launch");
			startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
			Editor editor = sp.edit();
			editor.putBoolean("isfirstlaunch", false);
			editor.commit();
		} else {
			if (!isMyServiceRunning(NotificationService.class)) {
				startService(new Intent(this, NotificationService.class));
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
