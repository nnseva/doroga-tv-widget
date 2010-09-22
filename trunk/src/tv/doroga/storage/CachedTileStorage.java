package tv.doroga.storage;

import java.text.SimpleDateFormat;

public abstract class CachedTileStorage implements TileStorage {
	private TileStorage base_storage;
	private int cache_timeout;
	public CachedTileStorage(TileStorage base, int timeout) {
		base_storage = base;
		cache_timeout = timeout;
	}

	public TileStorage BaseStorage() {
		return base_storage;
	}
	public int Timeout() {
		return cache_timeout;
	}

	public interface CachedTileReceiver {
		void TileGot(int tilex,int tiley, int tilezoom, byte [] content, long original_ts, long cached_ts); 
		void TileUnavailable(int tilex,int tiley, int tilezoom); 
	}

	public interface CachedTileWriter {
		byte [] GetTile(int tilex,int tiley, int tilezoom);
	}
	
	protected abstract void GetCachedTile(int tilex, int tiley, int tilezoom, CachedTileReceiver receiver);
	protected abstract void PutCachedTile(int tilex, int tiley, int tilezoom, byte [] content, long original_ts);

	private void RequestFromBaseStorage(int tilex, int tiley, int tilezoom, TileReceiver receiver)
	{
		final TileReceiver rcvr = receiver;
		BaseStorage().RequestTile(tilex, tiley, tilezoom, new TileReceiver() {
			@Override
			public void TileGot(int tilex, int tiley, int tilezoom, byte[] content, long original_ts) {
				PutCachedTile(tilex, tiley, tilezoom, content, original_ts);
				if( rcvr != null )
					rcvr.TileGot(tilex, tiley, tilezoom, content, original_ts);
			}

			@Override
			public void TileUnvailable(int tilex, int tiley, int tilezoom) {
				if( rcvr != null )
					rcvr.TileUnvailable(tilex, tiley, tilezoom);
			}
		});
	}
	
	@Override
	public void RequestTile(int tilex, int tiley, int tilezoom, TileReceiver receiver) {
		final TileReceiver rcvr = receiver;
		GetCachedTile(tilex,tiley,tilezoom, new CachedTileReceiver() {
			@Override
			public void TileGot(int tilex,int tiley, int tilezoom, byte [] content, long original_ts, long cached_ts) {
				SimpleDateFormat df = new SimpleDateFormat();
				long ts = df.getCalendar().getTime().getTime();
				//android.text.format.Time tm = new android.text.format.Time();
				//tm.setToNow();
				// return cached if present anyway
				rcvr.TileGot(tilex, tiley, tilezoom, content, original_ts);
				// request from the base cache if too old
				if( cached_ts < ts - Timeout() ) {
					// mark a tile to be normal to avoid double-requests
					PutCachedTile(tilex, tiley, tilezoom, content, original_ts);
					RequestFromBaseStorage(tilex,tiley,tilezoom,null);
				} else
					rcvr.TileGot(tilex, tiley, tilezoom, content, original_ts);
			}

			@Override
			public void TileUnavailable(int tilex, int tiley, int tilezoom) {
				// request from the base cache if not present
				RequestFromBaseStorage(tilex,tiley,tilezoom,rcvr);
			}
		});
		return;
	}
}
