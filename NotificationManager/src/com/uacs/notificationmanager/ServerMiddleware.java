package com.uacs.notificationmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerMiddleware {

	private static String convertInputStreamToString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		String line = "";
		String result = "";
		while ((line = bufferedReader.readLine()) != null)
			result += line;

		inputStream.close();
		return result;
	}

	/*
	 * public static void sendADataToServer(String hashID, File fileToUpLoad)
	 * throws IOException, JSONException{ JSONArray tempJsonArray =
	 * parsingFileToJsonArray(hashID, fileToUpLoad); String url =
	 * "http://ec2-54-218-7-156.us-west-2.compute.amazonaws.com/notification_status/many=false";
	 * HttpPost httpPost = new HttpPost(url); }
	 */

	public static void sendMultiDataToServer(String hashID, File fileToUpLoad) throws IOException, JSONException {
		JSONArray tempJsonArray = parsingFileToJsonArray(hashID, fileToUpLoad);
		System.out.println(tempJsonArray);
		HttpClient httpclient = new DefaultHttpClient();
		String url = "http://ec2-54-218-7-156.us-west-2.compute.amazonaws.com/notification_status/many=true";
		HttpPost httpPost = new HttpPost(url);
		StringEntity entity = new StringEntity(tempJsonArray.toString());
		httpPost.setEntity(entity);
		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");
		HttpResponse httpResponse = httpclient.execute(httpPost);
		System.out.println(httpResponse.getStatusLine());
	}

	public static JSONArray parsingFileToJsonArray(String hashID, File fileToUpLoad) throws IOException, JSONException {
		FileReader fileReader = new FileReader(fileToUpLoad);
		@SuppressWarnings("resource")
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		ArrayList<JSONObject> jsonObjectArray = new ArrayList<JSONObject>();
		String line = null;
		JSONObject tempJsonObject = null;
		while ((line = bufferedReader.readLine()) != null) {
			tempJsonObject = generateNotificationStatusJSON(hashID, line);
			jsonObjectArray.add(tempJsonObject);
		}
		JSONArray tempJsonArray = generateNotificationStatusJSONList(jsonObjectArray);
		return tempJsonArray;
	}

	public static JSONObject generateNotificationStatusJSON(String hashID, String data) throws JSONException {
		String[] dataArray = data.split("\\|");
		JSONObject jsonObject = new JSONObject();
		if (dataArray.length == 2) {
			jsonObject.put("time_stamp", dataArray[0]);
			jsonObject.put("action_type", dataArray[1]);
			jsonObject.put("type_flag", "S");
			jsonObject.put("user_hash_id", hashID);
		} else {
			jsonObject.put("time_stamp", dataArray[0]);
			jsonObject.put("action_type", dataArray[1]);
			jsonObject.put("notification_id", dataArray[2]);
			jsonObject.put("package_name", dataArray[3]);
			jsonObject.put("type_flag", "N");
			jsonObject.put("user_hash_id", hashID);
		}
		return jsonObject;
	}

	public static JSONArray generateNotificationStatusJSONList(ArrayList<JSONObject> jsonObjectArray)
			throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < jsonObjectArray.size(); i++)
			jsonArray.put(jsonObjectArray.get(i));
		return jsonArray;
	}
}
