package com.akujin.mangastream;

import java.util.HashMap;
import com.nullwire.trace.ExceptionHandler;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ChaptersBySeries extends Activity implements Runnable {
	
	public ChaptersBySeries self = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	self=this;
    	MangaStreamService.registerExceptionHandler(this);
    	
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	String seriesID = extras.getString("seriesID");
        	MangaStreamService.currentSeries = seriesID;
        }
    }
    
    public void onStart() {
    	super.onStart();
    	
    	if(MangaStreamService.get_chapters_by_series == null || MangaStreamService.get_chapters_by_series.size() == 0) {
    		if(!MangaStream.get_chapters_by_series(MangaStreamService.currentSeries)) {
    			finish();
    		}
    	}

        try {
        	String[] list = new String[MangaStreamService.get_chapters_by_series.size()];
        	int count = 0;
        	
	        for(int position = 0; position < MangaStreamService.get_chapters_by_series.size(); position++) {
		        HashMap<String, String> li = MangaStreamService.get_chapters_by_series.get(position);
		        
		        //Sets the name of the series according to the json, no need to pass it to the class anymore.
		        if (position == 0) { setTitle("MangaStream - " + li.get("series_name").toString() ); }
		        
		        String data = li.get("chapter").toString() + ": " + li.get("title").toString();
		        
		        list[count] = data;
		        count++;
	        }
	        	        
	        setContentView(R.layout.animelist_layout);
	        setupAnimeListView(list);
        }
    	catch(Exception e) {
    		Log.e("Error:", e.toString(), e);
    	}
    }
    
	private int selection = 0;
	private Context context = null;
    private ProgressDialog pbdialogs = null;
    
    private void setupAnimeListView(final String[] list) {
    	try {
	    	ArrayAdapter listItemAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
	    	ListView lv = (ListView)this.findViewById(R.id.list_view_id);
	    	lv.setAdapter(listItemAdapter);

	    	lv.setOnItemClickListener(new OnItemClickListener(){
	    		public void onItemClick( AdapterView av, View v, int index, long arg){

	    			context = v.getContext();
	    			selection = index;
	    			
	    			pbdialogs = ProgressDialog.show(ChaptersBySeries.this, "", "Loading. Please wait...", true);

	    			
	    			//Get the MangaStream ID of the selected chapter so we can pull info on it
	    			HashMap<String, String> li = MangaStreamService.get_chapters_by_series.get(index);
	    			String chapID = li.get("manga_id");
	    			
	    			MangaStreamService.currentChapter = chapID;
	    			
	    			Thread thread = new Thread(self);
    				thread.start();

	    		}
	    	
	    	});
    	}
    	catch(Exception e) {
    		Log.e("Error:", e.toString(), e);
    	}
    }

	@Override
	public void run() {
		Looper.prepare(); 
		
		if(MangaStreamService.getChapter(MangaStreamService.currentChapter)) {
			Intent myIntent = new Intent(context, PageView.class);
	        startActivity(myIntent);
		}
		else {
			Toast.makeText(getApplicationContext(), MangaStreamService.getError(), Toast.LENGTH_SHORT).show();
		}
		handler.sendEmptyMessage(0);
		
		Looper.loop();
        // after Looper.loop() we have to call quit() on our looper
        Looper.myLooper().quit();
	}
	
    private Handler handler = new Handler() {
    	@Override
    	public void handleMessage(Message msg)  {
			if (pbdialogs != null & pbdialogs.isShowing()) {
				pbdialogs.dismiss();
			}
    	}
    };
    @Override
	protected void onDestroy() {
    	ExceptionHandler.notifyContextGone();
		super.onDestroy();
	}
}
