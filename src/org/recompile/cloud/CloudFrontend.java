/* FreeJ2ME cloud frontend. Distributed under GPL-3.0-or-later; see LICENSE. */
package org.recompile.cloud;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;

/** Headless frontend. Binary IPC is private to the agent in this container. */
public final class CloudFrontend {
    public static final int MAGIC = 0x464a4331; // FJC1
    private final AtomicLong generation = new AtomicLong();
    private volatile long publishedAt;
    private final Object frameSignal = new Object();
    private final Semaphore connections = new Semaphore(2);
    private final AtomicBoolean controlOwner = new AtomicBoolean();
    private final AtomicBoolean frameOwner = new AtomicBoolean();
    private final ScheduledThreadPoolExecutor repeats = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        public Thread newThread(Runnable r) { Thread t = new Thread(r, "Cloud-KeyRepeat"); t.setDaemon(true); return t; }
    });
    private final byte[] token;
    private final int width, height;

    private CloudFrontend(int width, int height, String token) {
        repeats.setRemoveOnCancelPolicy(true);
        this.width = width;
        this.height = height;
        this.token = token.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) { throw new IllegalArgumentException("game.jar width height required"); }
        int width = Integer.parseInt(args[1]), height = Integer.parseInt(args[2]);
        if (width < 1 || height < 1 || width > 800 || height > 800) { throw new IllegalArgumentException("Invalid screen size"); }
        String token = System.getenv("AGENT_TOKEN");
        if (token == null || token.length() < 16 || token.length() > 1024) { throw new IllegalArgumentException("Agent token required"); }
        System.setProperty("java.awt.headless", "true");
        System.setProperty("freej2me.cloud.events", "true");
        CloudFrontend frontend = new CloudFrontend(width, height, token);
        Mobile.clearOldLog();
        Mobile.setPlatform(new MobilePlatform(width, height), new Runnable() {
            public void run() { Mobile.updateSettings(); }
        });
        // Reuse libretro lifecycle behavior, never launch a desktop process on app exit.
        MobilePlatform.isLibretro = true;
        if (!Mobile.getPlatform().load(new File(args[0]).toURI().toString())) { throw new IOException("Cannot load MIDlet"); }
        Mobile.config.settings.put("scrwidth", Integer.toString(width));
        Mobile.config.settings.put("scrheight", Integer.toString(height));
        Mobile.config.settings.put("rotate", "0");
        Mobile.updateSettings();
        Mobile.getPlatform().resizeLCD(width, height);
        Mobile.getPlatform().setPainter(new Runnable() {
            public void run() {
                synchronized (frontend.frameSignal) {
                    frontend.publishedAt = System.nanoTime();
                    frontend.generation.incrementAndGet();
                    frontend.frameSignal.notifyAll();
                }
            }
        });
        ServerSocket server = new ServerSocket(19001, 2, InetAddress.getByName("127.0.0.1"));
        Thread ipc = new Thread(new Runnable() {
            public void run() { frontend.accept(server); }
        }, "Cloud-IPC");
        ipc.setDaemon(true);
        ipc.start();
        System.err.println("cloud.listening headless=true protocol=FJC1");
        Mobile.getPlatform().runJar();
        // runJar may return after startApp; game threads remain responsible for lifecycle.
    }

    private void accept(ServerSocket server) {
        while (!server.isClosed()) {
            try {
                final Socket socket = server.accept();
                if (!connections.tryAcquire()) { socket.close(); continue; }
                Thread peer = new Thread(new Runnable() {
                    public void run() { serve(socket); }
                }, "Cloud-IPC-peer");
                peer.setDaemon(true);
                peer.start();
            } catch (IOException e) { System.err.println("cloud.accept failed: " + e); return; }
        }
    }

    private void serve(Socket socket) {
        boolean[] held = new boolean[23];
        boolean pointerDown = false;
        int pointerX = 0, pointerY = 0;
        boolean control = false;
        boolean claimed = false;
        ScheduledFuture<?>[] repeatTasks = new ScheduledFuture<?>[23];
        try (Socket peer = socket) {
            peer.setTcpNoDelay(true);
            peer.setSoTimeout(5000);
            DataInputStream in = new DataInputStream(new BufferedInputStream(peer.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(peer.getOutputStream()));
            if (in.readInt() != MAGIC) { throw new IOException("Protocol mismatch"); }
            int length = in.readUnsignedShort();
            if (length < 16 || length > 1024) { throw new IOException("Invalid credential length"); }
            byte[] supplied = new byte[length];
            in.readFully(supplied);
            if (!MessageDigest.isEqual(token, supplied)) { throw new IOException("Unauthorized"); }
            int channel = in.readUnsignedByte();
            if (channel != 1 && channel != 2) { throw new IOException("Invalid channel"); }
            control = channel == 2;
            claimed = (control ? controlOwner : frameOwner).compareAndSet(false, true);
            if (!claimed) { throw new IOException("Channel already owned"); }
            out.writeInt(MAGIC); out.flush();
            peer.setSoTimeout(0);
            int[] pixels = control ? null : new int[width * height];
            byte[] rgb = control ? null : new byte[width * height * 3];
            long lastSequence = 0;
            while (true) {
                int command = in.readUnsignedByte();
                if (command == 1 && !control) {
                    long previous = in.readLong();
                    int waitMillis = in.readUnsignedShort();
                    if (waitMillis > 50) { throw new IOException("Invalid snapshot wait"); }
                    synchronized (frameSignal) {
                        long deadline = System.nanoTime() + waitMillis * 1000000L;
                        while (generation.get() != 0 && generation.get() == previous && waitMillis > 0) {
                            long remaining = deadline - System.nanoTime();
                            if (remaining <= 0) { break; }
                            TimeUnit.NANOSECONDS.timedWait(frameSignal, remaining);
                        }
                    }
                    long current = generation.get();
                    if (current == 0) { out.writeByte(2); }
                    else if (current == previous) { out.writeByte(0); }
                    else {
                        PlatformImage image = Mobile.getPlatform().getLcdFrontbuffer();
                        synchronized (image) {
                            if (image.getDataBuffer().length != pixels.length) { throw new IOException("Screen dimensions changed"); }
                            System.arraycopy(image.getDataBuffer(), 0, pixels, 0, pixels.length);
                            current = generation.get();
                        }
                        for (int i = 0, j = 0; i < pixels.length; i++) {
                            rgb[j++] = (byte)(pixels[i] >> 16); rgb[j++] = (byte)(pixels[i] >> 8); rgb[j++] = (byte)pixels[i];
                        }
                        out.writeByte(1); out.writeLong(current); out.writeLong(publishedAt);
                        out.writeInt(width); out.writeInt(height); out.writeInt(rgb.length); out.write(rgb);
                    }
                    out.flush();
                } else if ((command == 2 || command == 3 || command == 4) && control) {
                    long sequence = in.readLong();
                    if (sequence <= lastSequence) { throw new IOException("Input sequence out of order"); }
                    lastSequence = sequence;
                    long received = System.nanoTime();
                    if (command == 2) {
                        int key = in.readUnsignedByte(), state = in.readUnsignedByte();
                        if (key >= 20 || state > 1) { throw new IOException("Invalid key"); }
                        synchronized (held) {
                            if (held[key] != (state == 1)) {
                                held[key] = state == 1;
                                MobilePlatform.pressedKeys[key] = held[key];
                                if (held[key]) {
                                    MobilePlatform.keyPressed(Mobile.getMobileKey(key));
                                    final int repeatedKey = key;
                                    repeatTasks[key] = repeats.scheduleWithFixedDelay(new Runnable() {
                                        public void run() {
                                            synchronized (held) {
                                                if (held[repeatedKey]) {
                                                    try { MobilePlatform.keyRepeated(Mobile.getMobileKey(repeatedKey)); }
                                                    catch (RuntimeException e) {
                                                        try { peer.close(); } catch (IOException ignored) {}
                                                    }
                                                }
                                            }
                                        }
                                    }, 400, 80, TimeUnit.MILLISECONDS);
                                } else {
                                    if (repeatTasks[key] != null) { repeatTasks[key].cancel(false); repeatTasks[key] = null; }
                                    MobilePlatform.keyReleased(Mobile.getMobileKey(key));
                                }
                            }
                        }
                    } else if (command == 3) {
                        int x = in.readInt(), y = in.readInt(), state = in.readUnsignedByte();
                        if (x < 0 || y < 0 || x >= width || y >= height || state > 1) { throw new IOException("Invalid pointer"); }
                        pointerX = x; pointerY = y;
                        if (state == 1 && !pointerDown) { MobilePlatform.pointerPressed(x, y); }
                        else if (state == 1) { MobilePlatform.pointerDragged(x, y); }
                        else if (pointerDown) { MobilePlatform.pointerReleased(x, y); }
                        pointerDown = state == 1;
                    } else {
                        release(held, repeatTasks);
                        if (pointerDown) { MobilePlatform.pointerReleased(pointerX, pointerY); pointerDown = false; }
                    }
                    // ACK means accepted/enqueued, not game logic or display completion.
                    out.writeLong(sequence); out.writeLong(received); out.writeLong(System.nanoTime()); out.flush();
                } else { throw new IOException("Invalid command for channel"); }
            }
        } catch (EOFException e) {
            // Normal agent shutdown/reconnect.
        } catch (Exception e) { System.err.println("cloud.peer failed: " + e); }
        finally {
            if (control) {
                try {
                    release(held, repeatTasks);
                    if (pointerDown) { MobilePlatform.pointerReleased(pointerX, pointerY); }
                } catch (Exception e) { System.err.println("cloud.release failed: " + e); }
            }
            if (claimed && control) {
                System.err.println("cloud.input.closed callbacks=" + javax.microedition.lcdui.Display.getCloudInputCount()
                    + " max_queue_us=" + javax.microedition.lcdui.Display.getCloudInputMaxLagMicros());
            }
            if (claimed) { (control ? controlOwner : frameOwner).set(false); }
            connections.release();
        }
    }

    private static void release(boolean[] held, ScheduledFuture<?>[] repeatTasks) {
      synchronized (held) {
        for (int key = 0; key < held.length; key++) {
            if (held[key]) {
                held[key] = false;
                if (repeatTasks[key] != null) { repeatTasks[key].cancel(false); repeatTasks[key] = null; }
                MobilePlatform.pressedKeys[key] = false;
                MobilePlatform.keyReleased(Mobile.getMobileKey(key));
            }
        }
      }
    }
}
