package com.akujin.mangastream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import com.nullwire.trace.ExceptionHandler;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.app.Service;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class MangaStreamService extends Service {

	public static String bugporturl = "http://www.jplai.com/mangastream/report.php";
	
	public static boolean serviceRunning = false;
	
	public static MainMenu MAIN_ACTIVITY;
	public static MangaStreamService MSS;

	private static Timer timer = null;
	public static NotificationManager mManager = null;
	private static Date lastChecked = null;
	public static int NOTIFICATIONID = 1;

	public static String error = null;
	
	public static int currentPage = 0;
	
	public static String currentChapter = null;
	public static String currentSeries = null;
	public static String currentSeriesName = null;
	
    public static ArrayList<HashMap<String, String>> get_chapters_latest = new ArrayList<HashMap<String, String>>();
    public static ArrayList<HashMap<String, String>> get_chapters_by_series = new ArrayList<HashMap<String, String>>();
    public static ArrayList<HashMap<String, String>> get_series = new ArrayList<HashMap<String, String>>();
    
    public static HashMap<String, String> get_chapter = new HashMap<String, String>();
    public static ArrayList<HashMap<String, String>> get_chapter_pages = new ArrayList<HashMap<String, String>>();
    
	public static DataBaseHelper myDbHelper = null;
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// environmental variables
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	
	public static boolean userAnswered = false;
	public static boolean userSubmitBugs = false;
	
	public static boolean hasSettings = false;
	public static int INTERVAL = 10;
	public static int PreviouschapID = 0;
	public static boolean enableNotifications = false;
	public static boolean autoStartService = false;
	public static boolean enableBugReporting = true;
	public static int appVersion = 0;
	public static int notiColor = Color.BLUE;
	public static String ringtonePref = "";
	public static boolean showAllNotifications = false;
	
	public static boolean disableFavs = true;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void registerExceptionHandler(Context ourObject) {
		ExceptionHandler.setUrl(MangaStreamService.bugporturl);
        ExceptionHandler.setMinDelay(4000);
        ExceptionHandler.setHttpTimeout(10000);
        ExceptionHandler.setup(ourObject, new ExceptionHandler.Processor() {
        	@Override
			public boolean beginSubmit() {
        		
        		System.out.println("registerExceptionHandler");
        		if (!userAnswered || !userSubmitBugs) { return false; }
        		
				mExceptionSubmitDialog.show();
				return true;
			}

			@Override
			public void submitDone() {
				mExceptionSubmitDialog.cancel();
			}

			@Override
			public void handlerInstalled() {
				
			}
		});
	}
	
	static Dialog mExceptionSubmitDialog;

	public static void getSettings(SharedPreferences settings) {
		enableNotifications = settings.getBoolean("enableNotifications", true);
		autoStartService = settings.getBoolean("autoStartService", true);
		PreviouschapID = settings.getInt("PreviouschapID", 0);
		INTERVAL = Integer.valueOf(settings.getString("updateIntervals", "10"));
		enableBugReporting = settings.getBoolean("enableBugReporting", true);
		appVersion = settings.getInt("appVersion", 0);
		notiColor = settings.getInt("notiColor", Color.BLUE);
		ringtonePref = settings.getString("ringtonePref", "");
		showAllNotifications = settings.getBoolean("showAllNotifications", false);
		hasSettings = true;
	}

    public void onCreate() {
		super.onCreate();
		
		MSS = this;

		registerExceptionHandler(this);

    	try {
	        myDbHelper = new DataBaseHelper(this);
	        myDbHelper.open();
	        
	        try {
	    	    ArrayList<String[]> map = new ArrayList<String[]>();
	    	    
	    	    String[] series_id = {"int","1"};
	    	    map.add(series_id);
	    	    
	    	    String[] timestamp = {"int","0"};
	    	    map.add(timestamp);
	    	    
	    	    String[] last_chapter_id = {"int","1"};
	    	    map.add(last_chapter_id);
	    	    
		    	MangaStreamService.myDbHelper.statement("insert into favorites (series_id,timestamp,last_chapter_id) values (?,?,?)",map);

	    	 } catch(Exception e) {e.printStackTrace();}
	    	 
	    	 try {
		    	ArrayList<String[]> amap = new ArrayList<String[]>();
	    	    
	    	    String[] aseries_id = {"int","5"};
	    	    amap.add(aseries_id);
	    	    
	    	    String[] atimestamp = {"int","0"};
	    	    amap.add(atimestamp);
	    	    
	    	    String[] alast_chapter_id = {"int","1"};
	    	    amap.add(alast_chapter_id);
	    	    
		    	MangaStreamService.myDbHelper.statement("insert into favorites (series_id,timestamp,last_chapter_id) values (?,?,?)",amap);
		    } catch(Exception e) {e.printStackTrace();}
		    
	    } catch(Exception e) {
    		e.printStackTrace();
    	}
				
		if (!hasSettings) {
			// Restore preferences
			getSettings(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
		}
		
		if (enableNotifications) {
			System.out.println("Starting MangaStreamService");
			serviceRunning = true;
			//PreviouschapID = 94;
			startservice();
		} else {
			//System.out.println("Stopping MangaStreamService, Disabled.");
			stopSelf();
		}
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	System.out.println("Stopping MangaStreamService, Disabled.");
    	serviceRunning = false;
    	stopservice();
    	try {
			myDbHelper.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
    	ExceptionHandler.notifyContextGone();
    }
    
	public static void startservice() {		
		int calculatedINTERVAL = (INTERVAL * 60 * 1000);

		System.out.println("Mangastream Timer Running");
		//MangaStreamService.PreviouschapID = 96;
		try {
			timer = new Timer();
			timer.scheduleAtFixedRate( new TimerTask() {
				private HashMap<Integer, HashMap<Integer, Long>> favs = null;
				
				//favorites: id series_id last_chapter_id timestamp
			    public HashMap<Integer, HashMap<Integer, Long>> getFavs() {
			        Cursor c = MangaStreamService.myDbHelper.rawQuery("SELECT series_id,last_chapter_id,timestamp FROM favorites", null);
			        
			        HashMap<Integer,HashMap<Integer,Long>> favs = new HashMap<Integer,HashMap<Integer,Long>>();
			        
			        int series_idColumn = c.getColumnIndex("series_id");
			        int last_chapter_idColumn = c.getColumnIndex("last_chapter_id");
			        int timestampColumn = c.getColumnIndex("timestamp");

			        System.out.println("Getting Favs List");
			        
			        /* Check if our result was valid. */
			        if (c != null) {
			             /* Check if at least one Result was returned. */
			        	if(c.moveToFirst()){
			                  int count = c.getCount();
			                  
			                  /* Loop through all Results */
			                  for(int i=0; i<count; i++){
			                       /* Retrieve the values of the Entry
			                        * the Cursor is pointing to. */
			                	  
			                	   HashMap<Integer, Long> row = new HashMap<Integer,Long>();
			                	   int idName = (int) c.getLong(series_idColumn);
			                	   long LidName = c.getLong(series_idColumn);
			                       long chpName = c.getLong(last_chapter_idColumn);
			                       long tsName = c.getLong(timestampColumn);
			                       row.put(1,LidName);
			                       row.put(2,chpName);
			                       row.put(3,tsName);
			                       
			                       favs.put(idName,row);
			                       c.moveToNext();
			                  };
			             }
			        }
			        c.close();
			        return favs;
			    }

				public void sendNotification(String tbN,String nT,String nD,Intent intent) {
					NotificationManager nmanager = (NotificationManager) MSS.getSystemService(Context.NOTIFICATION_SERVICE);
					nmanager.cancelAll();
					
				    Notification notification = new Notification(R.drawable.icon,tbN, System.currentTimeMillis());
				    //notification.defaults = Notification.DEFAULT_LIGHTS;
				    notification.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;
				    notification.sound = Uri.parse(ringtonePref);
				    notification.ledARGB = notiColor;
				    notification.ledOnMS = 500;
				    notification.ledOffMS = 500;
				    notification.setLatestEventInfo(
				    		MSS.getApplicationContext(),
				    		nT,
				    		nD,
				    		PendingIntent.getActivity(MSS.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
				    );
				    MangaStreamService.NOTIFICATIONID++;
				    nmanager.notify(MangaStreamService.NOTIFICATIONID, notification);
				}
			    
				public void run() {
					System.out.println("Mangastream Timer Running");
					Date now = new Date();
					
					if (lastChecked == null) {
						lastChecked = new Date();
					} else {
						long duration = ( now.getTime() - lastChecked.getTime() ) / 1000;
						if (duration<300) {
							//System.out.println("Mangastream Timer ran too soon, Last ran " + duration + " seconds ago.");
							return;
						} else {
							lastChecked = now;
						}
					}

					if (!MangaStream.get_chapters_latest()) {
						return;
					} else if (MangaStreamService.get_chapters_latest != null && MangaStreamService.get_chapters_latest.size() > 0) {
						if (!disableFavs) { favs = getFavs(); }

				        for(int position = 0; position < MangaStreamService.get_chapters_latest.size(); position++) {
				        	if (disableFavs || favs.size() == 0 || showAllNotifications) {
				        		System.out.println("Checking All Chapters");
				        		//Checks for Notifications for all series by comparing PreviouschapID
				        		checkNewNotificationsAll(position);
				        		break;
				        	} else {
				        		System.out.println("Checking For Fav Chapters");
				        		
				        		//Check the entire json array for new series announcements
				        		checkNewFavs(position);
				        	}
		    			}
				        
				        if (disableFavs || favs.size()>0 && !showAllNotifications) {
				        	//Now use that data and notify the user how many each series has
				        	NewFavsNotifications();
				        }
					}
				}

				public void checkNewNotificationsAll(int position) {
		        	HashMap<String, String> m = MangaStreamService.get_chapters_latest.get(position);

    				String chapID = m.get("manga_id").toString();
    				int intChapID = Integer.valueOf(chapID);
					if (PreviouschapID == 0 || intChapID > PreviouschapID) {

						Intent viewMsgIntent = null;
						
						String taskBarNotification = "";
						String notiTitle = "";
						String notiDesc = "";
						
						int newChps = ( intChapID - PreviouschapID );
						
						if (newChps>1) {
							taskBarNotification = "Mangastream - " + newChps + " New Chapters Released";
							notiTitle = "Mangastream - New Chapters Released";
							notiDesc = "There are " + newChps + " new chapters released";
							
							viewMsgIntent = new Intent(MSS.getBaseContext(), LatestChapters.class);
							viewMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							viewMsgIntent.setAction("LatestChapters");
						} else {
							taskBarNotification = "Mangastream - " + m.get("series_name") + " Chp. " + m.get("chapter") + " Released";
							notiTitle = "Mangastream - " + m.get("series_name") + " Chp. " + m.get("chapter") + " Released";
							notiDesc = "Click here to read this chapter";
							
							viewMsgIntent = new Intent(MSS.getBaseContext(), PageView.class);
							viewMsgIntent.setAction("PageView");
							viewMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							viewMsgIntent.putExtra("chapID",chapID);
						}
						
						int series_id = Integer.valueOf(m.get("series_id"));
						if (favs.containsKey(series_id)) {
							MangaStream.updateFav(series_id,Integer.valueOf(m.get("chapter")));
							
						}
						MangaStreamService.PreviouschapID = intChapID;
						
						sendNotification(taskBarNotification,notiTitle,notiDesc,viewMsgIntent);
					}
				}
				
				ArrayList<Integer> detectedSeriesIDs = new ArrayList<Integer>();
				HashMap<Integer,Integer> detectedTimes = new HashMap<Integer,Integer>();
				HashMap<Integer, HashMap<String,String>> detectedData = new HashMap<Integer, HashMap<String,String>>();
				
				public void checkNewFavs(int position) {
		        	HashMap<String, String> m = MangaStreamService.get_chapters_latest.get(position);

		        	int seriesID = Integer.valueOf(m.get("series_id"));
		        	int chapter = Integer.valueOf(m.get("chapter"));
    				String chapID = m.get("manga_id").toString();
    				int intChapID = Integer.valueOf(chapID);
    				
    				//Update Latest Chapter ID
    				if (position == 0) { MangaStreamService.PreviouschapID = intChapID; }
    				
    				try {
	    				if (favs.containsKey(seriesID)) {
	    					HashMap<Integer, Long> fav = favs.get(seriesID);
	    					System.out.println(fav.toString());
							int seriesid = Integer.valueOf(String.valueOf(fav.get(1)));
							int lastchp = Integer.valueOf(String.valueOf(fav.get(2)));
							long last_timestamp = fav.get(3);
							
							System.out.println(seriesid + " - " + lastchp + " - " + last_timestamp + " | " + chapter);
							
							if (lastchp == 0) {
								MangaStream.updateFav(seriesID,chapter);
								fav.put(2, Long.valueOf(m.get("chapter")));
								favs.put(seriesID,fav);
							}
							else if (chapter > lastchp) {
		    					if (!detectedSeriesIDs.contains(seriesID)) { detectedSeriesIDs.add(seriesID); }
		    					if (!detectedData.containsKey(seriesID)) { detectedData.put(seriesID,m); }
		    					
		    					if (!detectedTimes.containsKey(seriesID)) {	detectedTimes.put(seriesID,1); }
		    					else {
		    						int Dt = detectedTimes.get(seriesID);
		    						detectedTimes.put(seriesID,(Dt+1));
		    					}    					
							}
	    				}
    				} catch(Exception e) { e.printStackTrace(); }
				}
				public void NewFavsNotifications() {
					System.out.println(detectedSeriesIDs.toString());
					if ( detectedSeriesIDs.size() == 0 ) { return; }
					try {
						for(int i = 0; i < detectedSeriesIDs.size(); i++) {
							int seriesID = detectedSeriesIDs.get(i);
							
							HashMap<String, String> m = detectedData.get(seriesID);
							int Dt = detectedTimes.get(seriesID);
							
	    					HashMap<Integer, Long> fav = favs.get(seriesID);
	    					
							int lastchp = Integer.valueOf(String.valueOf(fav.get(2)));
							int chapter = Integer.valueOf(m.get("chapter"));
							
							Intent viewMsgIntent = null;
							
							String taskBarNotification = "";
							String notiTitle = "";
							String notiDesc = "";
							
							long newChps = ( chapter - lastchp );

							if (newChps>1) {
								taskBarNotification = "Mangastream - " + newChps + " New " + m.get("series_name") + " Chapters Released";
								notiTitle = "Mangastream - " + newChps + " New " + m.get("series_name") + " Chapters Released";
								notiDesc = "There are " + newChps + " new " + m.get("series_name") + " chapters released";
								
								viewMsgIntent = new Intent(MSS.getBaseContext(), ChaptersBySeries.class);
								viewMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								viewMsgIntent.setAction("ChaptersBySeries" + m.get("series_name"));
								viewMsgIntent.putExtra("seriesID",String.valueOf(m.get("series_id")));
							} else {
								taskBarNotification = "Mangastream - " + m.get("series_name") + " Chp. " + m.get("chapter") + " Released";
								notiTitle = "Mangastream - " + m.get("series_name") + " Chp. " + m.get("chapter") + " Released";
								notiDesc = "Click here to read this chapter";
								
								viewMsgIntent = new Intent(MSS.getBaseContext(), PageView.class);
								viewMsgIntent.setAction("PageView" + m.get("series_name"));
								viewMsgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								viewMsgIntent.putExtra("chapID",m.get("manga_id"));
							}
							
							sendNotification(taskBarNotification,notiTitle,notiDesc,viewMsgIntent);
							
							MangaStream.updateFav(seriesID,chapter);
							break;
						}
					} catch(Exception e) { e.printStackTrace(); }
				}
			}, 0, calculatedINTERVAL);
		} catch (IllegalStateException e) {}
	}
	
	public static void stopservice() {
		if (timer != null){
			timer.cancel();
		}
	}
	
	public static void setMainActivity(MainMenu mainMenu) {
		  MAIN_ACTIVITY = mainMenu;
	}
		
	public static boolean NextPage() {
		if(get_chapter_pages == null || get_chapter_pages.size() == 0) { return false; }
		int total = get_chapter_pages.size();
		if (currentPage == (total-1)) { return false; }
		currentPage++;
		return true;
	}
	
	public static boolean PrevPage() {
		if(get_chapter_pages == null || get_chapter_pages.size() == 0) { return false; }
		if (currentPage <= 0) { return false; }
		currentPage--;
		return true;
	}
	
	public static String getError() {
		return error;
	}
	
	public static boolean getLatestChapters() {
		return MangaStream.get_chapters_latest(); 
	}
	
	public static boolean getChapter(String id) {
		return MangaStream.get_chapter(id);
	}
	
	public static boolean getSeries() {
		return MangaStream.get_series();
	}
	
	public static boolean getChaptersBySeries(String id) {
		return MangaStream.get_chapters_by_series(id);
	}
}
