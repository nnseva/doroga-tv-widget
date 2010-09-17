package tv.doroga.storage;

import android.content.Context;

public interface TileStorage {
	public byte[] GetTile(Context context, int tilex, int tiley, int tilezoom);
}
