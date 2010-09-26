package tv.doroga.android.widget;

import java.util.Timer;

import javax.microedition.lcdui.Graphics;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ZoomControls;

import com.nutiteq.BasicMapComponent;
import com.nutiteq.android.MapView;
import com.nutiteq.components.PlaceIcon;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.location.LocationSource;
import com.nutiteq.location.NutiteqLocationMarker;
import com.nutiteq.location.providers.AndroidGPSProvider;
import com.nutiteq.maps.BaseMap;
import com.nutiteq.maps.OpenStreetMap;
import com.nutiteq.ui.Copyright;
import com.nutiteq.ui.StringCopyright;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.utils.Utils;

public class JamsMapComponent {
	private BasicMapComponent mapComponent;
    private NutiteqLocationMarker locationMarker;
    private BaseMap map;
    private Timer refreshTimer;
    private DorogaTVAPI api;
    private Context context;
    
    private boolean followMe;
    private WgsPoint center;
    private int zoom;

    public boolean getFollowMe() {
   		return followMe;
    }

    public void setFollowMe(boolean f) {
   		followMe = f;
   		if( locationMarker != null )
   			locationMarker.setTrackingEnabled(f);
    }
    
    public WgsPoint getCenter() {
    	if( mapComponent == null )
    		return new WgsPoint(center.getLon(),center.getLat());
    	return mapComponent.getCenterPoint();
    }
    
    public void setCenter(WgsPoint c) {
    	center = new WgsPoint(c.getLon(),c.getLat());
    	if( mapComponent != null )
    		mapComponent.setMiddlePoint(c);
    }
    
    public int getZoom() {
    	if( mapComponent == null )
    		return zoom;
    	return mapComponent.getZoom();
    }

    public void setZoom(int z) {
    	zoom = z;
    	if( mapComponent != null )
    		mapComponent.setZoom(z);
    }
    
    public JamsMapComponent(Context _context) {
    	this.context = _context;
    	this.followMe = false;
    	this.center = new WgsPoint(44, 56.32);
    	this.zoom = 10; 
    }
    
    public BasicMapComponent getMapComponent() {
    	return mapComponent;
    }

    public void start() {
    	api = new DorogaTVAPI(context);
        map = new OpenStreetMap(new StringCopyright("."),"http://tile.openstreetmap.org/", 256, 1, 18);
        mapComponent = new BasicMapComponent("0266e33d3f546cb5436a10798e657d974c8b53482634a7.63929236","Doroga-TV","Doroga TV Widget", 1, 1,
        		center, zoom);
		mapComponent.setMap(map);
		//mapComponent.setPanningStrategy(new ThreadDrivenPanning());
        
		// TODO: Allow to follow by user, f.e. using the component in a message-loop thread
		/*
        // GPS Location
		final LocationSource locationSource = new AndroidGPSProvider(
				(LocationManager) context.getSystemService(Context.LOCATION_SERVICE), 1000L);
		locationMarker = new NutiteqLocationMarker(
				new PlaceIcon(
						Utils.createImage("/res/drawable/blue_marker.png"), 5, 17),
						3000, followMe);
		locationSource.setLocationMarker(locationMarker);
		mapComponent.setLocationSource(locationSource);
		locationMarker.setTrackingEnabled(followMe);
		*/
		mapComponent.startMapping();

		refreshTimer = new Timer();
        refreshTimer.schedule(new RefreshJamsTask(context,mapComponent), 1000, 1000*60/2);
    }

    public void stop() {
    	if( mapComponent != null ) {
    		center = mapComponent.getCenterPoint();
    		zoom = mapComponent.getZoom();
    		refreshTimer.cancel();
    		mapComponent.stopMapping();
    		api.close();
    	}
    	mapComponent = null;
        locationMarker = null;
        map = null;
        refreshTimer = null;
        api = null;
    }
}