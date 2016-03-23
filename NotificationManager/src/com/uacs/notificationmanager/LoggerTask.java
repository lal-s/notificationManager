package com.uacs.notificationmanager;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;

import android.os.AsyncTask;
import android.util.Log;

public class LoggerTask extends AsyncTask<String, Void, Boolean> {
	private  final static String TAG = "Notification";
	//private final static String TAG = "alarm";


	private File file;
	private String hashId;

	@Override
	protected Boolean doInBackground(String... args) {
		this.file = new File(args[0]);
		this.hashId = args[1];
		Log.d(TAG, "uploading file:" + file.getName() +"; hashId:" + hashId);
		try {
				ServerMiddleware.sendMultiDataToServer(hashId, file);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			Log.d(TAG, "onPostExecute ");

			boolean isDeleted = Utils.deleteFile(file);
			if (isDeleted) {
				Log.d(TAG, file.getName() + " is deleted!");
			} else {
				Log.d(TAG, "fail to delete " + file.getName());

			}
		}
	}
}
