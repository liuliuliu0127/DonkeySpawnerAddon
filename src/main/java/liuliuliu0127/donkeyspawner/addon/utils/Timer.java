package liuliuliu0127.donkeyspawner.addon.utils;

public class Timer {
    public long getMsTime() {
        return this.time;
    }

    public long getTime() {
        return this.time;
    }

    private long time = -1L;

    public boolean passedS(double s) {
        return (getMs(System.currentTimeMillis() - this.time) >= (long) (s * 1000.0D));
    }

    public boolean passedM(double m) {
        return (getMs(System.currentTimeMillis() - this.time) >= (long) (m * 1000.0D * 60.0D));
    }

    public boolean passedDms(double dms) {
        return (getMs(System.currentTimeMillis() - this.time) >= (long) (dms * 10.0D));
    }

    public boolean passedDs(double ds) {
        return (getMs(System.currentTimeMillis() - this.time) >= (long) (ds * 100.0D));
    }

    public boolean passedMs(long ms) {
        return (getMs(System.currentTimeMillis() - this.time) >= ms);
    }

    public boolean passedMs(double ms) {
        return (getMs(System.currentTimeMillis() - this.time) >= ms);
    }

    public Timer reset() {
        this.time = System.currentTimeMillis();
        return this;
    }

    public boolean sleep(long l) {
        if (System.nanoTime() / 1000000L - l >= l) {
            reset();
            return true;
        }
        return false;
    }

    public long getPassedTimeMs() {
        return getMs(System.currentTimeMillis() - this.time);
    }

    public long getMs(long time) {
        return time;
    }

    public boolean passed(int ms) {
        return (System.currentTimeMillis() - this.time >= ms);
    }

    public boolean passed(double ms) {
        return ((System.currentTimeMillis() - this.time) >= ms);
    }
}
