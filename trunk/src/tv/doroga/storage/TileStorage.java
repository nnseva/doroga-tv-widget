package tv.doroga.storage;

import android.content.Context;

public interface TileStorage {
	public interface TileReceiver {
		void TileGot(int tilex, int tiley, int tilezoom, byte[] tilecontent, long tiletimestamp);
		void TileUnvailable(int tilex, int tiley, int tilezoom);
	}
	void RequestTile(int tilex, int tiley, int tilezoom, TileReceiver receiver);
}
