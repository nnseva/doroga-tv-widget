package tv.doroga.android.widget;

import android.content.Context;
import android.content.Intent;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class JamsWidgetProvider extends AppWidgetProvider {

	@Override
	public void onEnabled(Context context)
	{
		super.onEnabled(context);
		context.startService(new Intent(context, JamsWidgetUpdateService.class));
	}
	
	@Override
	public void onDeleted(Context context, int[] ids)
	{
		deleteWidgets(context,ids);
		context.startService(new Intent(context, JamsWidgetUpdateService.class));
		super.onDeleted(context, ids);
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds)
	{
		context.startService(new Intent(context, JamsWidgetUpdateService.class));
		super.onUpdate(context, mgr, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context)
	{
		context.stopService(new Intent(context, JamsWidgetUpdateService.class));
		super.onDisabled(context);
	}

	private boolean checkIfTableExists(Context context,String table)
	{
		try {
			SQLiteDatabase widgets = (new DBOpener(context,"widgets.db",2)).getReadableDatabase();
			Cursor c = widgets.rawQuery(String.format("select 0 from %s limit 1",table), null);
			c.close();
			widgets.close();
		} catch(Exception e) {
			return false;
		}
		return true;
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
			android.util.Log.e("DorogaTVWidget", "Widget error:",e);
		}
	}
}
