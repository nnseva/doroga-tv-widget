package tv.doroga.android.widget;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
//import android.graphics.drawable.Drawable;

public abstract class TileStorage {
	public class UrlCached { 
		
		public enum CachePolicy {
			BY_TIME, /// Caching for a period of time - default  
			BY_LAST_MODIFIED, /// Caching accordingly to last-modified response header
			BY_ETAG, /// Caching by checking ETag response header
			OSM_SPECIFIC /// Caching appending /status to the resource URL to request status
		}
	
		private CachePolicy cache_policy;
		private int request_period;
		private String tile_database_name;
		private String tile_database_table;
		private Thread update_thread;
		private HashMap<Long,Thread> request_threads;
		private boolean needs_to_stop;
		
		public TileStorage(String database_name, String database_table) {
			cache_policy = CachePolicy.BY_TIME;
			request_period = 5*60*1000; // 5 minutes
			tile_database_name = database_name;
			tile_database_table = database_table;
			needs_to_stop = false;
			request_threads = new HashMap<Long,Thread>(); 
		}
	
		public TileStorage(String database_name, String database_table, CachePolicy policy, int period) {
			cache_policy = policy;
			request_period = period;
			tile_database_name = database_name;
			tile_database_table = database_table;
			needs_to_stop = false;
			request_threads = new HashMap<Long,Thread>(); 
		}
		
		public synchronized byte[] GetTile(Context context, int tilex, int tiley, int tilezoom) {
			SQLiteDatabase db = new TileDatabaseOpener(context, this, tilezoom).getReadableDatabase();
			byte[] ret = null;
			Cursor c = db.rawQuery(String.format(
					"select rowid, tiletimestamp, tilecontent from %s "+
					"where tilex = %d and tiley = %d and tilezoom = %d",
					tile_database_table,tilex,tiley,tilezoom
					),null);
			if( c.getCount() > 0) {
				c.moveToFirst();
				// read content if possible, to return a drawable bitmap
				if( !c.isNull(c.getColumnIndex("tilecontent")) ) {
					//ret = Drawable.createFromStream( new ByteArrayInputStream( c.getBlob(c.getColumnIndex("tilecontent")) ), "tile.png" );
					ret = c.getBlob(c.getColumnIndex("tilecontent"));
				}
				// check if we need to issue the update request
				long rowid = c.getLong(c.getColumnIndex("rowid"));
				//android.text.format.Time t1;
				long ts = c.getLong(c.getColumnIndex("tiletimestamp"));
				android.text.format.Time tm = new android.text.format.Time();
				tm.setToNow();
				if( !request_threads.containsKey(rowid) ) {
					// we should request a status and make a tile request only if such requests not yet started
					if( tm.toMillis(true) - ts > request_period) {
						// we should request a status and make a tile request only if enough time has been passed after the last one
					}
				}
			}
			return ret;
		}
		
		public synchronized void start() {
			if( update_thread != null )
				return;
			needs_to_stop = false;
			update_thread = new Thread(new Runnable(){
				@Override
				public void run() {
					while(!needs_to_stop) {
						// 
						
						try {
							Thread.sleep(request_period);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			});
		}
		
		public synchronized void stop() {
			needs_to_stop = true;
			Iterator<Long> i = request_threads.keySet().iterator();
			while(i.hasNext()) {
				long key;
				Thread t;
				synchronized(request_threads) {
					key = i.next();
					t = request_threads.get(key);
				}
				try {
					t.join(100);
				} catch (InterruptedException e) {
				}
				if( t.isAlive() ) {
					t.interrupt();
				}
				request_threads.remove(key);
			}
			try {
				update_thread.join(100);
			} catch (InterruptedException e) {
			}
			if( update_thread.isAlive() ) {
				update_thread.interrupt();
			}
			update_thread = null;
		}
		
		public String DatabaseName() {
			return tile_database_name;
		}
	
		public String TableName() {
			return tile_database_table;
		}
		
		public boolean IsStarted() {
			return !needs_to_stop && update_thread != null;
		}
		
		abstract protected String CreateUrlFor(int tilex, int tiley, int tilezoom);
		
		public class Templated extends TileStorage {
			private String resourse_url_template;
			Templated(String database_name, String database_table, String url_template) {
				super(database_name,database_table);
				resourse_url_template = url_template;
			}
			Templated(String database_name, String database_table, String url_template, CachePolicy policy, int period) {
				super(database_name,database_table,policy,period);
				resourse_url_template = url_template;
			}
			protected String CreateUrlFor(int tilex, int tiley, int tilezoom) {
				return String.format(resourse_url_template, tilex, tiley, tilezoom);
			}
		}
		
		protected class TileDatabaseOpener extends SQLiteOpenHelper {
			private TileStorage tile_storage;
			private static final String create_tile_table_template = "CREATE TABLE %s (tilex integer not null, tiley integer not null, tilezoom integer not null, tiletimestamp integer, tilecontent blob, unique key(tilex,tiley,tilezoom) )";
			public TileDatabaseOpener(Context context, TileStorage storage, int version) {
				super(context, storage.DatabaseName(), null, version);
				tile_storage = storage;
			}
	
			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL(String.format(
						create_tile_table_template,
						tile_storage.TableName()
						));
			}
	
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				db.execSQL(String.format(
						"DROP TABLE %s IF EXISTS",
						tile_storage.TableName()
						));
				db.execSQL(String.format(
						create_tile_table_template,
						tile_storage.TableName()
						));
			}
		}
	}
}
