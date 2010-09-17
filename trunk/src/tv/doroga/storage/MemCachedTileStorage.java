package tv.doroga.storage;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import android.content.Context;

public class MemCachedTileStorage extends CachedTileStorage {
	private class CacheEntry {
		public byte [] content;
		public long ts;
		public long reverse;
	}
	private TreeMap<Long,CacheEntry> cache;
	private TreeMap< Long,Long > reverse_cache;
	private int cache_maxtiles;

	private long CachedKey(int tilex, int tiley, int tilezoom)
	{
		return (tilezoom << 40) | (tiley << 20) | tilex;
	}

	private boolean HasCached(int tilex, int tiley, int tilezoom)
	{
		return cache.containsKey(CachedKey(tilex,tiley,tilezoom));
	}

	private CacheEntry GetCachedEntry(int tilex, int tiley, int tilezoom)
	{
		long key = CachedKey(tilex,tiley,tilezoom); 
		if( cache.containsKey(key) ) {
			CacheEntry entry = cache.get(key);
			return entry;
		}
		return null;
	}

	private synchronized void PutCachedTile(int tilex, int tiley, int tilezoom, byte [] content)
	{
		android.text.format.Time tm = new android.text.format.Time();
		tm.setToNow();
		long key = CachedKey(tilex,tiley,tilezoom);
		if( cache.containsKey(key)) {
			CacheEntry entry = cache.get(key);
			
			long to_remove = entry.reverse;
			entry.reverse = reverse_cache.lastKey()+1;

			entry.content = content;
			entry.ts = tm.toMillis(false);
			
			reverse_cache.put(entry.reverse, key);
			reverse_cache.remove(to_remove);
			return;
		}
		CacheEntry entry = new CacheEntry();
		
		entry.content = content;
		entry.ts = tm.toMillis(false);
		entry.reverse = reverse_cache.size() > 0 ? reverse_cache.lastKey()+1:1;

		cache.put(key,entry);
		reverse_cache.put(entry.reverse, key);
		
		if(cache.size() > cache_maxtiles ) {
			// TODO!
		}
	}
	
	MemCachedTileStorage(TileStorage base, int timeout, int maxtiles) {
		super(base,timeout);
		cache = new TreeMap<Long,CacheEntry>();
		reverse_cache = new TreeMap<Long,Long>();
		cache_maxtiles = maxtiles;
	}

	@Override
	public synchronized byte[] GetTile(Context context, int tilex, int tiley, int tilezoom) {
		CacheEntry entry =  GetCachedEntry(tilex, tiley, tilezoom);

		android.text.format.Time tm = new android.text.format.Time();
		tm.setToNow();

		if( entry != null ) {
			if( entry.ts > tm.toMillis(false) - Timeout() ) {
				return entry.content;
			} else {
				if( !BaseStorage().GetTileHasChanged(context, tilex, tiley, tilezoom, entry.ts) ) {
					
				}
			}
		}
		byte [] lower = BaseStorage().GetTile(context, tilex, tiley, tilezoom);
		if( lower != null ) {
			PutCachedTile(tilex, tiley, tilezoom, lower);
		}
		return lower;
	}

	@Override
	public boolean GetTileHasChanged(Context context, int tilex, int tiley,
			int tilezoom, int ts) {
		// TODO Auto-generated method stub
		return false;
	}
}
