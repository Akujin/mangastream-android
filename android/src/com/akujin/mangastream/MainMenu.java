package com.akujin.mangastream;

import com.akujin.mangastream.MangaStreamService;
import com.nullwire.trace.ExceptionHandler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainMenu extends Activity implements Runnable {
	public ProgressDialog pbdialogs = null;
	public MainMenu self;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self = this;
        
        MangaStreamService.setMainActivity(this);
		if (!MangaStreamService.hasSettings) {
			// Restore preferences
			MangaStreamService.getSettings(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
		}

        if (ExceptionHandler.checkhasStrackTraces(this)) {
            MangaStreamService.mExceptionSubmitDialog = new AlertDialog.Builder(this).setTitle("Please wait...")
    		.setIcon(R.id.Logo).setMessage("Please wait while I am submitting "+
    			"some information that will help us fix the problem.").setCancelable(false).create();
            
        	Dialog dialogAsk = new AlertDialog.Builder(this).setTitle("Please wait...").setMessage("The application has previously crashed "+
        			"unexpectedly. In order to help us fix this problem, Would you like to submit a online bug report?\nBy clicking yes a bug "+
        			"report will automatically be submitted.")
    			.setIcon(R.id.Logo).setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		        	   MangaStreamService.userAnswered = true;
    		        	   MangaStreamService.userSubmitBugs = true;
    		        	   System.out.println("ST: " + ExceptionHandler.checkhasStrackTraces(self));
    		        	   MangaStreamService.registerExceptionHandler(self);
    		        	   if (MangaStreamService.enableNotifications) {
    							Intent svc = new Intent(self, MangaStreamService.class);
    							startService(svc);
    		               }
    		        	   dialog.dismiss();
    		           }
    		       }).setNegativeButton("No", new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		        	   MangaStreamService.userAnswered = true;
    		        	   MangaStreamService.userSubmitBugs = false;
    		        	   ExceptionHandler.deleteOldStackTraces();
    		        	   MangaStreamService.registerExceptionHandler(self);
    		        	   if (MangaStreamService.enableNotifications) {
    							Intent svc = new Intent(self, MangaStreamService.class);
    							startService(svc);
    		               }
    		        	   dialog.dismiss();
    		           }
    		       }).create();
        	dialogAsk.show();
        }
        else {
            MangaStreamService.registerExceptionHandler(this);
            if (MangaStreamService.enableNotifications) {
            	Intent svc = new Intent(self, MangaStreamService.class);
            	startService(svc);
            }
        }
        
		//System.out.println("Setting PreviouschapID: " + MangaStreamService.PreviouschapID);

        if (MangaStreamService.mManager == null) MangaStreamService.mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
   
        String[] menuItems = {"Latest Chapters","Chapters by Series","About"};
                
        setContentView(R.layout.animelist_layout);
        this.setupMenu(menuItems);
        
        PackageManager pm = getPackageManager();
        try {
            //---Get Version Number---
            PackageInfo pi = pm.getPackageInfo("com.akujin.mangastream", 0);
            int AppVer = Integer.valueOf(pi.versionCode);
            
            //MangaStreamService.appVersion = 0;
            
            if (MangaStreamService.appVersion == 0) {
            	//Display First Launch Dialog
            	//final Dialog dialog = new Dialog(this);

            	LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
            	View layout = inflater.inflate(R.layout.firstboot,(ViewGroup) findViewById(R.id.layout_root));

        		TextView title = (TextView) layout.findViewById(R.id.title);
        		title.setText("Welcome to Mangastream");
        		
        		TextView text = (TextView) layout.findViewById(R.id.text);
        		text.setText("In order to provide the best user experience you may decide to enable online error reporting to allow us to fix potential bugs if the application crashes.\n\nYou my disable error reporting in the settings at any time. To access the settings menu click the menu button on your phone.\n\nWould you like to enable crash reports?");
        		
        		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        		dialog.setView(layout);
        		
        		dialog.setCancelable(false)
        		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		        	   	MangaStreamService.enableBugReporting = true;
               		    		dialog.dismiss();
								saveSettings();
        		           }
        		       })
        		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		        	   MangaStreamService.enableBugReporting = false;
							   dialog.dismiss();
							   saveSettings();
        		           }
        		       });
        		
        		dialog.show();
				
				MangaStreamService.appVersion = AppVer;
                saveSettings();
 
            } else if (AppVer > MangaStreamService.appVersion) {
            	//Display ChangeLog or Upgrade Message
            	
            	AboutDialog dialog = new AboutDialog(this, true, null);

        		dialog.setContentView(R.layout.about);
        		dialog.setTitle("Update Successful");

        		TextView text = (TextView) dialog.findViewById(R.id.text);
        		text.setText("You have updated to the latest version of MangaStream");
        	
        		dialog.show();
				
				MangaStreamService.appVersion = AppVer;
                saveSettings();
            }
            
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	saveSettings();
    }
    
    public void saveSettings() {
      	// Save user preferences. We need an Editor object to
		// make changes. All objects are from android.context.Context
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("enableNotifications", MangaStreamService.enableNotifications);
		editor.putBoolean("autoStartService", MangaStreamService.autoStartService);
		editor.putBoolean("enableBugReporting", MangaStreamService.enableBugReporting);
		editor.putInt("PreviouschapID", MangaStreamService.PreviouschapID);
		editor.putString("updateInterval", String.valueOf(MangaStreamService.INTERVAL));
		editor.putInt("appVersion", MangaStreamService.appVersion);
		editor.putInt("notiColor", MangaStreamService.notiColor);
		editor.putString("ringtonePref", "");
		editor.putBoolean("showAllNotifications", MangaStreamService.showAllNotifications);

		// Don't forget to commit your edits!!!
		editor.commit();
    }
    
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
			// capture "Menu" button pressed -> display settings dialog
			Intent myIntent = new Intent(this, SettingsDialog.class);
	        startActivity(myIntent);
			return true;
		default: 
			return super.onKeyDown(keyCode, event);
		}
	}
	
	private String selection = null;
	private Context context = null;
	
    @SuppressWarnings("unchecked")
	private void setupMenu(String[] list) {
    	try {
			
	    	ArrayAdapter<?> listItemAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
	    	ListView lv = (ListView)this.findViewById(R.id.list_view_id);
	    	lv.setAdapter(listItemAdapter);

	    	lv.setOnItemClickListener(new OnItemClickListener(){
	    		public void onItemClick( AdapterView<?> av, View v, int index, long arg){
	    			context = v.getContext();
	    			
	    			selection = av.getItemAtPosition(index).toString();


	    			if(selection == "About") {
	    				showAbout(context);
	    			}
	    			else {
	    				progressBarShow();
	    				Thread thread = new Thread(self);
	    				thread.start();
	    			}
	    		}
	    	
	    	});
    	}
    	catch(Exception e) {
    		Log.e("Error:", e.toString(), e);
    	}
    }
    
    public void progressBarShow() {
    	this.pbdialogs = ProgressDialog.show(MainMenu.this,"","Loading. Please wait...",false,true);
    }

	@Override
	public void run() {
		Looper.prepare(); 
		
		// TODO Auto-generated method stub
		if(selection == "Latest Chapters") {
			if(MangaStreamService.getLatestChapters()) {
		        Intent myIntent = new Intent(context, LatestChapters.class);
		        startActivity(myIntent);
			}
			else {
				Toast.makeText(getApplicationContext(), MangaStreamService.getError(), Toast.LENGTH_LONG).show();
			}
		}
		
		if(selection == "Chapters by Series") {
			if(MangaStreamService.getSeries()) {
		        Intent myIntent = new Intent(context, SeriesSelection.class);
		        startActivity(myIntent);
			}
			else {
				Toast.makeText(getApplicationContext(), MangaStreamService.getError(), Toast.LENGTH_LONG).show();
			}
		}
		handler.sendEmptyMessage(0);
		
		Looper.loop();
        // after Looper.loop() we have to call quit() on our looper
        Looper.myLooper().quit(); 
	}
	
	
    private Handler handler = new Handler() {
    	@Override
    	public void handleMessage(Message msg)  {
    		progressBarHide();
    	}
    };
    
    public void progressBarHide() {
		try {
			if (pbdialogs != null & pbdialogs.isShowing()) {
				pbdialogs.dismiss();
			}
		}
		catch(Exception e) {}
    }

    private void showAbout(Context c) {
    	AboutDialog dialog = new AboutDialog(c, true, null);

		dialog.setContentView(R.layout.about);
		dialog.setTitle("Mangastream for Android");

		TextView text = (TextView) dialog.findViewById(R.id.text);
		text.setText("An android client for MangaStream.com.\n" +
					"Written by Henry Paradiz and Brandon Lis");
		dialog.show();
    }

    @Override
	protected void onDestroy() {
    	ExceptionHandler.notifyContextGone();
		super.onDestroy();
	}
}

class AboutDialog extends Dialog {
	protected AboutDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

    @Override 
    public boolean dispatchTouchEvent(MotionEvent me){ 
		this.dismiss();
		return super.dispatchTouchEvent(me); 
    }
}