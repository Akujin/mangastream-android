package com.akujin.mangastream;

import com.nullwire.trace.ExceptionHandler;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsDialog extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MangaStreamService.registerExceptionHandler(this);
		addPreferencesFromResource(R.layout.settings);
	
		findPreference("enableNotifications").setEnabled(MangaStreamService.enableNotifications);

		findPreference("enableNotifications").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				// TODO Auto-generated method stub
				MangaStreamService.enableNotifications = Boolean.valueOf(arg1.toString());
				serviceSettingsChanged();
				findPreference("NotificationSettings").setEnabled(MangaStreamService.enableNotifications);
				return true;
			}
		});
		
		findPreference("updateIntervals").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				// TODO Auto-generated method stub

				MangaStreamService.INTERVAL = Integer.valueOf(arg1.toString());
				serviceSettingsChanged();
				return true;
			}
		});
		
		final ColorPickerDialog colorpicker = new ColorPickerDialog(this,new ColorPickerDialog.OnColorChangedListener(){
			public void colorChanged(int color) {
				MangaStreamService.notiColor = color;
				MangaStreamService.MAIN_ACTIVITY.saveSettings();
			}
		},Color.BLUE);
		
		Preference customPref = (Preference) findPreference("lightPrefDialog");
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				colorpicker.show();
				return true;
			}
		});
		
	}
	
    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Reset Settings").setIcon(android.R.drawable.ic_menu_rotate);
        return true;
    }
    
    public boolean requestReset = false;
    
    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 0:
			findPreference("enableNotifications").setPersistent(false);
			findPreference("autoStartService").setPersistent(false);
			findPreference("enableBugReporting").setPersistent(false);
			findPreference("ringtonePref").setPersistent(false);
			
			ListPreference updateIntervals = (ListPreference) findPreference("updateIntervals");
			updateIntervals.setValue("10");
			
	    	MangaStreamService.INTERVAL = 10;
	    	MangaStreamService.enableNotifications = true;
	    	MangaStreamService.autoStartService = true;
	    	MangaStreamService.enableBugReporting = true;
	    	MangaStreamService.notiColor = Color.BLUE;
	    	MangaStreamService.ringtonePref = "";
	    	MangaStreamService.showAllNotifications = false;
	    	
    	    MangaStreamService.MAIN_ACTIVITY.saveSettings();
    	    
        	finish();
            return true;
        }
        return false;
    }
    
	void serviceSettingsChanged() {
		System.out.println("Service Modified");
		MangaStreamService.MAIN_ACTIVITY.saveSettings();
		if (MangaStreamService.enableNotifications) {
			MangaStreamService.stopservice();
			if (!MangaStreamService.serviceRunning) {
				Intent svc = new Intent(getApplicationContext(), MangaStreamService.class);
		        startService(svc);
			} else {
				MangaStreamService.startservice();
			}
		} else {
			if (MangaStreamService.serviceRunning) {
				Intent svc = new Intent(getApplicationContext(), MangaStreamService.class);
		        stopService(svc);
		        MangaStreamService.stopservice();
			}
		}
	}
	
	protected void onPause(){
		super.onPause();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		MangaStreamService.getSettings(settings);
	}
	
   @Override
	protected void onDestroy() {
    	ExceptionHandler.notifyContextGone();
		super.onDestroy();
	}
}
