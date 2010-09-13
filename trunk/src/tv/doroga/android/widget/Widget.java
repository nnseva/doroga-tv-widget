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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider{
    @Override
	public void onEnabled(Context context)
	{
		super.onEnabled(context);
        updateWidgets(context);
	}
	
	@Override
	public void onDeleted(Context ctxt, int[] ids)
	{
		deleteWidgets(ctxt,ids);
		for(int i=0; i < ids.length; i++) {
			Intent updaterIntent = new Intent();
			updaterIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			updaterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
			updaterIntent.setData(Uri.withAppendedPath(Uri.parse("doroga://widget/id/"), String.valueOf(ids[i]))); // Find unique intent
			PendingIntent pendingIntent = PendingIntent.getBroadcast(ctxt.getApplicationContext(), 0, updaterIntent, PendingIntent.FLAG_NO_CREATE);
			if( pendingIntent != null )
				pendingIntent.cancel();
		}
		super.onDeleted(ctxt, ids);
	}
	
	@Override
	public void onUpdate(Context ctxt, AppWidgetManager mgr, int[] appWidgetIds)
	{
		final Context context = ctxt;
		final AppWidgetManager appWidgetManager = mgr;
		final int[] ids = appWidgetIds;

		new Thread(new Runnable(){
			@Override
			public void run() {
				for (int i = 0; i < ids.length; i++)
				{
					appWidgetManager.updateAppWidget(ids[i], buildUpdate(context, ids[i]));
				}
			}
		}).start();
		super.onUpdate(ctxt, mgr, appWidgetIds);
	}
	public RemoteViews buildUpdate(Context context, int id)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget1x1);
		return views;
	}
	@Override
	public void onDisabled(Context ctxt)
	{
	}

	private boolean checkIfTableExists(Context context,String table)
	{
		try {
			SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getWritableDatabase();
			widgets.rawQuery(String.format("select 0 from %s limit 1",table), null);
			widgets.close();
		} catch(Exception e) {
			return false;
		}
		return true;
	}

	private void updateWidgets(Context context)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		try
		{
			if( !checkIfTableExists(context,Config.WIDGETS_TABLE) ) {
				android.util.Log.i("DorogaTVWidget", String.format("Widget - widgets table not yet created"));
			} else {
				SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getWritableDatabase();
				Cursor c = widgets.rawQuery(String.format("select * from %s",Config.WIDGETS_TABLE), null);
				for(c.moveToFirst(); !c.isAfterLast();c.moveToNext()) {
					int widgetId = c.getInt(c.getColumnIndex("widgetId"));
					double lon = c.getDouble(c.getColumnIndex("lon"));
					double lat = c.getDouble(c.getColumnIndex("lat"));
					int zoom = c.getInt(c.getColumnIndex("zoom"));
					int sizeSelector = c.getInt(c.getColumnIndex("sizeSelector"));
					boolean followMe = c.getInt(c.getColumnIndex("followMe")) > 0;
					// TODO: use values to service up
					RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget1x1);
					appWidgetManager.updateAppWidget(widgetId, views);
					// Restart alarms to have unique updates
					Intent updaterIntent = new Intent();
					updaterIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
					updaterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetId});
					updaterIntent.setData(Uri.withAppendedPath(Uri.parse("doroga://widget/id/"), String.valueOf(widgetId))); // To have an intent be unique
					PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updaterIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+5, 60000, pendingIntent);
				}
				c.close();
				widgets.close();
			}
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "Widget",e);
		}
	}

	private void deleteWidgets(Context context,int [] ids)
	{
		try
		{
			if( !checkIfTableExists(context,Config.WIDGETS_TABLE) ) {
				android.util.Log.i("DorogaTVWidget", String.format("Widget - widgets table not yet created"));
			} else if( ids.length > 0 ){
				StringBuilder sb = new StringBuilder();
				for( int i=0; i < ids.length; i++) {
					if( i > 0 ) {
						sb.append(',');
					}
					sb.append(ids[i]);
				}
				SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getWritableDatabase();
				widgets.execSQL(String.format("delete from %s where widgetId in (%s)",Config.WIDGETS_TABLE,sb.toString()));
				widgets.close();
			}
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "Widget",e);
		}
	}
}
