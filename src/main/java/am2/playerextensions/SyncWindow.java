package am2.playerextensions;

/**
 * Shared tick-throttling state for the three synced extended-property stores. Each store keeps its own payload and
 * "has data" rule; this class only owns the countdown, delayed-sync and forced-sync semantics they duplicated.
 */
final class SyncWindow {

    private final int delay;
    private int ticksToSync;
    private boolean forcingSync;

    SyncWindow(int delay) {
        this.delay = delay;
    }

    int ticks() {
        return ticksToSync;
    }

    boolean isForcing() {
        return forcingSync;
    }

    void setTicks(int ticks) {
        this.ticksToSync = ticks;
    }

    void force() {
        this.forcingSync = true;
        this.ticksToSync = 0;
    }

    void fullSync() {
        this.ticksToSync = 0;
        this.forcingSync = true;
    }

    void clearForcing() {
        this.forcingSync = false;
    }

    boolean shouldSend(boolean isPlayer, boolean hasData) {
        if (!isPlayer && !forcingSync) {
            return false;
        }
        this.ticksToSync--;
        if (this.ticksToSync <= 0) this.ticksToSync = delay;
        return hasData && this.ticksToSync == delay;
    }
}
