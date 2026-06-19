package main.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A set of N replicas of the same data item Q, one per site.
 *
 * All replicas start with the same initial value and zero vector clocks.
 * This mirrors the exam setup: "all vectors are initialized with the zero vector."
 *
 * Usage:
 *   ReplicaSet rs = new ReplicaSet(3, 0);   // 3 sites, initial value = 0
 *   Replica s1 = rs.get(0);                 // S1
 *   Replica s2 = rs.get(1);                 // S2
 */
public class ReplicaSet {

    private final List<Replica> replicas;
    private final int numSites;

    /**
     * @param numSites      number of sites (e.g. 3 for S1, S2, S3)
     * @param initialValue  starting value of Q at every site
     */
    public ReplicaSet(int numSites, int initialValue) {
        this.numSites = numSites;
        this.replicas = new ArrayList<>(numSites);
        for (int i = 0; i < numSites; i++) {
            replicas.add(new Replica(i, numSites, initialValue));
        }
    }

    public Replica get(int siteIndex) {
        return replicas.get(siteIndex);
    }

    public List<Replica> all() {
        return Collections.unmodifiableList(replicas);
    }

    public int size() { return numSites; }
}
