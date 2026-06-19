package main.conflict;

import main.core.Replica;
import main.core.VectorClock;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyses all pairs of replicas and classifies their relationship.
 *
 * -- Three possible relationships (from lecture) ---------------------------
 *
 *  1. IDENTICAL   - same clock, same value; trivially consistent
 *
 *  2. LINEAR      - one clock dominates the other component-wise
 *                   The dominant replica is simply a newer version.
 *                   Resolution: keep the dominant one, discard the other.
 *
 *  3. BRANCHING   - neither dominates; clocks are concurrent
 *                   ∃ x,y: Vi[x] < Vj[x]  AND  Vi[y] > Vj[y]
 *                   The replicas diverged from a common ancestor.
 *                   Resolution: requires explicit reconciliation (merge, user decision,
 *                   last-writer-wins, etc.) - cannot be resolved automatically.
 *
 * -- Why timestamps alone are not enough ----------------------------------
 *
 *  A plain timestamp tells you *when* the last write happened, but not
 *  *what history* led to that value. Two replicas with the same timestamp
 *  could still have branching histories if they received different updates.
 *  Vector clocks capture causality - not just recency.
 */
public class ConflictDetector {

    public enum Relationship { IDENTICAL, LINEAR, BRANCHING }

    /** Result for one pair of replicas. */
    public static class PairResult {
        public final Replica    a;
        public final Replica    b;
        public final Relationship relationship;
        public final Replica    newer;       // non-null iff LINEAR
        public final Replica    older;       // non-null iff LINEAR
        public final String     evidence;    // human-readable proof

        PairResult(Replica a, Replica b, Relationship rel,
                   Replica newer, Replica older, String evidence) {
            this.a            = a;
            this.b            = b;
            this.relationship = rel;
            this.newer        = newer;
            this.older        = older;
            this.evidence     = evidence;
        }
    }

    /**
     * Analyse all pairs of replicas.
     *
     * @param replicas  list of replicas (one per site)
     * @return          one PairResult per unique pair
     */
    public static List<PairResult> analyseAll(List<Replica> replicas) {
        List<PairResult> results = new ArrayList<>();
        int n = replicas.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                results.add(analyse(replicas.get(i), replicas.get(j)));
            }
        }
        return results;
    }

    /**
     * Analyse a single pair of replicas.
     */
    public static PairResult analyse(Replica a, Replica b) {
        VectorClock va = a.clockRef();
        VectorClock vb = b.clockRef();
        int n = va.size();

        if (va.equals(vb)) {
            return new PairResult(a, b, Relationship.IDENTICAL, null, null,
                    "clocks are equal");
        }

        if (va.dominates(vb)) {
            String evidence = buildDominanceEvidence(va, vb, n, a.getSiteLabel(), b.getSiteLabel());
            return new PairResult(a, b, Relationship.LINEAR, a, b, evidence);
        }

        if (vb.dominates(va)) {
            String evidence = buildDominanceEvidence(vb, va, n, b.getSiteLabel(), a.getSiteLabel());
            return new PairResult(a, b, Relationship.LINEAR, b, a, evidence);
        }

        // Neither dominates -> concurrent -> branching
        String evidence = buildBranchingEvidence(va, vb, n, a.getSiteLabel(), b.getSiteLabel());
        return new PairResult(a, b, Relationship.BRANCHING, null, null, evidence);
    }

    // -- Evidence builders -----------------------------------------------------

    /**
     * Find specific indices proving dominance: ∀k: dominator[k] ≥ dominated[k].
     */
    private static String buildDominanceEvidence(VectorClock dom, VectorClock sub,
                                                  int n, String domLabel, String subLabel) {
        StringBuilder sb = new StringBuilder();
        sb.append(domLabel).append(" dominates ").append(subLabel).append(": ");
        for (int k = 0; k < n; k++) {
            sb.append(String.format("[%d]%d≥%d ", k, dom.get(k), sub.get(k)));
        }
        sb.append("-> ").append(domLabel).append(" is strictly newer");
        return sb.toString();
    }

    /**
     * Find witness indices (x, y) proving branching:
     *   Va[x] < Vb[x]  AND  Va[y] > Vb[y]
     */
    private static String buildBranchingEvidence(VectorClock va, VectorClock vb,
                                                   int n, String aLabel, String bLabel) {
        int xWhere_a_less = -1;   // index where Va[x] < Vb[x]
        int yWhere_a_more = -1;   // index where Va[y] > Vb[y]

        for (int k = 0; k < n; k++) {
            if (va.get(k) < vb.get(k) && xWhere_a_less == -1) xWhere_a_less = k;
            if (va.get(k) > vb.get(k) && yWhere_a_more == -1) yWhere_a_more = k;
        }

        return String.format(
            "BRANCHING: %s[%d]=%d < %s[%d]=%d  AND  %s[%d]=%d > %s[%d]=%d " +
            "-> parallel independent writes -> must reconcile",
            aLabel, xWhere_a_less, va.get(xWhere_a_less),
            bLabel, xWhere_a_less, vb.get(xWhere_a_less),
            aLabel, yWhere_a_more, va.get(yWhere_a_more),
            bLabel, yWhere_a_more, vb.get(yWhere_a_more)
        );
    }

    // -- Pretty printer --------------------------------------------------------

    /**
     * Print a formatted report of all pair results.
     */
    public static void printReport(List<PairResult> results) {
        System.out.println();
        System.out.println("  ┌-----------------------------------------------------------------┐");
        System.out.println("  │  Conflict Detection Report                                      │");
        System.out.println("  └-----------------------------------------------------------------┘");

        for (PairResult r : results) {
            String pair = "(" + r.a.getSiteLabel() + ", " + r.b.getSiteLabel() + ")";
            System.out.printf("%n  Pair %-10s clock %s vs %s%n",
                    pair, r.a.clockRef(), r.b.clockRef());

            switch (r.relationship) {
                case IDENTICAL -> {
                    System.out.printf("    -> IDENTICAL - clocks equal, no conflict%n");
                }
                case LINEAR -> {
                    System.out.printf("    -> LINEAR    - %s is newer, %s can be discarded%n",
                            r.newer.getSiteLabel(), r.older.getSiteLabel());
                    System.out.printf("       Evidence: %s%n", r.evidence);
                }
                case BRANCHING -> {
                    System.out.printf("    -> BRANCHING *** CONFLICT - requires reconciliation ***%n");
                    System.out.printf("       Evidence: %s%n", r.evidence);
                }
            }
        }
        System.out.println();
    }
}
