/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import java.util.HashMap;
import java.util.Map;

import org.recompile.mobile.Mobile;

class HttpConnectionImpl implements HttpConnection, com.nttdocomo.io.HttpConnection
{
	private static final int CONNECT_TIMEOUT = 10000;
	private static final int READ_TIMEOUT = 30000;

	private final String url;
	private String requestMethod = GET;

	private Map<String, String> requestProperty = new HashMap<String, String>();

	private HttpURLConnection conn;
	private URL urlObj;
	private OutputStream pendingOutput;

	public HttpConnectionImpl(String url)
	{
		this.url = url;
		Mobile.log(Mobile.LOG_DEBUG, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "New Http Connection: " + this.url);
	}

	private void flushOutput()
	{
		if (pendingOutput != null)
		{
			try
			{
				pendingOutput.close();
			}
			catch (IOException e) { Mobile.log(Mobile.LOG_WARNING, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "Error closing output stream: " + e.getMessage()); }
			pendingOutput = null;
		}
	}

	private HttpURLConnection getConnection() throws IOException
	{
		flushOutput();
		if (conn == null)
		{
			try
			{
				urlObj = new URL(url.trim().replace(" ", "%20"));
			}
			catch (MalformedURLException e)
			{
				Mobile.log(Mobile.LOG_WARNING, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "Malformed URL: " + url);
				throw new IOException("Malformed URL: " + url);
			}
			URLConnection c = urlObj.openConnection();
			if (c instanceof HttpURLConnection)
			{
				conn = (HttpURLConnection)c;
			}
			else
			{
				throw new IOException("Not an HTTP URL: " + url);
			}
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.setReadTimeout(READ_TIMEOUT);
			conn.setInstanceFollowRedirects(true);
			if (!requestProperty.containsKey("User-Agent"))
			{
				conn.setRequestProperty("User-Agent", "FreeJ2ME/2.2.9");
			}
			for (Map.Entry<String, String> entry : requestProperty.entrySet())
			{
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
			conn.setRequestMethod(requestMethod);
			Mobile.log(Mobile.LOG_WARNING, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "Http Connection requested: " + this.url);
		}
		return conn;
	}

	public String getURL() { return url; }

	public String getProtocol() { return url.split(":")[0]; }

	public String getHost()
	{
		try { return new URL(url).getHost(); }
		catch (MalformedURLException e) { return ""; }
	}

	public String getFile()
	{
		try
		{
			String file = new URL(url).getFile();
			return (file != null) ? file : "";
		}
		catch (MalformedURLException e) { return ""; }
	}

	public String getRef()
	{
		try
		{
			String ref = new URL(url).getRef();
			return (ref != null) ? ref : "";
		}
		catch (MalformedURLException e) { return ""; }
	}

	public String getQuery()
	{
		try
		{
			String query = new URL(url).getQuery();
			return (query != null) ? query : "";
		}
		catch (MalformedURLException e) { return ""; }
	}

	public int getPort()
	{
		try
		{
			int port = new URL(url).getPort();
			if (port == -1)
			{
				return (url.toLowerCase().startsWith("https:")) ? 443 : 80;
			}
			return port;
		}
		catch (MalformedURLException e) { return 80; }
	}

	public String getRequestMethod() { return requestMethod; }

	public void setRequestMethod(String method)
	{
		this.requestMethod = method;
		if (conn != null)
		{
			try { conn.setRequestMethod(method); }
			catch (ProtocolException e) { Mobile.log(Mobile.LOG_WARNING, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "Unsupported request method: " + method); }
		}
	}

	public String getRequestProperty(String key) { return requestProperty.get(key); }

	public void setRequestProperty(String key, String value)
	{
		requestProperty.put(key, value);
		if (conn != null)
		{
			conn.setRequestProperty(key, value);
		}
	}

	public void connect() throws IOException { getConnection().connect(); }

	public int getResponseCode() throws IOException
	{
		int code = getConnection().getResponseCode();
		Mobile.log(Mobile.LOG_DEBUG, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "Response code: " + code);
		return code;
	}

	public String getResponseMessage() throws IOException { return getConnection().getResponseMessage(); }

	public long getExpiration() throws IOException { return getConnection().getExpiration(); }

	public long getDate() throws IOException { return getConnection().getDate(); }

	public long getLastModified() throws IOException { return getConnection().getLastModified(); }

	public String getHeaderField(String name) throws IOException { return getConnection().getHeaderField(name); }

	public int getHeaderFieldInt(String name, int def) throws IOException { return getConnection().getHeaderFieldInt(name, def); }

	public long getHeaderFieldDate(String name, long def) throws IOException { return getConnection().getHeaderFieldDate(name, def); }

	public String getHeaderField(int n) throws IOException { return getConnection().getHeaderField(n); }

	public String getHeaderFieldKey(int n) throws IOException { return getConnection().getHeaderFieldKey(n); }

	public void close()
	{
		flushOutput();
		if (conn != null)
		{
			conn.disconnect();
			conn = null;
		}
		Mobile.log(Mobile.LOG_DEBUG, HttpConnectionImpl.class.getPackage().getName() + "." + HttpConnectionImpl.class.getSimpleName() + ": " + "'closing' http connection");
	}

	public String getType() throws IOException { return getConnection().getContentType(); }

	public String getEncoding() throws IOException { return getConnection().getContentEncoding(); }

	public long getLength() throws IOException { return getConnection().getContentLengthLong(); }

	public DataInputStream openDataInputStream() throws IOException { return new DataInputStream(this.openInputStream()); }

	public InputStream openInputStream() throws IOException
	{
		flushOutput();
		HttpURLConnection c = getConnection();
		int code = c.getResponseCode();
		if (code >= 400)
		{
			throw new IOException("HTTP error: " + code + " " + c.getResponseMessage());
		}
		return c.getInputStream();
	}

	public DataOutputStream openDataOutputStream() throws IOException { return new DataOutputStream(this.openOutputStream()); }

	public OutputStream openOutputStream() throws IOException
	{
		HttpURLConnection c = getConnection();
		c.setDoOutput(true);
		pendingOutput = c.getOutputStream();
		return pendingOutput;
	}
}
