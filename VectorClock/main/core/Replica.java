package main.core;

/**
 * A single replica of data item Q at site Si.
 *
 * Each replica stores:
 *   - its current value (the local copy of Q)
 *   - its vector clock (the causal history of how this value was reached)
 *   - the site index it belongs to (0-based)
 *
 * Operations:
 *   localWrite(value)       - W(Q): change value, increment own clock entry
 *   copyFrom(other)         - C(Sj): adopt other's value and merge clocks
 *
 * The vector clock grows monotonically - it never decreases.
 * A higher clock value means more events have been incorporated.
 */
public class Replica {

    private final int     siteIndex;   // 0-based (S1 -> 0, S2 -> 1, S3 -> 2)
    private final String  siteLabel;   // "S1", "S2", "S3" - for display
    private       int     value;       // current value of data item Q
    private final VectorClock clock;   // causal history

    public Replica(int siteIndex, int numSites, int initialValue) {
        this.siteIndex  = siteIndex;
        this.siteLabel  = "S" + (siteIndex + 1);
        this.value      = initialValue;
        this.clock      = new VectorClock(numSites);
    }

    // -- Write operations ------------------------------------------------------

    /**
     * Local write W(Q) at this site.
     *
     * Changes the local value and increments this site's own clock entry.
     * No other site's clock entry is touched - this is a purely local event.
     *
     * @param newValue  the new value to write
     */
    public void localWrite(int newValue) {
        clock.localWrite(siteIndex);
        this.value = newValue;
        System.out.printf("  [%s] W(Q) -> value=%d   clock=%s%n",
                siteLabel, value, clock);
    }

    /**
     * Copy-from C(Sj): adopt the value and merge the clock from another replica.
     *
     * Steps (per lecture rules):
     *  1. V[this.siteIndex]++           (the copy event is local to this site)
     *  2. ∀k: V[k] = max(V[k], Vj[k])  (merge source's history)
     *  3. value = source.value          (adopt source's data)
     *
     * @param source  the replica being copied from
     */
    public void copyFrom(Replica source) {
        clock.copyFrom(siteIndex, source.clock);
        this.value = source.value;
        System.out.printf("  [%s] C(%s) -> value=%d  clock=%s%n",
                siteLabel, source.siteLabel, value, clock);
    }

    // -- Getters ---------------------------------------------------------------

    public int         getSiteIndex() { return siteIndex; }
    public String      getSiteLabel() { return siteLabel; }
    public int         getValue()     { return value; }
    public VectorClock getClock()     { return new VectorClock(clock); } // defensive copy
    public VectorClock        clockRef()     { return clock; }                  // package-internal

    @Override
    public String toString() {
        return String.format("%s(value=%d, clock=%s)", siteLabel, value, clock);
    }
}
