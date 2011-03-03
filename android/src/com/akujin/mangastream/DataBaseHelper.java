package com.akujin.mangastream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class DataBaseHelper extends SQLiteOpenHelper {

	//Default sqlite3 database name for this application.
    private static String DB_NAME = "mangastream.db";
 
    private SQLiteDatabase myDataBase; 
 
    private final Context myContext;
 
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
	public DataBaseHelper(Context context) {
		//Context context
		super(context, DB_NAME, null, 1);
		this.myContext = context;
	}
     
	 /**
	  * Constructor
	  * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
	  * @param context
	  * @param string - Database name file you would like to use.
	  */
	public DataBaseHelper(Context context, String NEW_DBNAME) {
		//Context context
		super(context, NEW_DBNAME, null, 1);
		this.myContext = context;
	}
 
    public DataBaseHelper open() throws SQLException {
 
    	//Open the database
        //String myPath = DB_NAME;
    	//myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    	this.myDataBase = this.getWritableDatabase();
    	return this;
    }
 
    @Override
	public void close() throws SQLException {
		System.out.println("DBH Close");
		//this.close();
		
		super.close();
	}
    
    public long statement(String statement, ArrayList<String[]> map) {
    	SQLiteStatement insertStmt = this.myDataBase.compileStatement(statement);
        if (map.size()>0) {
        	for(int i = 0; i < map.size(); i++) {
        		
	        	int key = (i+1);
	        	String[] parm = map.get(i);
	        	String type = parm[0];
	        	
	        	if (type == "int") { insertStmt.bindLong(key, Long.valueOf(parm[1])); }
	        	else if (type == "string") { insertStmt.bindString(key, String.valueOf(parm[1])); }
	        	
	        	System.out.println(parm[0] + " " + parm[1]);
	        	System.out.println("Key " + key + " + Parms: " + parm.toString());
	        }
        }
    	return insertStmt.executeInsert();
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS bookmarks (id INTEGER PRIMARY KEY ASC, series_id INTEGER, chapter_id INTEGER, timestamp INTEGER);");
		db.execSQL("CREATE TABLE IF NOT EXISTS favorites (id INTEGER PRIMARY KEY ASC, series_id, last_chapter_id INTEGER, timestamp INTEGER, UNIQUE (series_id));");
		
		//favorites: id series_id last_chapter_id timestamp
		//bookmarks: id series_id chapter_id page_id timestamp		
		/*try { db.execSQL("INSERT INTO settings (key, val) VALUES ('next', 'n');"); } catch (SQLiteException e) {}*/
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS bookmarks");
		db.execSQL("DROP TABLE IF EXISTS favorites");
	}

	public Cursor rawQuery(String sql, String[] selectionArgs) throws SQLException {
		return this.myDataBase.rawQuery(sql,selectionArgs);
	}
 
   // Add your public helper methods to access and get content from the database.
   // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
   // to you to create adapters for your views.
 
}