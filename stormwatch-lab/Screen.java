import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

final class Screen implements Runnable {
    private final Map<Item.Kind, AtomicLong> shown = new EnumMap<>(Item.Kind.class);
    private final IntSupplier waiting;
    private volatile boolean running = true;
    private volatile String frame = "";
    private static final long SCREEN_TIMEOUT_MS = 3_000;
    private volatile long lastWake = System.currentTimeMillis();

    Screen(IntSupplier waiting) {
        this.waiting = waiting;
        for (Item.Kind k : Item.Kind.values()) shown.put(k, new AtomicLong());
    }

    void record(Item.Kind kind) {
        shown.get(kind).incrementAndGet();
        if (kind == Item.Kind.SEVERE) lastWake = System.currentTimeMillis();
    }
    void stop() { running = false; }

    public void run() {
        long lastPrint = System.currentTimeMillis();
        while (running) {
            long now = System.currentTimeMillis();
            boolean screenOn = now - lastWake < SCREEN_TIMEOUT_MS;
            if (screenOn) frame = render();
            if (now - lastPrint >= 10_000) {
                System.out.printf("[app] shown: routine=%d warnings=%d refreshes=%d | waiting=%d%n",
                        shown.get(Item.Kind.ROUTINE).get(), shown.get(Item.Kind.SEVERE).get(),
                        shown.get(Item.Kind.REFRESH).get(), waiting.getAsInt());
                lastPrint = now;
            }
            long sleepMs;
            if (screenOn) {
                sleepMs = 2;
            } else {
                sleepMs = 100;
            }
            try { Thread.sleep(sleepMs); } catch (InterruptedException e) { return; }
        }
    }

    private String render() {
        long total = 0;
        long[] v = new long[3];
        int i = 0;
        for (AtomicLong c : shown.values()) { v[i] = c.get(); total += v[i++]; }
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 12; row++) {
            long bar = total == 0 ? 0 : v[row % 3] * 60 / total;
            for (int col = 0; col < 60; col++)
                sb.append(String.format("%c", col < bar ? '#' : '.'));
            sb.append('\n');
        }
        return sb.toString();
    }
}
