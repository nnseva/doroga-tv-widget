package tv.doroga.storage;

import java.text.SimpleDateFormat;
import java.util.TreeMap;

public class MemCachedTileStorage extends CachedTileStorage {
	public class CacheEntry {
		byte [] content;
		long original_ts;
		long cached_ts;
		public long reverse;
	}

	private TreeMap<Long,CacheEntry> cache;
	private TreeMap< Long,Long > reverse_cache;
	private int cache_maxtiles;

	private long CachedKey(int tilex, int tiley, int tilezoom)
	{
		return (tilezoom << 40) | (tiley << 20) | tilex;
	}

	@Override
	protected void GetCachedTile(int tilex, int tiley, int tilezoom, CachedTileReceiver receiver)
	{
		long key = CachedKey(tilex,tiley,tilezoom); 
		if( cache.containsKey(key) ) {
			CacheEntry entry = cache.get(key);
			receiver.TileGot(tilex, tiley, tilezoom, entry.content, entry.original_ts, entry.cached_ts);
		} else {
			receiver.TileUnavailable(tilex, tiley, tilezoom);
		}
	}

	@Override
	protected synchronized void PutCachedTile(int tilex, int tiley, int tilezoom, byte [] content, long original_ts)
	{
		SimpleDateFormat df = new SimpleDateFormat();
		long ts = df.getCalendar().getTime().getTime();
		long key = CachedKey(tilex,tiley,tilezoom);
		if( cache.containsKey(key)) {
			CacheEntry entry = cache.get(key);
			
			long to_remove = entry.reverse;
			entry.reverse = reverse_cache.lastKey()+1;

			entry.content = content;
			entry.cached_ts = ts;
			
			reverse_cache.put(entry.reverse, key);
			reverse_cache.remove(to_remove);
			return;
		}
		CacheEntry entry = new CacheEntry();
		
		entry.content = content;
		entry.cached_ts = ts;
		entry.reverse = reverse_cache.size() > 0 ? reverse_cache.lastKey()+1:1;
		entry.original_ts = original_ts;

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
}