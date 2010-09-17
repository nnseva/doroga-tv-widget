package tv.doroga.storage;

import android.content.Context;

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

	public abstract boolean GetTileHasChanged(Context context, int tilex, int tiley, int tilezoom, int ts);
}
