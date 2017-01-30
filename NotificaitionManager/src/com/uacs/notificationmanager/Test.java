package com.uacs.notificationmanager;

import java.io.File;
import java.io.IOException;
import org.json.JSONException;

public class Test {
	 public static void main(String[] args){
		 File testFile = new File("2015-11-11.txt");
		 try {
			ServerMiddleware.sendMultiDataToServer("16f9a13b67322955f28248af218b5095", testFile);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
}
