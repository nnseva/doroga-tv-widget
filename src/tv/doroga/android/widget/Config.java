package tv.doroga.android.widget;

import java.util.Timer;
import java.util.TimerTask;

import com.nutiteq.BasicMapComponent;
import com.nutiteq.MapComponent;
import com.nutiteq.android.MapView;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.MapTile;
import com.nutiteq.components.PlaceIcon;
import com.nutiteq.components.Point;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.controls.AndroidKeysHandler;

import com.nutiteq.maps.BaseMap;
import com.nutiteq.maps.CloudMade;
import com.nutiteq.maps.OpenStreetMap;
import com.nutiteq.maps.MapTileOverlay;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;

import com.nutiteq.location.LocationMarker;
import com.nutiteq.location.LocationSource;
import com.nutiteq.location.NutiteqLocationMarker;
import com.nutiteq.location.providers.AndroidGPSProvider;


import android.widget.CompoundButton.OnCheckedChangeListener;

public class Config extends Activity {
	private MapView mapView;
	private BasicMapComponent mapComponent;
    private ZoomControls zoomControls;
    private CheckBox followMe;
    private Button addButton;
    private NutiteqLocationMarker location_marker;
    private BaseMap map;
    private Timer refreshTimer;

    // Correct cleanup
    private boolean onRetainCalled;

    @Override
    public Object onRetainNonConfigurationInstance() {
    	onRetainCalled = true;
    	return mapComponent;
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if( refreshTimer != null ) {
    		refreshTimer.cancel();
    	}
    	if (mapView != null) {
    		mapView.clean();
    		mapView = null;
    	}
    	if (!onRetainCalled) {
    		mapComponent.stopMapping();
    		mapComponent = null;
    	}
    }

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        map = new OpenStreetMap("http://tile.openstreetmap.org/", 256, 1, 18);
        mapComponent = new BasicMapComponent("0266e33d3f546cb5436a10798e657d974c8b53482634a7.63929236","Doroga-TV","Doroga TV Widget", 1, 1,
        		new WgsPoint(44, 56.32), 10);
		mapComponent.setMap(map);
		mapComponent.setPanningStrategy(new ThreadDrivenPanning());
		mapComponent.setControlKeysHandler(new AndroidKeysHandler());

		map.addTileOverlay(new MapTileOverlay() {
			@Override
			public String getOverlayTileUrl(MapTile tile) {
				// TODO Auto-generated method stub
				//Point p = tile.getMap().mapPosToWgs(new MapPos(tile.getX(),tile.getY(),tile.getZoom()));
				int tilex = tile.getX() >> 8;
				int tiley = tile.getY() >> 8;
				
				String s = String.format("http://nnov.doroga.tv/makeTraficImg.php?x=%2$d&y=%3$d&Level=%4$d",0,tilex,tiley,tile.getZoom());  
				return s;
				//return null;
			}
        });
		refreshTimer = new Timer();
        refreshTimer.schedule(
        		new TimerTask() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						//int zoom = mapComponent.getZoom();
				        //mapComponent.setZoom(zoom+1);
				        //mapComponent.setZoom(zoom);
					}
        		}
        		, 1000*60*1, 1000*60*1);

        mapView = new MapView(this, mapComponent);
				
		{ // adding map view to the box
			RelativeLayout map_view_layout = (RelativeLayout) findViewById(R.id.map_view_layout);
			final RelativeLayout.LayoutParams mapViewLayoutParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.FILL_PARENT,
					RelativeLayout.LayoutParams.FILL_PARENT);
			map_view_layout.addView(mapView, mapViewLayoutParams);
		}

		//Add ZoomControls
		zoomControls = (ZoomControls) findViewById(R.id.zoom_control);
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapComponent.zoomIn();
		}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapComponent.zoomOut();
			}
		});
		// GPS Location
		final LocationSource locationSource = new AndroidGPSProvider(
				(LocationManager) getSystemService(Context.LOCATION_SERVICE), 1000L);

		location_marker = new NutiteqLocationMarker(
				new PlaceIcon(
						Utils.createImage("/res/drawable/blue_marker.png"), 5, 17),
						3000, false);
		locationSource.setLocationMarker(location_marker);
		mapComponent.setLocationSource(locationSource);

		// Follow Me check box
		followMe = (CheckBox) findViewById(R.id.follow_me);
		followMe.setOnCheckedChangeListener(
			new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked)
				{
					location_marker.setTrackingEnabled(isChecked);
				}
		});
		mapComponent.startMapping();

		// Add OK Button
		//addButton = new Button(this);
    }
}