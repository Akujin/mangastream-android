package com.akujin.mangastream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

/*	@Author	Henry Paradiz
 * 	@data	12/26/2009
 * 	
 * 	Documentation for API @ http://mangastream.com/api/documentation
 *  
 *  
 *  
 */


public class MangaStream {
	private static String API_KEY 			= "0e6f1c793b875733626e7fc9091c8b81";
	private static String API_POST_URL 	= "http://mangastream.com/api";
	
	private static boolean API_LENGTH_CHECK = false;
	
	/* 
	 * 
	 * 
	 */
	public static boolean download_file(String url, String destination) {
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MangaStreamService.MAIN_ACTIVITY.getApplicationContext());
		
		try {
			File file = new File(destination);
			//file.setLastModified(1270094400);
			
			if(file.exists() && file.length() == 0) {
	        	file.delete();
	        } else if (file.exists()) {
	        	
				try {
			    	Date now = new Date();
					long nowtime = now.getTime();
					
					//System.out.println("Checking file timestamp: " + destination);
					
					long duration = ( nowtime - file.lastModified() ) / 1000;
					if (duration<86400) {
						file.setLastModified(nowtime);
						return true;
					}
	
					//System.out.println("File is older then a day, checking etag: " + destination);
					
			        DefaultHttpClient httpclient = new DefaultHttpClient();
			        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
	
			        //We MUST set the expect-continue to false or the webservers will give a 417 error.
			        httpclient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			        
			        HttpContext localContext = new BasicHttpContext();
	
			        HttpHead httphead = new HttpHead(url);
			        httphead.setHeader("User-Agent", "Mangastream for Android/"+MangaStreamService.appVersion);
			        httphead.setHeader("Accept", "text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
			        
			        HttpResponse response = httpclient.execute(httphead,localContext);
	
			        //Check header for a 200 response, if we don't get it we output an error; 400 Errors are API errors.
			        int status_code = response.getStatusLine().getStatusCode();
			        if (status_code != 200) {
						return false;
					}
			        
			        Header etagH = response.getFirstHeader("ETag");
			        String etagV = etagH.getValue();
			        String savedetag = etagV.substring(1, (etagV.length()-1));
			        
			        String etag = settings.getString("etag_" + destination, "");
	
			        if (savedetag.equalsIgnoreCase(etag)) {
			        	
			        	//System.out.println("Etag matches for: " + url);
			        	
			        	file.setLastModified(nowtime);
			        	return true;
			        } else {
			        	//System.out.println("Etag does not match, redownloading: " + url);
			        	file.delete();
			        }
			        
			        //System.out.println(etag);
			        
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			if (file.exists()) {
				return true;
			}
			
		} catch(Exception e) {}
		
		System.out.println("Downloading " + url + " to " + destination);

		try {
	        DefaultHttpClient httpclient = new DefaultHttpClient();
	        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);

	        //We MUST set the expect-continue to false or the webservers will give a 417 error.
	        httpclient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
	        
	        HttpContext localContext = new BasicHttpContext();

	        HttpGet httpget = new HttpGet(url);
	        httpget.setHeader("User-Agent", "Mangastream for Android/"+MangaStreamService.appVersion);
	        httpget.setHeader("Accept", "text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
	        
	        HttpResponse response = httpclient.execute(httpget,localContext);

	        //Check header for a 200 response, if we don't get it we output an error; 400 Errors are API errors.
	        int status_code = response.getStatusLine().getStatusCode();
	        if (status_code != 200) {
				return false;
			}
	        
	        /*Header etag = response.getFirstHeader("ETag");
	        String etagvalue = etag.getValue();
	        System.out.println(etagvalue);*/
	        
	        InputStream in = response.getEntity().getContent();
	        
	        //Check for gzip Content-Encoding headers, As of time of writing, server returns null, as gzip isn't enabled.
	        //Header contentEncoding = response.getFirstHeader("Content-Encoding");
	    	
		    FileOutputStream f = new FileOutputStream(new File(destination));
			
		    byte[] buffer = new byte[1024];
		    int len1 = 0;
		    while ( (len1 = in.read(buffer)) != -1 ) {
		      f.write(buffer,0, len1);
		    }

		    f.close();
	        
		    File checkFile = new File(destination);
		    long filesize = checkFile.length();
		    long contentlength = response.getEntity().getContentLength();
		    
		    //System.out.println("Local Filesize: " + filesize + ", Remote Filesize: " + contentlength);
		    
		    if (filesize != contentlength) {
		    	checkFile.delete();
		    	return false;
		    } else {
		    	SharedPreferences.Editor editor = settings.edit();
		    				    	
				Header etagH = response.getFirstHeader("ETag");
		        String etagV = etagH.getValue();
		        String etag = etagV.substring(1, (etagV.length()-1));
		        
				editor.putString("etag_" + destination, etag);
				
				editor.commit();
		    	return true;
		    }
		}
		catch(Exception e) {
			e.printStackTrace();
		}
			
		return false;
	}
	
	public static String make_call(String call_name,Map<String,String> params) {
		String ret = null;
	    try {
	        DefaultHttpClient httpclient = new DefaultHttpClient();
	        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);

	        //We MUST set the expect-continue to false or the webservers will give a 417 error.
	        httpclient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
	        
	        HttpContext localContext = new BasicHttpContext();

	        HttpPost httppost = new HttpPost(API_POST_URL);
	        httppost.addHeader("Accept-Encoding", "gzip");
	        httppost.setHeader("User-Agent", "Mangastream for Android/"+MangaStreamService.appVersion);
	    	httppost.setHeader("Accept", "text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
	    	httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
	    	
	        ArrayList pairs = new ArrayList(); 
	        pairs.add(new BasicNameValuePair("apikey", API_KEY));
	        pairs.add(new BasicNameValuePair("call", call_name));
	        if(API_LENGTH_CHECK) {
	        	pairs.add(new BasicNameValuePair("length", "1"));
	        }
	        
	        //More params?
	        if( params != null ) {
	            Set<String> parameters = params.keySet();
	            Iterator it = parameters.iterator();
	            StringBuffer buf = new StringBuffer();

	            for( int i = 0, paramCount = 0; it.hasNext(); i++ ) {
	              String parameterName = (String) it.next();
	              String parameterValue = (String) params.get( parameterName );

	              if( parameterValue != null ) {
	                pairs.add(new BasicNameValuePair(parameterName, parameterValue));
	                ++paramCount;
	              }
	            }
	        }
	        
	        
	        UrlEncodedFormEntity p_entity = new UrlEncodedFormEntity(pairs, "UTF-8"); 
	        httppost.setEntity(p_entity);
	        
	        HttpResponse response = httpclient.execute(httppost,localContext);

	        //Check header for a 200 response, if we don't get it we output an error; 400 Errors are API errors.
	        int status_code = response.getStatusLine().getStatusCode();
	        if (status_code == 400) {
	        	MangaStreamService.error = "Mangastream API Error, Please report to application creator.";
				return null;
	        }
	        else if (status_code != 200) {
				MangaStreamService.error = "Mangastream Server Error, Please try again later. Error Code: " + status_code;
				return null;
			}

	        InputStream instream = response.getEntity().getContent();
	        
	        //Check for gzip Content-Encoding headers, As of time of writing, server returns null, as gzip isn't enabled.
	        Header contentEncoding = response.getFirstHeader("Content-Encoding");
	        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
	            instream = new GZIPInputStream(instream);
	        }
			
	        // Get the response
	        BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
	        String line;
		    try {
		        while ((line = rd.readLine()) != null) {
		            // Process line...
		        	if(ret == null) {
		        		ret = line.toString();
		        	} else {
		        		ret += line.toString();
		        	}
		        }
		    } finally {
	            try {
	            	rd.close();
	            	instream.close();
	            } catch (IOException e) {
	                 e.printStackTrace();
	            }
	       }

		   //System.out.println(ret);
		    
		   if (ret.length() == 0) {
			   MangaStreamService.error = "Mangastream Server Error, Please try again later. Error Code: MS0";
			   return null;
		   } else {
			 //Crappy way, but it makes sure that the response is an expected json string that begins with [ and ends with ]
			   int R1 = ret.substring(0,1).hashCode();
			   int R2 = ret.substring((ret.length()-1),ret.length()).hashCode();
			   if (!(R1 == 91 && R2 == 93)) {
				   MangaStreamService.error = "Mangastream Server Error, Please try again later. Error Code: MS1";
				   return null;
			   }
		   }
	    }
	    catch (Exception e) {
	    	MangaStreamService.error = e.getMessage();
	    	e.printStackTrace();
	    }

	    return ret;
	}
	
	public static boolean get_chapter(String id) {
		Map<String, String> post = new HashMap<String, String>();
		post.put("manga_id",id);
		
		String json = make_call("get_chapter",post);
		if(json==null) { return false; }
		
		MangaStreamService.get_chapter.clear();
		MangaStreamService.get_chapter_pages.clear();
		
		try {
			JSONTokener as = new JSONTokener(json);
			JSONArray bleh = new JSONArray(as);
			JSONObject row = bleh.getJSONObject(0);

			Iterator it = row.keys();

			while(it.hasNext()) {
				String key = it.next().toString();
				if (key.equalsIgnoreCase("pages")) {
					
					JSONArray pgArray = new JSONArray(new JSONTokener(row.get(key).toString()));
					 for(int pgi = 0; pgi < pgArray.length(); pgi++) {
						 JSONObject pgrow = pgArray.getJSONObject(pgi);
						 Iterator pgit = pgrow.keys();
						 
						 HashMap<String, String> pgmap = new HashMap <String,String>();
						 while(pgit.hasNext()) {
							 String pgkey = pgit.next().toString();
							 pgmap.put(pgkey, pgrow.get(pgkey).toString());
						 }
						 MangaStreamService.get_chapter_pages.add(pgmap);
					 }
					 
				} else {
					MangaStreamService.get_chapter.put(key, row.get(key).toString());
				}
			}

			System.out.println(MangaStreamService.get_chapter.toString());
			return true;
		}
		catch(JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean get_chapters_latest() {
		String json = make_call("get_chapters_latest",null);
		if(json==null) { return false; }
		
		MangaStreamService.get_chapters_latest.clear();
		
		try {
		   JSONTokener as = new JSONTokener(json);
		   JSONArray bleh = new JSONArray(as);

		   for(int i = 0; i < bleh.length(); i++) {
		    JSONObject row = bleh.getJSONObject(i);
		    Iterator it = row.keys();
		    
		    HashMap<String, String> map = new HashMap <String,String>();
		    while(it.hasNext()) {
				String key = it.next().toString();
				map.put(key, row.get(key).toString());
		    }
		    MangaStreamService.get_chapters_latest.add(map);

		   }
		   
		   return true;
		}
		catch(JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean get_chapters_by_series(String id) {
		Map<String, String> post = new HashMap<String, String>();
		post.put("series_id",id);
		
		String json = make_call("get_chapters_by_series",post);
		if(json==null) { return false; }
		
		MangaStreamService.get_chapters_by_series.clear();
		
		try {
		   JSONTokener as = new JSONTokener(json);
		   JSONArray bleh = new JSONArray(as);

		   for(int i = 0; i < bleh.length(); i++) {
		    JSONObject row = bleh.getJSONObject(i);
		    Iterator it = row.keys();
		    
		    HashMap<String, String> map = new HashMap <String,String>();
		    while(it.hasNext()) {
				String key = it.next().toString();
				map.put(key, row.get(key).toString());
		    }
		    MangaStreamService.get_chapters_by_series.add(map);

		   }
		   
		   return true;
		}
		catch(JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean get_series() {
		String json = make_call("get_series",null);
		if(json==null) { return false; }
		
		MangaStreamService.get_series.clear();
		
		try {
		   JSONTokener as = new JSONTokener(json);
		   JSONArray bleh = new JSONArray(as);

		   for(int i = 0; i < bleh.length(); i++) {
		    JSONObject row = bleh.getJSONObject(i);
		    Iterator it = row.keys();
		    
		    HashMap<String, String> map = new HashMap <String,String>();
		    while(it.hasNext()) {
				String key = it.next().toString();
				map.put(key, row.get(key).toString());
		    }
		    MangaStreamService.get_series.add(map);

		   }
		   
		   return true;
		}
		catch(JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	//
    public static boolean updateFav(int seriesid,int chapter2) {
	    ArrayList<String[]> map = new ArrayList<String[]>();
	    	    
	    String[] chapter = {"int",String.valueOf(chapter2)};
	    map.add(chapter);
	    
	    String[] timestamp = {"int",String.valueOf(new Date().getTime())};
	    map.add(timestamp);
	    
	    String[] series_id = {"int",String.valueOf(seriesid)};
	    map.add(series_id);
	    
	    try {
	    	MangaStreamService.myDbHelper.statement("update favorites set last_chapter_id = ?, timestamp = ? WHERE series_id = ?",map);
	    } catch(Exception e) {e.printStackTrace();}
	    
	    return true;
    }
    public static boolean addFav(String seriesID) {
	    ArrayList<String[]> map = new ArrayList<String[]>();
	    
	    String[] series_id = {"int",String.valueOf(seriesID)};
	    map.add(series_id);
	    
	    String[] timestamp = {"int","0"};
	    map.add(timestamp);
	    
	    try {
	    	MangaStreamService.myDbHelper.statement("insert into favorites (series_id,timestamp) values (?,?)",map);
	    } catch(Exception e) {e.printStackTrace();}
	    
	    return true;
    }
    
    public static boolean delFav(String seriesID) {
	    ArrayList<String[]> map = new ArrayList<String[]>();
	    
	    String[] series_id = {"int",String.valueOf(seriesID)};
	    map.add(series_id);

	    try {
	    	MangaStreamService.myDbHelper.statement("delete from favorites where series_id = ?",map);
	    } catch(Exception e) {e.printStackTrace();}
	    
	    return true;
    }
    
    public static ArrayList<Long> getFavsIDs() {
        Cursor c = MangaStreamService.myDbHelper.rawQuery("SELECT series_id FROM favorites", null);
        
        ArrayList<Long> favs = new ArrayList<Long>();
        
        int series_idColumn = c.getColumnIndex("series_id");

        /* Check if our result was valid. */
        if (c != null) {
             /* Check if at least one Result was returned. */
        	if(c.moveToFirst()){
                  int count = c.getCount();
                  
                  /* Loop through all Results */
                  for(int i=0; i<count; i++){
                       /* Retrieve the values of the Entry
                        * the Cursor is pointing to. */
                       long keyName = c.getLong(series_idColumn);
                       favs.add(keyName);
                       System.out.println(keyName);
                       
                       c.moveToNext();
                  };
             }
        }
        c.close();
        return favs;
    }
}
