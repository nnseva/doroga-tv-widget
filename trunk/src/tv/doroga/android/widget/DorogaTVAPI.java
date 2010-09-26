package tv.doroga.android.widget;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.*;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;

public class DorogaTVAPI {
	public static String RESULT_OK = "ok";
	public static String RESULT_SERVER_ERROR = "Server Error";
	public static String RESULT_NETWORK_ERROR = "Network Error";
	public static String JAMS_TEMPLATE = "/makeTraficImg.php?x=%1$d&y=%2$d&Level=%3$d";
	public static String DATA_PROVIDER_LIST = "http://tools.doroga.tv/backend/region_list.php";
	public static String DATA_PROVIDERS_TABLE = "data_providers";
	
	private String data_provider_list;
	private Context context;
	
	public DorogaTVAPI(Context context) {
		data_provider_list = DATA_PROVIDER_LIST;
		this.context = context;
		prepareDataProviders();
	}

	public DorogaTVAPI(Context context,String dataProviderListURL) {
		data_provider_list = dataProviderListURL;
		this.context = context;
		prepareDataProviders();
	}
	private boolean checkIfTableExists(String table)
	{
		SQLiteDatabase data_providers = (new DBOpener(context,"dataproviders.db",1)).getWritableDatabase();
		try {
			data_providers.rawQuery(String.format("select 0 from %s limit 1",table), null);
		} catch(Exception e) {
			return false;
		}
		data_providers.close();
		return true;
	}
	private void prepareDataProviders()
	{
		SQLiteDatabase data_providers = (new DBOpener(context,"dataproviders.db",1)).getWritableDatabase();
		try
		{
			if( !checkIfTableExists("last_update")) {
				data_providers.execSQL("create table last_update (last_update INTEGER)");
				data_providers.execSQL(new StringBuilder().append("insert into last_update (last_update) values (").append(System.currentTimeMillis()).append(")").toString());
			}

			Cursor c = data_providers.rawQuery("select last_update from last_update",null);
			c.moveToFirst();
			if( c.getLong(0) > System.currentTimeMillis() + 1000*60*60 ) {
				android.util.Log.i("DorogaTVWidget", String.format("DorogaTVAPI - data provider table too old"));
				requestDataProviders();
			} else if( !checkIfTableExists(DATA_PROVIDERS_TABLE) ) {
				android.util.Log.i("DorogaTVWidget", String.format("DorogaTVAPI - data provider table not yet created"));
				requestDataProviders();
			}
			c.close();
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "DorogaTVAPI",e);
		}
		data_providers.close();
	}
	
	private void requestDataProviders()
	{
		Thread requestThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String proxyHost = android.net.Proxy.getDefaultHost();
				int proxyPort = android.net.Proxy.getDefaultPort();
				
				try
				{
					URL url = new URL(data_provider_list);
					HttpURLConnection connection;
					if (proxyPort > 0)
					{
						InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
						Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
						connection = (HttpURLConnection) url.openConnection(proxy);
					}
					else
					{
						connection = (HttpURLConnection) url.openConnection();					
					}
					connection.setDoInput(true);
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					LabeledCSVParser lcsvp = new LabeledCSVParser(new ExcelCSVParser(reader));
					String[] labels = lcsvp.getLabels();
					String[][] values = lcsvp.getAllValues();
					storeDataProviders(labels,values);
				}
				catch(Exception e)
				{
					android.util.Log.e("DorogaTVWidget", "DorogaTVAPI",e);
				}
			}
			
		});
		requestThread.start();
	}
	
	protected void storeDataProviders(String[] labels, String[][] values) {
		try
		{
			SQLiteDatabase data_providers = (new DBOpener(context,"dataproviders.db",1)).getWritableDatabase();
			data_providers.execSQL(new StringBuilder("DROP TABLE IF EXISTS ").append(DATA_PROVIDERS_TABLE).toString());
			{
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ").append(DATA_PROVIDERS_TABLE).append(" (");
				for(int i=0; i < labels.length; i++) {
					if(i > 0) sb.append(',');
					sb.append(labels[i]);
					sb.append(' ');
					if( i > 0 )
						sb.append("LONG VARCHAR");
					else if( labels[i] == "minLon" || labels[i] == "minLat" || labels[i] == "maxLon" || labels[i] == "maxLat" )
						sb.append("DOUBLE");
					else
						sb.append("INTEGER");
				}
				sb.append(", PRIMARY KEY(").append(labels[0]).append(") )");
				data_providers.execSQL(sb.toString());
				data_providers.execSQL(new StringBuilder().append("CREATE INDEX minlatlon ON ").append(DATA_PROVIDERS_TABLE).append("(minLon,minLat)").toString());
				data_providers.execSQL(new StringBuilder().append("CREATE INDEX maxlatlon ON ").append(DATA_PROVIDERS_TABLE).append("(maxLon,maxLat)").toString());
			}
			for(int j=0; j < values.length; j++) {
				ContentValues cv = new ContentValues();
				for(int i=0; i < labels.length; i++) {
					cv.put(labels[i],values[j][i]);
				}
				data_providers.insert(DATA_PROVIDERS_TABLE, null, cv);
			}
			data_providers.close();
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "DorogaTVAPI",e);
		}
	}

	public String getDataProviderURL(double lon, double lat) {
		try
		{
			String[] fields = {
					"InetAddress"
			};
			String[] args = {
					String.format("%18.15f", lon),
					String.format("%18.15f", lat)
			};
			if( !checkIfTableExists(DATA_PROVIDERS_TABLE) ) {
				android.util.Log.i("DorogaTVWidget", String.format("DorogaTVAPI - data provider table not found for location %f/%f",lon,lat));					
				return null;
			}
			SQLiteDatabase data_providers = (new DBOpener(context,"dataproviders.db",1)).getWritableDatabase();
			Cursor c = data_providers.query(DATA_PROVIDERS_TABLE, fields,
					"? BETWEEN minLon and maxLon AND ? BETWEEN minLat and maxLat", args,null,null,"RegionId");
			if( c.moveToFirst() ) {
				android.util.Log.i("DorogaTVWidget", String.format("DorogaTVAPI - provider found for location %f/%f - %s",lon,lat,c.getString(0)));
				String r = c.getString(0);
				c.close();
				data_providers.close();
				return r;
			}
			android.util.Log.i("DorogaTVWidget", String.format("DorogaTVAPI - provider not found for location %f/%f",lon,lat));
			data_providers.close();
		}
		catch(Exception e)
		{
			android.util.Log.e("DorogaTVWidget", "DorogaTVAPI",e);
		}
		return null;
	}
	public void close() {
	}
}
