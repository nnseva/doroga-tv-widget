package tv.doroga.android.widget;

import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.microedition.lcdui.Graphics;

import com.nutiteq.components.WgsPoint;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class JamsWidgetUpdateService extends Service {
	private Timer processTimer;
	private TreeMap<Integer,JamsMapComponent> mapComponents;
	private boolean started;

	@Override
    public void onStart(Intent intent, int startId) {
        if( started )
        	return;
        Log.d("UpdateService", "Started");
        started = true;
		mapComponents = new TreeMap<Integer,JamsMapComponent>();
        final Context context = this;
        startMapComponents();
        processTimer = new Timer();
        processTimer.schedule(new TimerTask() {
			@Override
			public void run() {
		        if( !started )
		        	return;
				try {
					// TODO: some desinchronization might happen with widgetIds lost from deleteWidget 
					SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getReadableDatabase();
    				Cursor c = widgets.rawQuery(String.format("select * from %s",Config.WIDGETS_TABLE), null);
    				for(c.moveToFirst(); !c.isAfterLast();c.moveToNext()) {
    					int widgetId = c.getInt(c.getColumnIndex("widgetId"));
    					int sizeSelector = c.getInt(c.getColumnIndex("sizeSelector"));
    					JamsMapComponent component = null;
    					synchronized(mapComponents) {
	    					if( !mapComponents.containsKey(widgetId) ) {
	    						component = createJamsComponent(c);
	    					} else {
	    						component = mapComponents.get(widgetId);
	    					}
    					}
						int layout_id = 1;
						switch(sizeSelector) {
						case 1: // 1x1
							layout_id = R.layout.widget1x1;
							break;
						case 2: // 2x1
							layout_id = R.layout.widget2x1;
							break;
						case 3: // 2x2
							layout_id = R.layout.widget2x2;
							break;
						case 4: // 4x2
							layout_id = R.layout.widget4x2;
							break;
						}
						
						Bitmap bitmap = Bitmap.createBitmap(component.getMapComponent().getWidth(), component.getMapComponent().getHeight(), Bitmap.Config.ARGB_8888);
						Canvas canvas = new Canvas(bitmap);
						Graphics graphics = new Graphics(canvas);
						
    					component.getMapComponent().paint(graphics);
						RemoteViews views = new RemoteViews(context.getPackageName(), layout_id);
						views.setImageViewBitmap(R.id.icon, bitmap);
						AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
						appWidgetManager.updateAppWidget(widgetId, views);
    				}
    				c.close();
    				widgets.close();
    				Log.i("UpdateService", "Widget update finished successfully");
				} catch(Exception e) {
					Log.e("UpdateService", "The service circle error:", e);
				}
			}
		},1000,5000);
    }

	public JamsWidgetUpdateService() {
		super();
	}
	
	private JamsMapComponent createJamsComponent(Cursor c) {
		int widgetId = c.getInt(c.getColumnIndex("widgetId"));
		double lon = c.getDouble(c.getColumnIndex("lon"));
		double lat = c.getDouble(c.getColumnIndex("lat"));
		int zoom = c.getInt(c.getColumnIndex("zoom"));
		int sizeSelector = c.getInt(c.getColumnIndex("sizeSelector"));
		boolean followMe = c.getInt(c.getColumnIndex("followMe")) > 0;
		return createJamsComponent(widgetId,lon,lat,zoom,sizeSelector,followMe);
	}

	private JamsMapComponent createJamsComponent(int widgetId, double lon, double lat, int zoom, int sizeSelector, boolean followMe) {
		JamsMapComponent component = new JamsMapComponent(this);
		synchronized(mapComponents) {
			mapComponents.put(widgetId,component);
		}
		component.setZoom(zoom);
		component.setCenter(new WgsPoint(lon,lat));
		component.setFollowMe(followMe);
		component.start();
		int w=1,h=1;
		switch(sizeSelector) {
		case 1: // 1x1
			w = 72;
			h = 72;
			break;
		case 2: // 2x1
			w = 146;
			h = 72;
			break;
		case 3: // 2x2
			w = 146;
			h = 146;
			break;
		case 4: // 4x2
			w = 294;
			h = 146;
			break;
		}
		component.getMapComponent().setSize(w, h);
		return component;
	}
	
	private void startMapComponents() {
		try {
			SQLiteDatabase widgets = (new DBOpener(this,"widgets.db",2)).getReadableDatabase();
			Cursor c = widgets.rawQuery(String.format("select * from %s",Config.WIDGETS_TABLE), null);
			for(c.moveToFirst(); !c.isAfterLast();c.moveToNext()) {
    			createJamsComponent(c);
			}
			c.close();
			widgets.close();
		} catch(Exception e) {
			Log.e("UpdateService", "The service circle error:", e);
		}
	}
	
    @Override
    public void onDestroy() {
    	processTimer.cancel();
        started = false;
		synchronized(mapComponents) {
	    	Collection<JamsMapComponent> components = mapComponents.values();
	    	for(Iterator<JamsMapComponent> i = components.iterator(); i.hasNext(); ) {
	    		JamsMapComponent c = i.next();
	    		c.stop();
	    	}
		}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
