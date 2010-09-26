package tv.doroga.android.widget;

import java.util.TimerTask;

import android.content.Context;

import com.nutiteq.BasicMapComponent;
import com.nutiteq.components.MapTile;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.maps.MapTileOverlay;

public class RefreshJamsTask extends TimerTask {
	private BasicMapComponent mapComponent;
	private Context context;
    private String currentJamsUrl;

    public RefreshJamsTask(Context context, BasicMapComponent mapComponent) {
		this.mapComponent = mapComponent;
		this.context = context;
		this.currentJamsUrl = "";
	}
	
    @Override
	public void run() {
		WgsPoint p = mapComponent.getCenterPoint();
		DorogaTVAPI api = new DorogaTVAPI(context);
		String baseurl = api.getDataProviderURL(p.getLon(),p.getLat());
		if( baseurl != null ) {
			String oldJamsUrl = currentJamsUrl;
			currentJamsUrl = baseurl + DorogaTVAPI.JAMS_TEMPLATE;
			if( currentJamsUrl != oldJamsUrl ) {
				mapComponent.getMap().addTileOverlay(new MapTileOverlay() {
					@Override
					public String getOverlayTileUrl(MapTile tile) {
						int tilex = tile.getX() >> 8;
						int tiley = tile.getY() >> 8;

						if( currentJamsUrl != null ) {
							String s = String.format(currentJamsUrl,tilex,tiley,tile.getZoom());  
							return s;
						}
						return null;
					}
		        });
			}

			mapComponent.refreshTileOverlay();
		}
		api.close();
	}
}
