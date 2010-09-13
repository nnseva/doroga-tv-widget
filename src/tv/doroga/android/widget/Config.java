package tv.doroga.android.widget;

import java.util.Timer;
import java.util.TimerTask;

import com.nutiteq.BasicMapComponent;
import com.nutiteq.MapComponent;
import com.nutiteq.android.MapView;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.MapTile;
import com.nutiteq.components.OnMapElement;
import com.nutiteq.components.PlaceIcon;
import com.nutiteq.components.Point;
import com.nutiteq.components.WgsBoundingBox;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.controls.AndroidKeysHandler;

import com.nutiteq.maps.BaseMap;
import com.nutiteq.maps.CloudMade;
import com.nutiteq.maps.OpenStreetMap;
import com.nutiteq.maps.MapTileOverlay;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.utils.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.ZoomControls;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;

import com.nutiteq.listeners.MapListener;
import com.nutiteq.listeners.OnMapElementListener;
import com.nutiteq.location.LocationMarker;
import com.nutiteq.location.LocationSource;
import com.nutiteq.location.NutiteqLocationMarker;
import com.nutiteq.location.providers.AndroidGPSProvider;


import android.widget.CompoundButton.OnCheckedChangeListener;

public class Config extends Activity {
	public static String WIDGETS_TABLE = "widgets";
	
	private MapView mapView;
	private BasicMapComponent mapComponent;
    private ZoomControls zoomControls;
    private CheckBox followMe;
    private RadioGroup sizeSelector;
    private Button addButton;
    private NutiteqLocationMarker location_marker;
    private BaseMap map;
    private Timer refreshTimer;
    private DorogaTVAPI api;
    private String currentJamsUrl;
    private ProgressDialog progressDialog;
    private Handler resultHandler;

    // Correct cleanup
    private boolean onRetainCalled;

    @Override
    public Object onRetainNonConfigurationInstance() {
    	onRetainCalled = true;
    	return mapComponent;
    }
    
    @Override
    protected void onDestroy() {
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
    	if( api != null )
    		api.close();
    	super.onDestroy();
    }

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        api = new DorogaTVAPI(this);
        
        setContentView(R.layout.main);

