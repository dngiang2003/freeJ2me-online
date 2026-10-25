import javax.microedition.midlet.MIDlet;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import java.io.*;

/** Verifies the fork's real HTTP/TCP implementations without a public game account. */
public final class CloudNetworkProbeMIDlet extends MIDlet {
    protected void startApp() {
        try {
            final java.net.ServerSocket httpServer = new java.net.ServerSocket(18080, 1, java.net.InetAddress.getByName("127.0.0.1"));
            final java.net.ServerSocket tcpServer = new java.net.ServerSocket(18081, 1, java.net.InetAddress.getByName("127.0.0.1"));
            Thread httpPeer = new Thread(new Runnable() {
                public void run() {
                    try (java.net.ServerSocket server = httpServer; java.net.Socket socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {}
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 15\r\nConnection: close\r\n\r\ncloud-online-ok".getBytes("UTF-8"));
                        socket.getOutputStream().flush();
                    } catch (Exception e) { throw new RuntimeException(e); }
                }
            }, "Probe-HTTP");
            httpPeer.setDaemon(true); httpPeer.start();
            Thread tcpPeer = new Thread(new Runnable() {
                public void run() {
                    try (java.net.ServerSocket server = tcpServer; java.net.Socket socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        byte[] ping = new byte[4]; new DataInputStream(socket.getInputStream()).readFully(ping);
                        if (!"PING".equals(new String(ping, "UTF-8"))) { throw new IOException("TCP request"); }
                        socket.getOutputStream().write("PONG".getBytes("UTF-8")); socket.getOutputStream().flush();
                    } catch (Exception e) { throw new RuntimeException(e); }
                }
            }, "Probe-TCP");
            tcpPeer.setDaemon(true); tcpPeer.start();
            HttpConnection http = (HttpConnection)Connector.open(getAppProperty("Probe-HTTP"));
            if (http.getResponseCode() != 200) { throw new IOException("HTTP status"); }
            InputStream body = http.openInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int next;
            while ((next = body.read()) != -1) { bytes.write(next); }
            body.close(); http.close();
            if (!"cloud-online-ok".equals(new String(bytes.toByteArray(), "UTF-8"))) { throw new IOException("HTTP body"); }
            SocketConnection tcp = (SocketConnection)Connector.open(getAppProperty("Probe-TCP"));
            OutputStream output = tcp.openOutputStream();
            output.write(new byte[]{80,73,78,71}); output.flush();
            DataInputStream input = tcp.openDataInputStream();
            byte[] response = new byte[4]; input.readFully(response);
            input.close(); output.close(); tcp.close();
            if (!"PONG".equals(new String(response, "UTF-8"))) { throw new IOException("TCP response"); }
            System.err.println("probe.network.ok http=true tcp=true");
            Display.getDisplay(this).setCurrent(new Canvas() {
                protected void paint(Graphics g) { g.setColor(0x00cc44); g.fillRect(0, 0, getWidth(), getHeight()); }
            });
        } catch (Exception e) { throw new RuntimeException("Online probe failed", e); }
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) {}
}
