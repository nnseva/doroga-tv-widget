package tv.doroga.android.widget;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider{
	@Override
	public void onEnabled(Context context)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget1x1);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int id = 1;
		appWidgetManager.updateAppWidget(id, views);
		super.onEnabled(context);
	}
	
	@Override
	public void onDeleted(Context ctxt, int[] ids)
	{
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
				// TODO Auto-generated method stub
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
}
