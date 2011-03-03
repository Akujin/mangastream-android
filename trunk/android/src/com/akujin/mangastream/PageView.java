package com.akujin.mangastream;

import java.util.HashMap;
import com.akujin.mangastream.SimpleGestureFilter.SimpleGestureListener;
import com.nullwire.trace.ExceptionHandler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class PageView extends Activity implements SimpleGestureListener {
	
	private static final String String = null;

	WebView webview = null;
	
	AlertDialog pageSelection = null;
	
	private SimpleGestureFilter detector;
	
	private int zoom = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MangaStreamService.registerExceptionHandler(this);
        
        
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	try {
	        	MangaStreamService.get_chapter.clear();
	        	MangaStreamService.get_chapter_pages.clear();
	        	
	        	String chapID = extras.getString("chapID");
	        	MangaStreamService.currentChapter = chapID;
	        	if(!MangaStreamService.getChapter(MangaStreamService.currentChapter)) {
	    			Toast.makeText(getApplicationContext(), MangaStreamService.getError(), Toast.LENGTH_SHORT).show();
	    			finish();
	    		}
        	} catch(Exception e) { e.printStackTrace(); }
        }
        else if(MangaStreamService.get_chapter_pages == null || MangaStreamService.get_chapter_pages.size() == 0) {
    		if(!MangaStreamService.getChapter(MangaStreamService.currentChapter)) {
    			Toast.makeText(getApplicationContext(), MangaStreamService.getError(), Toast.LENGTH_SHORT).show();
    			finish();
    		}
    	}
 
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        
        detector = new SimpleGestureFilter(this,this);
        	  	
        try {
        	
        	//final String[] pages = new String[MangaStreamService.currentChapter_pages.size()];
        	
        	
        	final String[] pages = new String[MangaStreamService.get_chapter_pages.size()];
        	int count = 0;
        	
	        for(int position = 0; position < MangaStreamService.get_chapter_pages.size(); position++) {
		        HashMap<String, String> li = MangaStreamService.get_chapter_pages.get(position);

		        pages[count] = "Page " + li.get("page");
	            count++;
	        }

	        System.out.println(MangaStreamService.get_chapter_pages.toString());
	        
	        MangaStreamService.currentPage = 0;
	        
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle("Select a Page");
        	builder.setItems(pages, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int page) {
        			MangaStreamService.currentPage = Integer.valueOf(page);
        	    	refresh();
        	    }
        	});
        	pageSelection = builder.create();
	        
	        refresh();
        }
		catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
            switch(keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if(MangaStreamService.PrevPage()) {
                	refresh();
                }
                return super.onKeyDown(keyCode, ev);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if(MangaStreamService.NextPage()) {
                	refresh();
                }
                return super.onKeyDown(keyCode, ev);
            case KeyEvent.KEYCODE_DPAD_UP:
            	webview.scrollBy(0, -10);
            	return super.onKeyDown(keyCode, ev);
            case KeyEvent.KEYCODE_DPAD_DOWN:
            	webview.scrollBy(0, 10);
            	return super.onKeyDown(keyCode, ev);
            case KeyEvent.KEYCODE_R:
            	refresh();
            	return super.onKeyDown(keyCode, ev);
            case KeyEvent.KEYCODE_BACK:
            	finish();
            	return super.onKeyDown(keyCode, ev);
            default:
                    return false;
            }
    } 
    
    
    /* Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Next Page").setIcon(android.R.drawable.ic_media_rew);
        menu.add(0, 1, 0, "Refresh").setIcon(android.R.drawable.ic_menu_rotate);
        menu.add(0, 2, 0, "Page Selection").setIcon(android.R.drawable.ic_menu_upload);
        menu.add(0, 3, 0, "Prev Page").setIcon(android.R.drawable.ic_media_ff);
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 0:
            if(MangaStreamService.NextPage()) {
            	refresh();
            } else {
            	Toast.makeText(getApplicationContext(), "You are on the last page.", Toast.LENGTH_SHORT).show();
            }
            return true;
        case 1:
        	refresh();
        	return true;
        case 2:
        	pageSelection.show();
        	
        	//refresh();
        	return true;
        case 3:
            if(MangaStreamService.PrevPage()) {
            	refresh();
            } else {
            	Toast.makeText(getApplicationContext(), "You are on the first page.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }
    
    public void refresh() {
    	String currentMangaName = MangaStreamService.get_chapter.get("series_name").toString();
    	String currentChapterName = MangaStreamService.get_chapter.get("title").toString();
    	
    	//Get the MangaStream ID of the selected chapter so we can pull info on it
		HashMap<String, String> li = MangaStreamService.get_chapter_pages.get(MangaStreamService.currentPage);
		
		String url = li.get("url").toString();
		String page = li.get("page").toString();
		
		System.out.println(url);
		
    	setURL(url);
    	setTitle(currentMangaName + " " + currentChapterName + " Page " + page + " | MangaStream");
    }

    public void onConfigurationChanged(Configuration newConfig) { 
    	super.onConfigurationChanged(newConfig); 

	  	DisplayMetrics metrics = new DisplayMetrics();
	  	getWindowManager().getDefaultDisplay().getMetrics(metrics);
	  	
	  	int orientation = newConfig.orientation;
	  	
	  	int mindistance = 0;
	  	
	  	if (orientation==1) {
	  		mindistance = (int) (metrics.widthPixels * 0.40);
	  	} else {
	  		mindistance = (int) (metrics.widthPixels * 0.60);
	  	}
	  	int maxdistance = (int) (metrics.widthPixels * 0.99);
	  	
	  	detector.setSwipeMinDistance(mindistance);
	  	detector.setSwipeMaxDistance(maxdistance);
	  	detector.setSwipeMinVelocity(600);
    }
    
    public void setURL(String url) {
    	zoom = 0;
    	
    	webview = new WebView(this);

    	webview.requestFocus();
    	webview.getSettings().setBuiltInZoomControls(true);
    	webview.getSettings().setUseWideViewPort(true);
    	webview.setMapTrackballToArrowKeys(true);
    	webview.setInitialScale(0); //From API doc: 0 means default. If getUseWideViewPort() is true, it zooms out all the way
    	
		setContentView(webview);
		
		final Activity activity = this;
		webview.setWebChromeClient(new WebChromeClient() {
		   public void onProgressChanged(WebView view, int progress) {
		     // Activities and WebViews measure progress with different scales.
		     // The progress meter will automatically disappear when we reach 100%
			 if(progress == 100) {
				try{ closeOptionsMenu(); } catch(Exception e) {}
				 
			  	DisplayMetrics metrics = new DisplayMetrics();
			  	getWindowManager().getDefaultDisplay().getMetrics(metrics);
					  	
			  	int orientation = getResources().getConfiguration().orientation;
			  	
			  	int mindistance = 0;
			  	int maxdistance = 0;
			  	if (orientation==1) {
			  		mindistance = (int) (metrics.widthPixels * 0.90);
			  		maxdistance = (int) (metrics.widthPixels * 1);
			  	} else {
			  		mindistance = (int) (metrics.widthPixels * 0.75);
			  		maxdistance = (int) (metrics.widthPixels * 1);
			  	}
			  	
			  	//System.out.println("Orientation Detected: "+orientation);
			  	
			  	detector.setSwipeMinDistance(mindistance);
			  	detector.setSwipeMaxDistance(maxdistance);
			  	detector.setSwipeMinVelocity(600);
			 }
			 else {
				 try { openOptionsMenu(); } catch(Exception e) {}
			 }
		     activity.setProgress(progress * 100);
		   }
		 });
		 webview.setWebViewClient(new WebViewClient() {
		   public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		     Toast.makeText(activity, "Error: " + description, Toast.LENGTH_SHORT).show();
		   }
		 });
		 
		/*"file://sdcard/DCIM/Camera/1250058664828.jpg"*//*OMservice.url + OMservice.getCurrentPage()*/
		
		HashMap<String, String> li = MangaStreamService.get_chapter_pages.get(MangaStreamService.currentPage);
		String width = li.get("width").toString();
		 
		webview.getSettings().setJavaScriptEnabled(true);
		webview.loadData("<center><IMG SRC=\"" + url + "\" /></center><script>window.scrollTo(" + width + ", 0);</script>", "text/html",  "UTF-8");

		//webview.scrollTo(300, 0);
		//webview.loadUrl(url);
    }

    @Override 
    public boolean dispatchTouchEvent(MotionEvent me){ 
      this.detector.onTouchEvent(me);
     return super.dispatchTouchEvent(me); 
    }
    
    @Override
    public void onSwipe(int direction) {
     String str = "";
	 
	 switch (direction) {
		 case SimpleGestureFilter.SWIPE_RIGHT :
         if(MangaStreamService.NextPage()) {
         	refresh();
         } else {
         	Toast.makeText(getApplicationContext(), "You are on the last page.", Toast.LENGTH_SHORT).show();
         }
	     break;
		 case SimpleGestureFilter.SWIPE_LEFT :
         if(MangaStreamService.PrevPage()) {
         	refresh();
         } else {
         	Toast.makeText(getApplicationContext(), "You are on the first page.", Toast.LENGTH_SHORT).show();
         }
	     break;         
		 //case SimpleGestureFilter.SWIPE_UP :
	     //    webview.zoomIn();
		 //break;
		 //case SimpleGestureFilter.SWIPE_DOWN :
	     //    webview.zoomOut();
		 //break; 
     }
     //System.out.println(str);
     //Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDoubleTap() {
    	if(zoom == 0) {
    		webview.zoomOut();
    		webview.zoomOut();
    		webview.zoomOut();
    		zoom = 1;
    	}
    	else {
    		webview.zoomIn();
    		webview.zoomIn();
    		webview.zoomIn();
    		zoom = 0;
    	}
       //Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show(); 
    }
    @Override
	protected void onDestroy() {
    	ExceptionHandler.notifyContextGone();
		super.onDestroy();
	}
}
