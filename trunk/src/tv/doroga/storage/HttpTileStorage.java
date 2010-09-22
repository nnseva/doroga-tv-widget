package tv.doroga.storage;

import java.text.SimpleDateFormat;
import java.util.Vector;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public abstract class HttpTileStorage implements TileStorage {
	private boolean needs_to_stop;
	private String proxy_host;
	private int proxy_port;

	public HttpTileStorage() {
		needs_to_stop = false;
		this.proxy_host = null;
		this.proxy_port = 0;
		//request_threads = new HashMap<Long,Thread>(); 
	}
	public HttpTileStorage(String proxy_host, int proxy_port) {
		needs_to_stop = false;
		this.proxy_host = proxy_host;
		this.proxy_port = proxy_port;
		//request_threads = new HashMap<Long,Thread>();
	}

	public abstract String GetTileUrl(int tilex, int tiley, int tilezoom);
	
	@Override
	public void RequestTile(int tilex, int tiley, int tilezoom, TileReceiver receiver) {
		final String url = GetTileUrl(tilex,tiley,tilezoom);
		final TileReceiver rcvr = receiver;
		final int x = tilex,y = tiley,z = tilezoom;
		if( url == null || url.length() == 0 ) {
			receiver.TileUnvailable(tilex, tiley, tilezoom);
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					URL ref = new URL(url);
					HttpURLConnection connection;
					if( proxy_host != null ) {
						InetSocketAddress proxyAddr = new InetSocketAddress(proxy_host, proxy_port);
						Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
						connection = (HttpURLConnection) ref.openConnection(proxy);
					} else {
						connection = (HttpURLConnection) ref.openConnection();
					}
					connection.setDoInput(true);
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					Vector<byte[]> bufs = new Vector<byte[]>();
					int total=0;
					{
						byte [] buf = new byte[512];
						int bytes;
						while( (bytes=inputStream.read(buf)) >= 0 && !needs_to_stop ) {
							byte[] r = new byte[bytes];
							System.arraycopy(buf, 0, r, 0, bytes);
							bufs.add(r);
							total += bytes;
						}
						if( needs_to_stop ) {
							return;
						}
					}
					byte[] result = new byte[total];
					int current = 0;
					for(int i=0; i < bufs.size(); i++) {
						byte [] buf = bufs.get(i);
						System.arraycopy(buf, 0, result, current, buf.length);
					}
					//String s = connection.  
					SimpleDateFormat df = new SimpleDateFormat();
					long ts = df.getCalendar().getTime().getTime();
					rcvr.TileGot(x, y, z, result, ts); // Because of null returned from connection.getHeaderField ...
				} catch(Exception e) {
					e.printStackTrace();
					rcvr.TileUnvailable(x, y, z);
				}
			}
			
		});
	}
}
