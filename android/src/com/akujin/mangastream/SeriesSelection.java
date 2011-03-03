package com.akujin.mangastream;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import com.nullwire.trace.ExceptionHandler;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SeriesSelection extends Activity {
	private SeriesSelection self = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self=this;
        MangaStreamService.registerExceptionHandler(this);
    }
    
    public void onStart() {
    	super.onStart();
    	
    	if(MangaStreamService.get_series == null || MangaStreamService.get_series.size() == 0) {
    		if(!MangaStream.get_series()) {
    			finish();
    		}
    	}
    	
        try {
        	ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        	int count = 0;
        	
	        for(int position = 0; position < MangaStreamService.get_series.size(); position++) {
		        HashMap<String, String> li = MangaStreamService.get_series.get(position);
		        
		        HashMap<String, String> map = new HashMap<String, String>();
		        
	        	map.put("series", li.get("series_name").toString());
	        	map.put("id", String.valueOf(count));
	        	list.add(map);

		        count++;
	        }

	        setContentView(R.layout.animelist_layout);
	        setupAnimeListView(list);
        }
    	catch(Exception e) {
    		Log.e("Error:", e.toString(), e);
    	}
    }
    
    private void setupAnimeListView(final ArrayList<HashMap<String, String>> list) {
    	try {
    		SeriesAdapter listItemAdapter = new SeriesAdapter(this, list, R.layout.animelist_layout_row, new String[] {"series","id"}, new int[] {R.id.TITLE});
	    	ListView lv = (ListView)this.findViewById(R.id.list_view_id);
	    	lv.setAdapter(listItemAdapter);
    	}
    	catch(Exception e) {
    		Log.e("Error:", e.toString(), e);
    	}
    }
        
    private class SeriesAdapter extends SimpleAdapter implements Runnable {

    	ArrayList<HashMap<String, String>> list = null;
    	String[] from = null;
    	int[] to = null;
    	int layoutRow = 0;
    	Context context = null;
    	SeriesAdapter self = null;
    	
    	/*	SeriesSelection 					context					(this) from activity class
    	 *  ArrayList<HashMap<String, String>> 	list					data
    	 *  int 								animelistLayoutRow		ID of row Layout
    	 *  String[] 							from					
    	 *  int[] 								to
    	 */
    	public SeriesAdapter(SeriesSelection context,ArrayList<HashMap<String, String>> list, int animelistLayoutRow,String[] from, int[] to) {
    		super(context, list, animelistLayoutRow, from, to);
    		
    		this.list = list;
    		this.from = from;
    		this.to = to;
    		this.layoutRow = animelistLayoutRow;
    		this.context = context;
    		this.self = this;
    		this.favs = MangaStream.getFavsIDs();
    	}
    	
    	View v = null;
        private ProgressDialog pbdialogs = null;
    	private ArrayList<Long> favs = null;
    	
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
                v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(this.layoutRow, null);
                }
                
                final HashMap<String, String> li = list.get(position);
                int count = 0;
                if (li != null) {
                        TextView title = (TextView) v.findViewById(R.id.TITLE);
                        if (title != null) {
                              title.setText(li.get("series"));
                        }

                        String seriesid = li.get("id");
                        
                        HashMap<String, String> row = MangaStreamService.get_series.get(Integer.valueOf(seriesid));
                		final String seriesID = row.get("series_id");
                		
                        title.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                            	
                            	pbdialogs = ProgressDialog.show(SeriesSelection.this, "", "Loading. Please wait...", false, true);
                            	//Get the MangaStream ID of the selected chapter so we can pull info on it

                        		MangaStreamService.currentSeries = seriesID;
                        		
        	    				Thread thread = new Thread(self);
        	    				thread.start();
                            }
                        });
                        
                        //Get Icon URL
                        String url = row.get("icon");
                        String iconpath = null;
                        
                        if (url.length()>0) {
                        	//Get the location of the private directory located in /data/data/package/app_<name>/
	                        File privateDir = context.getDir("icons", Context.MODE_PRIVATE);
	                        
	                        //Get Path to where the icon should be saved to
							iconpath = privateDir.getAbsolutePath() + "/" + url.substring(url.lastIndexOf("/")+1,url.length());
                        }
                        
        	            ImageView icon = (ImageView) v.findViewById(R.id.SeriesIcon);
        	            BitmapFactory.Options options = new BitmapFactory.Options();
        	            options.inSampleSize = 2;
                        
            			if(iconpath != null && MangaStream.download_file(url, iconpath)) {
            	            //Set Icon
            	            Bitmap bm = BitmapFactory.decodeFile(iconpath, options);
            	            if (bm != null) {
            	            	icon.setImageBitmap(bm);
            	            } else {
            	            	icon.setImageDrawable(context.getResources().getDrawable(R.drawable.icon));
            	            }
            			} else {
            				icon.setImageDrawable(context.getResources().getDrawable(R.drawable.icon));
            			}
                        
            			final String seriesName = li.get("series");
            			
                        //Stars
                        final CheckBox isFav = (CheckBox) v.findViewById(R.id.isFavorite);
                        
                        long seriesIDLong = Long.valueOf(seriesID);
                        if (favs.contains(seriesIDLong)) {
                        	isFav.setChecked(true);
                        }
                        else {
                        	isFav.setChecked(false);
                        }
                        
                        final Context context = this.context;
                        isFav.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                // Perform action on clicks
                                //Toast.makeText(context, "Beep Bop", Toast.LENGTH_SHORT).show();
                            	if (isFav.isChecked()) {
                            		//Toast.makeText(context, "You will now be notified when " + seriesName + " has new chapters.", Toast.LENGTH_LONG).show();
                            		MangaStream.addFav(seriesID);
                            	} else{
                            		//Toast.makeText(context, "You will no longer be notified when " + seriesName + " has new chapters.", Toast.LENGTH_LONG).show();
                            		MangaStream.delFav(seriesID);
                            	}
                            }
                        });
                        
                        //Drawable star = context.getResources().getDrawable(R.drawable.star);

                        count++;
                }
                return v;
        }

    	@Override
    	public void run() {
    		Looper.prepare(); 
    		
    		
    		if(MangaStreamService.getChaptersBySeries(MangaStreamService.currentSeries)) {
    			Intent myIntent = new Intent(context, ChaptersBySeries.class);
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

    }
    
    @Override
	protected void onDestroy() {
    	ExceptionHandler.notifyContextGone();
		super.onDestroy();
	}
}
