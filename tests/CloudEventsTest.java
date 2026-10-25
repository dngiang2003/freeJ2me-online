import java.util.*;
import java.util.concurrent.*;
import javax.microedition.lcdui.Display;

/** Run in a dedicated JVM because LCDUI intentionally owns a lifetime event thread. */
public final class CloudEventsTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("freej2me.cloud.events", "true");
        Display display = new Display();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        display.callSerially(new Runnable() {
            public void run() {
                entered.countDown();
                try { release.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
            }
        });
        if (!entered.await(2, TimeUnit.SECONDS)) { throw new AssertionError("No event consumer"); }
        int threads = Thread.activeCount();
        List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
        CountDownLatch completed = new CountDownLatch(1024);
        long start = System.nanoTime();
        for (int i = 0; i < 1024; i++) {
            final int sequence = i;
            display.postInputEvent(new Runnable() {
                public void run() { order.add(sequence); completed.countDown(); }
            });
        }
        long enqueueMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (enqueueMs > 100) { throw new AssertionError("Ingress blocked by game callback: " + enqueueMs); }
        if (Thread.activeCount() > threads + 1) { throw new AssertionError("Input creates threads"); }
        try {
            display.postInputEvent(new Runnable() { public void run() {} });
            throw new AssertionError("Queue should reject overload explicitly");
        } catch (IllegalStateException expected) {}
        release.countDown();
        if (!completed.await(3, TimeUnit.SECONDS)) { throw new AssertionError("Input dispatch stalled"); }
        for (int i = 0; i < 1024; i++) {
            if (order.get(i).intValue() != i) { throw new AssertionError("Input order at " + i); }
        }
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        CountDownLatch afterFailure = new CountDownLatch(1);
        display.callSerially(new Runnable() { public void run() { calls.incrementAndGet(); throw new RuntimeException("intentional test callback failure"); } });
        display.callSerially(new Runnable() { public void run() { afterFailure.countDown(); } });
        if (!afterFailure.await(2, TimeUnit.SECONDS) || calls.get() != 1) { throw new AssertionError("Failed callback replayed or consumer died"); }
        System.out.println("PASS cloud event queue: 1024 ordered events; no per-event threads; nonblocking ingress; bounded overload; no callback replay (enqueue=" + enqueueMs + "ms)");
        System.exit(0);
    }
}
