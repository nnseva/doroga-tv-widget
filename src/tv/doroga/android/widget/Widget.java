package tv.doroga.android.widget;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
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
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	@Override
	public void onEnabled(Context context)
	{
		super.onEnabled(context);
		/*
        updateWidgets(context);
        */
		context.startService(new Intent(context, UpdateService.class));
	}
	
	@Override
	public void onDeleted(Context ctxt, int[] ids)
	{
		deleteWidgets(ctxt,ids);
		/*
		for(int i=0; i < ids.length; i++) {
			Intent updaterIntent = new Intent();
			updaterIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			updaterIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
			updaterIntent.setData(Uri.withAppendedPath(Uri.parse("doroga://widget/id/"), String.valueOf(ids[i]))); // Find unique intent
			PendingIntent pendingIntent = PendingIntent.getBroadcast(ctxt.getApplicationContext(), 0, updaterIntent, PendingIntent.FLAG_NO_CREATE);
			if( pendingIntent != null )
				pendingIntent.cancel();
		}*/
		super.onDeleted(ctxt, ids);
	}
	
	@Override
	public void onUpdate(Context ctxt, AppWidgetManager mgr, int[] appWidgetIds)
	{
		/*
		final Context context = ctxt;
		final int[] ids = appWidgetIds;
		 */
		super.onUpdate(ctxt, mgr, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context)
	{
		context.stopService(new Intent(context, UpdateService.class));
	}

	private boolean checkIfTableExists(Context context,String table)
	{
		try {
			SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getReadableDatabase();
			widgets.rawQuery(String.format("select 0 from %s limit 1",table), null);
			widgets.close();
		} catch(Exception e) {
			return false;
		}
		return true;
	}
/*
	private void updateWidget(Context context,int widgetId, double lon, double lat, int zoom, int sizeSelector, boolean followMe) {
		int[] widgetIds = {
				0,
				R.layout.widget1x1,
				R.layout.widget2x1,
				R.layout.widget2x2,
				R.layout.widget4x2
		};
		RemoteViews views = new RemoteViews(context.getPackageName(), widgetIds[sizeSelector]);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgetManager.updateAppWidget(widgetId, views);
	}
*/
	/* we don't need clock alarms, the service will do the job
	private void updateWidgets(Context context)
	{
		try
		{
			if( !checkIfTableExists(context,Config.WIDGETS_TABLE) ) {
				android.util.Log.i("DorogaTVWidget", String.format("Widget - widgets table not yet created"));
			} else {
				SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getReadableDatabase();
				Cursor c = widgets.rawQuery(String.format("select * from %s",Config.WIDGETS_TABLE), null);
				for(c.moveToFirst(); !c.isAfterLast();c.moveToNext()) {
					int widgetId = c.getInt(c.getColumnIndex("widgetId"));
					double lon = c.getDouble(c.getColumnIndex("lon"));
					double lat = c.getDouble(c.getColumnIndex("lat"));
					int zoom = c.getInt(c.getColumnIndex("zoom"));
					int sizeSelector = c.getInt(c.getColumnIndex("sizeSelector"));
					boolean followMe = c.getInt(c.getColumnIndex("followMe")) > 0;
					updateWidget(context,widgetId,lon,lat,zoom,sizeSelector,followMe);
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
		*/

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
			android.util.Log.e("DorogaTVWidget", "Widget error:",e);
		}
	}

	public class UpdateService extends Service {
		private Thread processThread;
		private boolean needsStop;
        @Override
        public void onStart(Intent intent, int startId) {
            Log.d("UpdateService", "Started");
            final Context context = this;
            needsStop = false;
    		processThread = new Thread(new Runnable(){
    			@Override
    			public void run() {
    				while(!needsStop) {
    					try {
        					SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getReadableDatabase();
		    				Cursor c = widgets.rawQuery(String.format("select widgetId from %s",Config.WIDGETS_TABLE), null);
		    				for(c.moveToFirst(); !c.isAfterLast();c.moveToNext()) {
		    					int widgetId = c.getInt(c.getColumnIndex("widgetId"));
		    					startUpdateWidget(widgetId,context);
		    				}
		    				widgets.close();
    					} catch(Exception e) {
    						// TODO Auto-generated catch block
    						Log.e("UpdateService", "The service circle error:", e);
    					}
	    				for(int i=0; i < 6 && !needsStop; i++) { // to avoid system load but quickly react to destroy
		    				try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								break;
							}
	    				}
    				}
    			}
    		});
    		processThread.start();
        }
        @Override
        public void onDestroy() {
        	needsStop = true;
        	try {
				processThread.join();
				Log.i("UpdateService", "The service finished successfully");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Log.e("StartUpdateWidget", "The service finished with error:", e);
			}
        }

        private void startUpdateWidget(int widgetId, Context context) {
        	try {
	        	SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getReadableDatabase();
	        	SQLiteDatabase tilecache = (new DBOpener(context,"tilecache.db",2)).getReadableDatabase();
				Log.i("UpdateService", "Widget update finished successfully");
        	} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("StartUpdateWidget", "Widget update finished with error:", e);
			}
    		/*
    		Cursor c = widgets.rawQuery(String.format("select * from %s where widgetId = %d",Config.WIDGETS_TABLE,widgetId), null);
    		if(c.moveToFirst()) {
    			double lon = c.getDouble(c.getColumnIndex("lon"));
    			double lat = c.getDouble(c.getColumnIndex("lat"));
    			int zoom = c.getInt(c.getColumnIndex("zoom"));
    			int sizeSelector = c.getInt(c.getColumnIndex("sizeSelector"));
    			boolean followMe = c.getInt(c.getColumnIndex("followMe")) > 0;
    			updateWidget(context,widgetId,lon,lat,zoom,sizeSelector,followMe);

            
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);
            Log.d("WordWidget.UpdateService", "update built");
            
            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, WordWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
            Log.d("WordWidget.UpdateService", "widget updated");
    		}*/

    	}

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
        
        /*
         * Build a widget update to show the current Wiktionary
         * "Word of the day." Will block until the online API returns.
        public RemoteViews buildUpdate(Context context) {
            // Pick out month names from resources
            Resources res = context.getResources();
            String[] monthNames = res.getStringArray(R.array.month_names);
            
            // Find current month and day
            Time today = new Time();
            today.setToNow();

            // Build the page title for today, such as "March 21"
            String pageName = res.getString(R.string.template_wotd_title,
                    monthNames[today.month], today.monthDay);
            String pageContent = null;
            
            try {
                // Try querying the Wiktionary API for today's word
                SimpleWikiHelper.prepareUserAgent(context);
                pageContent = SimpleWikiHelper.getPageContent(pageName, false);
            } catch (ApiException e) {
                Log.e("WordWidget", "Couldn't contact API", e);
            } catch (ParseException e) {
                Log.e("WordWidget", "Couldn't parse API response", e);
            }
            
            RemoteViews views = null;
            Matcher matcher = null;
            
                Prefs prefs = new Prefs(this);
            if (pageContent == null) {
                // could not get content, use cache
                // could be null
                pageContent = prefs.getPageContent();
            }
            
            if (pageContent != null) {
                // we have page content
                // is it valid?
                matcher = Pattern.compile(WOTD_PATTERN).matcher(pageContent);
            }
            if (matcher != null && matcher.find()) {
                // valid content, cache it 
                // ensure that latest valid content is
                // always cached in case of failures
                prefs.setPageContent(pageContent);

                // Build an update that holds the updated widget contents
                views = new RemoteViews(context.getPackageName(), R.layout.widget_word);
                
                String wordTitle = matcher.group(1);
                views.setTextViewText(R.id.word_title, wordTitle);
                views.setTextViewText(R.id.word_type, matcher.group(2));
                views.setTextViewText(R.id.definition, matcher.group(3).trim());
                
                // When user clicks on widget, launch to Wiktionary definition page
                String definePage = String.format("%s://%s/%s", ExtendedWikiHelper.WIKI_AUTHORITY,
                        ExtendedWikiHelper.WIKI_LOOKUP_HOST, wordTitle);
                Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(definePage));
                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 , defineIntent, 0 );
                views.setOnClickPendingIntent(R.id.widget, pendingIntent);
                
            } else {
                // Didn't find word of day, so show error message
                views = new RemoteViews(context.getPackageName(), R.layout.widget_message);
                views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
            }
            return views;
        }         */
    }

}
