import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;

/** Deterministic input-to-frame probe, not shipped as a user's game. */
public final class CloudProbeMIDlet extends MIDlet {
    protected void startApp() {
        try {
            javax.microedition.rms.RecordStore store = javax.microedition.rms.RecordStore.openRecordStore("cloud-probe-boots", true);
            int boots = store.getNumRecords() == 0 ? 1 : store.getRecord(1)[0] + 1;
            byte[] data = new byte[] { (byte)boots };
            if (store.getNumRecords() == 0) { store.addRecord(data, 0, 1); }
            else { store.setRecord(1, data, 0, 1); }
            store.closeRecordStore();
            System.err.println("probe.boot=" + boots);
        } catch (Exception e) { throw new RuntimeException("RMS probe failed", e); }
        Display.getDisplay(this).setCurrent(new Probe());
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) {}
    private static final class Probe extends Canvas {
        private int presses;
        public void keyPressed(int key) { presses++; repaint(); }
        public void keyRepeated(int key) { keyPressed(key); }
        protected void paint(Graphics g) {
            g.setColor((presses & 1) == 0 ? 0x00cc44 : 0xcc0044);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(0xffffff);
            g.drawString(Integer.toString(presses), 10, 10, Graphics.TOP | Graphics.LEFT);
        }
    }
}