        final Config config = this;
	    resultHandler = new Handler(){
	    	@Override
	    	public void handleMessage(Message message)
	    	{
	    		if( progressDialog != null )
	    			progressDialog.dismiss();
				AlertDialog.Builder alert = new AlertDialog.Builder(config);
				alert.setIcon(android.R.drawable.ic_dialog_alert);
				alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.dismiss();
					}
				});
	    		String result = message.getData().getString("RESULT");
	    		if (result.equals(DorogaTVAPI.RESULT_OK))
	    		{
	    			finish();
	    		}
	    		else
	    		{
	    			alert.setTitle(result);
		    		String msg = message.getData().getString("MESSAGE");
	    			alert.setMessage(msg);
	    			alert.show();
	    		}
	    	}
	    };

        map = new OpenStreetMap("http://tile.openstreetmap.org/", 256, 1, 18);
        mapComponent = new BasicMapComponent("0266e33d3f546cb5436a10798e657d974c8b53482634a7.63929236","Doroga-TV","Doroga TV Widget", 1, 1,
        		new WgsPoint(44, 56.32), 10);
		mapComponent.setMap(map);
		mapComponent.setPanningStrategy(new ThreadDrivenPanning());
		mapComponent.setControlKeysHandler(new AndroidKeysHandler());

		refreshTimer = new Timer();
        refreshTimer.schedule(
        		new TimerTask() {
					@Override
					public void run() {
						WgsPoint p = mapComponent.getCenterPoint();
						String baseurl = api.getDataProviderURL(p.getLon(),p.getLat());
						if( baseurl != null ) {
							String oldJamsUrl = currentJamsUrl;
							currentJamsUrl = baseurl + DorogaTVAPI.JAMS_TEMPLATE;
							if( currentJamsUrl != oldJamsUrl ) {
								map.addTileOverlay(new MapTileOverlay() {
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
							/*
							WgsBoundingBox bb = mapComponent.getBoundingBox();
							MapPos lb = map.wgsToMapPos(bb.getWgsMin().toInternalWgs(), mapComponent.getZoom());
							MapPos rt = map.wgsToMapPos(bb.getWgsMax().toInternalWgs(), mapComponent.getZoom());
							
							for(int left = (lb.getX() >> 8) << 8; left < rt.getX(); left += 256 ) {
								for(int top = (rt.getY() >> 8) << 8; top < lb.getY(); top += 256 ) {
									MapTile t = new MapTile(left, top, lb.getZoom(), map, mapComponent);
									mapComponent.updateTile(t);
								}
							}
							*/
						}
					}
        		}
        		, 0, 1000*60/2);

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
		// Add Radio Group
		sizeSelector = (RadioGroup) findViewById(R.id.size_selector);

		// Add OK Button
		addButton = (Button) findViewById(R.id.add_button);
		addButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				addWidget();
			}
		});

		mapComponent.startMapping();
    }
    
    void addWidget()
    {
    	Intent intent = getIntent();
    	if (intent.getExtras() != null)
		{
			int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
			{
				// create a record in the table
				prepareWidgets();
				int checkedId = sizeSelector.getCheckedRadioButtonId();
				RadioButton rb = (RadioButton) sizeSelector.findViewById(checkedId);
				int i = Integer.parseInt(rb.getTag().toString());
				appendWidget(appWidgetId,mapComponent.getCenterPoint().getLon(),mapComponent.getCenterPoint().getLat(),mapComponent.getZoom(),i,followMe.isChecked());
				// start sending update notifications to the widget manager
				Intent updaterIntent = new Intent();
				updaterIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				updaterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
				updaterIntent.setData(Uri.withAppendedPath(Uri.parse("doroga://widget/id/"), String.valueOf(appWidgetId))); // To have an intent be unique; note about Mainfest!!!
				PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, updaterIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
				alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 60000, pendingIntent);
				// prepare the result to return
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				setResult(RESULT_OK, resultValue);
				finish();
			}
		}
    }

	private void appendWidget(int appWidgetId, double lon, double lat, int zoom, int size, boolean followMe) {
        SQLiteDatabase widgets = (new DBOpener(this,"widgets.db",2)).getWritableDatabase();
		try
		{
			widgets.execSQL(String.format("INSERT INTO %s (widgetId,lon,lat,zoom,sizeSelector,followMe) VALUES (%d,%18.15f,%18.15f,%d,%d,%d) ",WIDGETS_TABLE,appWidgetId,lon,lat,zoom,size,followMe?1:0));
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "Config",e);
		}
		widgets.close();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private boolean checkIfTableExists(String table)
	{
		boolean r = true;
        SQLiteDatabase widgets = (new DBOpener(this,"widgets.db",2)).getReadableDatabase();
		try {
			widgets.rawQuery(String.format("select 0 from %s limit 1",table), null);
		} catch(Exception e) {
			r = false;
		}
		widgets.close();
		return r;
	}

	private void prepareWidgets()
	{
		try
		{
			if( !checkIfTableExists(WIDGETS_TABLE) ) {
				
				android.util.Log.i("DorogaTVWidget", String.format("Config - widgets table not yet created"));
				createWidgets();
			}
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "Config",e);
		}
	}

	private void createWidgets() {
        SQLiteDatabase widgets = (new DBOpener(this,"widgets.db",2)).getWritableDatabase();
		widgets.execSQL(new StringBuilder("DROP TABLE IF EXISTS ").append(WIDGETS_TABLE).toString());
		{
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE ").append(WIDGETS_TABLE).append(" (")
				.append("widgetId INTEGER")
				.append(",").append("lon DOUBLE")
				.append(",").append("lat DOUBLE")
				.append(",").append("zoom INTEGER")
				.append(",").append("sizeSelector INTEGER")
				.append(",").append("followMe INTEGER");
			sb.append(", PRIMARY KEY(widgetId) )");
			widgets.execSQL(sb.toString());
		}
		widgets.close();
	}
}