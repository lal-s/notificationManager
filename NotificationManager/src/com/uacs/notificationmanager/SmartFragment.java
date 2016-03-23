package com.uacs.notificationmanager;

import java.util.Date;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SmartFragment extends Fragment {
	private final String TAG = "Notification";
	private CheckBox _checkbox;
	private FragmentActivity activity;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.smart_fragment, container, false);
		_checkbox = (CheckBox) view.findViewById(R.id.cb_smart);
		activity = this.getActivity();
		
		SharedPreferences sp = this.getActivity().getSharedPreferences("default", 0);
		boolean isSmart = sp.getBoolean("smart", false);
		_checkbox.setChecked(isSmart);

		_checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (_checkbox.isChecked()) {
					SharedPreferences sp = activity.getSharedPreferences("default", 0);
					SharedPreferences.Editor editor = sp.edit();
					editor.putBoolean("smart", true);
					editor.commit();
					
					//update log		
					Date date = new Date();
					long milliseconds = date.getTime();
					String line = milliseconds + "|SmartOn\n";
					Utils.writeToLog(line);
					Log.d(TAG, "Smart on");
					
				} else {
					SharedPreferences sp = activity.getSharedPreferences("default", 0);
					SharedPreferences.Editor editor = sp.edit();
					editor.putBoolean("smart", false);
					editor.commit();				
					
					//update log		
					Date date = new Date();
					long milliseconds = date.getTime();
					String line = milliseconds + "|SmartOff\n";
					Utils.writeToLog(line);
					Log.d(TAG, "Smart off");
				}
			}
		});
		
		return view;
	}
}
