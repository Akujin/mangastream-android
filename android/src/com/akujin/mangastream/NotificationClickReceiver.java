package com.akujin.mangastream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationClickReceiver extends Activity {
	@Override
	public void onNewIntent(Intent intent){
		System.out.println("!-------------- notificationView onNewIntent -------------! ");
		String chapID = intent.getStringExtra("chapID");
		System.out.println("!-------------- notificationView -------------! " + chapID);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	System.out.println("!-------------- notificationView -------------! ");
        
        Bundle extras = getIntent().getExtras();
		
		String chapID = extras.getString("chapID");
		System.out.println("!-------------- notificationView -------------! " + chapID);
	}
}