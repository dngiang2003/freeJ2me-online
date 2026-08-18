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

import java.net.Socket;
import java.net.InetSocketAddress;

import org.recompile.mobile.Mobile;

public class SocketConnectionImpl implements SocketConnection
{
	private static final int CONNECT_TIMEOUT = 10000;

	private final String host;
	private final int port;

	private Socket socket;
	private String address;

	public SocketConnectionImpl(String name) throws IOException
	{
		String spec = name;
		if (spec.startsWith("socket://")) { spec = spec.substring("socket://".length()); }
		else if (spec.startsWith("socket:")) { spec = spec.substring("socket:".length()); }

		int slash = spec.indexOf('/');
		if (slash != -1) { spec = spec.substring(0, slash); }

		int colon = spec.lastIndexOf(':');
		if (colon == -1)
		{
			throw new IllegalArgumentException("Invalid socket URL: " + name);
		}
		host = spec.substring(0, colon);
		port = Integer.parseInt(spec.substring(colon + 1));

		Mobile.log(Mobile.LOG_DEBUG, SocketConnectionImpl.class.getPackage().getName() + "." + SocketConnectionImpl.class.getSimpleName() + ": " + "New Socket Connection: " + host + ":" + port);
	}

	private void connect() throws IOException
	{
		if (socket == null)
		{
			Mobile.log(Mobile.LOG_WARNING, SocketConnectionImpl.class.getPackage().getName() + "." + SocketConnectionImpl.class.getSimpleName() + ": " + "Socket Connection requested: " + host + ":" + port);
			socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
			socket.setTcpNoDelay(true);
			address = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : host;
		}
	}

	public String getAddress()
	{
		try { connect(); return address; }
		catch (IOException e) { return host; }
	}

	public String getLocalAddress()
	{
		try { connect(); return socket.getLocalAddress() != null ? socket.getLocalAddress().getHostAddress() : ""; }
		catch (IOException e) { return ""; }
	}

	public int getLocalPort()
	{
		try { connect(); return socket.getLocalPort(); }
		catch (IOException e) { return 0; }
	}

	public int getPort() { return port; }

	public int getSocketOption(byte option)
	{
		try
		{
			connect();
			switch (option)
			{
				case DELAY: return socket.getTcpNoDelay() ? 1 : 0;
				case LINGER: return socket.getSoLinger();
				case KEEPALIVE: return socket.getKeepAlive() ? 1 : 0;
				case RCVBUF: return socket.getReceiveBufferSize();
				case SNDBUF: return socket.getSendBufferSize();
				default: return 0;
			}
		}
		catch (IOException e) { return 0; }
	}

	public void setSocketOption(byte option, int value)
	{
		try
		{
			connect();
			switch (option)
			{
				case DELAY: socket.setTcpNoDelay(value != 0); break;
				case LINGER: socket.setSoLinger(value != 0, value); break;
				case KEEPALIVE: socket.setKeepAlive(value != 0); break;
				case RCVBUF: socket.setReceiveBufferSize(value); break;
				case SNDBUF: socket.setSendBufferSize(value); break;
				default: break;
			}
		}
		catch (IOException e) { Mobile.log(Mobile.LOG_WARNING, SocketConnectionImpl.class.getPackage().getName() + "." + SocketConnectionImpl.class.getSimpleName() + ": " + "Failed to set socket option: " + e.getMessage()); }
	}

	public DataInputStream openDataInputStream() throws IOException { return new DataInputStream(openInputStream()); }

	public InputStream openInputStream() throws IOException { connect(); return socket.getInputStream(); }

	public DataOutputStream openDataOutputStream() throws IOException { return new DataOutputStream(openOutputStream()); }

	public OutputStream openOutputStream() throws IOException { connect(); return socket.getOutputStream(); }

	public void close() throws IOException
	{
		if (socket != null)
		{
			socket.close();
			socket = null;
		}
		Mobile.log(Mobile.LOG_DEBUG, SocketConnectionImpl.class.getPackage().getName() + "." + SocketConnectionImpl.class.getSimpleName() + ": " + "'closing' socket connection");
	}
}
