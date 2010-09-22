package tv.doroga.storage;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQliteCachedTileStorage extends CachedTileStorage {
	private String tile_database_name;
	private String tile_database_table;
	private int cache_maxtiles;
	private Context database_context;

	@Override
	protected synchronized void GetCachedTile(int tilex, int tiley, int tilezoom,CachedTileReceiver receiver)
	{
		SQLiteDatabase cache = new TileDatabaseOpener(database_context, this, 1).getWritableDatabase();
		Cursor c = cache.rawQuery(String.format("select * from %s where tilex = %d and tiley = %d and tilezoom = %d",tile_database_table,tilex,tiley,tilezoom), null);
		byte [] content = null;
		long original_ts = 0;
		long cached_ts = 0;
		if( c.getCount() > 0 ) {
			content = c.getBlob(c.getColumnIndex("tilecontent"));
			original_ts = c.getLong(c.getColumnIndex("original_ts"));
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				cached_ts = df.parse(c.getString(c.getColumnIndex("cached_timestamp"))).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		cache.close();
		if( content != null )
			receiver.TileGot(tilex, tiley, tilezoom, content, original_ts, cached_ts);
		else
			receiver.TileUnavailable(tilex, tiley, tilezoom);
	}

	@Override
	protected synchronized void PutCachedTile(int tilex, int tiley, int tilezoom, byte [] content, long original_ts)
	{
		SQLiteDatabase cache = new TileDatabaseOpener(database_context, this, 1).getWritableDatabase();
		ContentValues c = new ContentValues();
		c.put("tilex", tilex);
		c.put("tiley", tiley);
		c.put("tilezoom", tilezoom);
		c.put("original_ts", original_ts);
		c.put("tilecontent", content);
		
		cache.insert(tile_database_table, null, c);
		cache.close();
	}

	protected class TileDatabaseOpener extends SQLiteOpenHelper {
		private SQliteCachedTileStorage tile_storage;
		private static final String create_tile_table_template = "CREATE TABLE %s (tilex integer not null, tiley integer not null, tilezoom integer not null, original_ts integer, cached_timestamp varchar(40) default current_timestamp, tilecontent blob, unique key(tilex,tiley,tilezoom) )";
		public TileDatabaseOpener(Context context, SQliteCachedTileStorage storage, int version) {
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

	public String DatabaseName() {
		return tile_database_name;
	}

	public String TableName() {
		return tile_database_table;
	}

	SQliteCachedTileStorage(Context context, TileStorage base, String database_name, String database_table, int timeout, int maxtiles) {
		super(base,timeout);
		tile_database_name = database_name;
		tile_database_table = database_table;
		cache_maxtiles = maxtiles;
		database_context = context;
	}
}