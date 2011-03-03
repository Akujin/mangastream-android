package com.akujin.mangastream;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver {

 @Override
 public void onReceive(Context context, Intent intent) {
	Intent serviceIntent=new Intent(context, MangaStreamService.class);
	PendingIntent pi=PendingIntent.getBroadcast(context, 0,serviceIntent, 0);
	context.startService(serviceIntent);
 }
}