package main.core;

import java.util.Arrays;

/**
 * A vector clock for N sites.
 *
 * Each site Si holds a vector V = [V[0], V[1], ..., V[N-1]] where:
 *   V[i] = number of write events that have happened at site i
 *          and are reflected in this replica's history.
 *
 * -- Rules (from lecture) --------------------------------------------------
 *
 *  Local write W(Q) at site i:
 *    V[i] <- V[i] + 1
 *
 *  Copy C(Sj) at site i  (copy value from Sj to Si):
 *    V[i] <- V[i] + 1
 *    for all k: V[k] <- max(V[k], Vj[k])
 *
 * -- Comparison semantics --------------------------------------------------
 *
 *  V dominates U (V ≥ U):  ∀k: V[k] ≥ U[k]     - V is at least as new as U
 *  V equals U:              ∀k: V[k] == U[k]
 *  Concurrent (conflict):   ∃i,j: V[i] < U[i] AND V[j] > U[j]
 *                           - branching history, must reconcile
 */
public class VectorClock {

    private final int[] vector;
    private final int n;

    /** Create a zero-initialised vector clock for n sites. */
    public VectorClock(int n) {
        this.n      = n;
        this.vector = new int[n];
    }

    /** Deep-copy constructor. */
    public VectorClock(VectorClock other) {
        this.n      = other.n;
        this.vector = Arrays.copyOf(other.vector, other.n);
    }

    // -- Mutation operations ---------------------------------------------------

    /**
     * Apply a local write at site siteIndex.
     * Increments V[siteIndex] by 1.
     *
     * @param siteIndex  0-based index of the site performing the write
     */
    public void localWrite(int siteIndex) {
        checkIndex(siteIndex);
        vector[siteIndex]++;
    }

    /**
     * Apply a copy-from operation at site destIndex, copying from a source clock.
     *
     * Steps:
     *  1. Increment destIndex's own counter (this is the copy event itself)
     *  2. Take component-wise max with the source clock
     *
     * @param destIndex   0-based index of the destination site (the site doing the copy)
     * @param sourceClock the vector clock of the site being copied from
     */
    public void copyFrom(int destIndex, VectorClock sourceClock) {
        checkIndex(destIndex);
        if (sourceClock.n != this.n)
            throw new IllegalArgumentException("Clock sizes must match");

        // Step 1: increment own counter (the copy is a local event at destIndex)
        vector[destIndex]++;

        // Step 2: component-wise max with source
        for (int k = 0; k < n; k++) {
            vector[k] = Math.max(vector[k], sourceClock.vector[k]);
        }
    }

    // -- Comparison ------------------------------------------------------------

    /**
     * Check if this clock dominates other (this ≥ other component-wise).
     * If true, this replica's history is a superset of other's history.
     */
    public boolean dominates(VectorClock other) {
        for (int k = 0; k < n; k++) {
            if (this.vector[k] < other.vector[k]) return false;
        }
        return true;
    }

    /**
     * Check if two clocks represent concurrent (branching) histories.
     *
     * Branching condition (from lecture):
     *   ∃ x, y such that this[x] < other[x]  AND  this[y] > other[y]
     *
     * Meaning: neither clock dominates the other -> they diverged from a
     * common ancestor via independent parallel writes -> conflict.
     */
    public boolean isConcurrentWith(VectorClock other) {
        boolean thisLessInSome  = false;  // ∃k: this[k] < other[k]
        boolean thisMoreInSome  = false;  // ∃k: this[k] > other[k]

        for (int k = 0; k < n; k++) {
            if (this.vector[k] < other.vector[k]) thisLessInSome = true;
            if (this.vector[k] > other.vector[k]) thisMoreInSome = true;
        }
        return thisLessInSome && thisMoreInSome;
    }

    /** True if both clocks are identical (same history). */
    public boolean equals(VectorClock other) {
        return Arrays.equals(this.vector, other.vector);
    }

    // -- Helpers ---------------------------------------------------------------

    public int get(int index) {
        checkIndex(index);
        return vector[index];
    }

    public int size() { return n; }

    public int[] toArray() { return Arrays.copyOf(vector, n); }

    private void checkIndex(int i) {
        if (i < 0 || i >= n)
            throw new IndexOutOfBoundsException("Site index " + i + " out of range [0," + n + ")");
    }

    @Override
    public String toString() {
        return Arrays.toString(vector);
    }
}
