package main;

import main.conflict.ConflictDetector;
import main.core.ReplicaSet;
import main.schedule.ScheduleRunner;

import java.util.List;

import static main.schedule.ScheduleEvent.copy;
import static main.schedule.ScheduleEvent.write;

/**
 * ===========================================================================
 *  Assignment 3 - Vector Clock Engine
 * ===========================================================================
 *
 *  SCENARIOS
 *  ---------
 *  EXAM   - Exact Figure 1 from Midterm II Exercise 3 (3 sites, 7 steps).
 *           Final answer: V1=[2,0,0]  V2=[1,3,2]  V3=[1,0,2]
 *           (Q1,Q2)=BRANCHING  (Q1,Q3)=BRANCHING  (Q2,Q3)=LINEAR
 *
 *  VC-1   - Pure linear history: all sites copy from the same chain.
 *           No branching - shows what DOMINATED looks like.
 *
 *  VC-2   - Immediate branch: two sites write independently after copying
 *           the same source. Classic fork.
 *
 *  VC-3   - Convergence after branching: two sites write independently,
 *           then one copies the other. Shows how a branch can be "resolved"
 *           by a subsequent copy.
 *
 *  VC-4   - Why timestamps alone fail: two replicas with equal wall-clock
 *           timestamps but a genuine branching history.
 *
 *  VC-5   - Four sites: more complex merge chain. Exercises the max()
 *           propagation rule across multiple hops.
 *
 *  VC-6   - Cascading copies: S3 copies S2 which already copied S1.
 *           Shows how history propagates transitively.
 *
 * ===========================================================================
 */
public class Main {

    public static void main(String[] args) {

        examScenario();
        scenario_VC1_linearChain();
        scenario_VC2_immediateBranch();
        scenario_VC3_convergenceAfterBranch();
        scenario_VC4_whyTimestampsFail();
        scenario_VC5_fourSites();
        scenario_VC6_cascadingCopies();
    }

    // -------------------------------------------------------------------------
    // EXAM - Midterm II Figure 1 (exact)
    // -------------------------------------------------------------------------

    private static void examScenario() {
        header("EXAM: Midterm II Exercise 3 - Figure 1",
               "3 sites, Q replicated. All clocks start at [0,0,0].",
               "Schedule: S1:W  S2:C(S1)  S2:W  S3:C(S1)  S3:W  S2:C(S3)  S1:W");

        /*
         * Figure 1 (verbatim from exam):
         *
         *   S1         S2         S3
         *   W(Q)
         *              C(Q1)
         *              W(Q)
         *                         C(Q1)
         *                         W(Q)
         *              C(Q3)
         *   W(Q)
         *
         * Note: values are arbitrary (the exam cares about clocks, not values).
         * We use 10, 20, 30, ... to make the data store meaningful.
         */
        var rs  = new ReplicaSet(3, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(0, 10),   // S1: W(Q) -> value=10
            copy (1, 0),    // S2: C(S1)
            write(1, 20),   // S2: W(Q) -> value=20
            copy (2, 0),    // S3: C(S1)
            write(2, 30),   // S3: W(Q) -> value=30
            copy (1, 2),    // S2: C(S3)
            write(0, 40)    // S1: W(Q) -> value=40
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  Expected final clocks (from study guide worked example):");
        System.out.println("    V1 = [2, 0, 0]");
        System.out.println("    V2 = [1, 3, 2]");
        System.out.println("    V3 = [1, 0, 2]");

        var results = ConflictDetector.analyseAll(rs.all());
        ConflictDetector.printReport(results);

        System.out.println("  Expected answers:");
        System.out.println("    (Q1, Q2) -> BRANCHING  V1[0]=2 > V2[0]=1  but  V1[1]=0 < V2[1]=3");
        System.out.println("    (Q1, Q3) -> BRANCHING  V1[0]=2 > V3[0]=1  but  V1[2]=0 < V3[2]=2");
        System.out.println("    (Q2, Q3) -> LINEAR     V2 dominates V3 component-wise [1≥1, 3≥0, 2≥2]");
    }

    // -------------------------------------------------------------------------
    // VC-1: Pure linear chain - no conflicts anywhere
    // -------------------------------------------------------------------------

    private static void scenario_VC1_linearChain() {
        header("VC-1: Pure linear chain",
               "S1 writes, S2 copies S1, S3 copies S2.",
               "Each step dominates all previous -> no branching.");

        var rs  = new ReplicaSet(3, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(0, 10),   // S1: W(Q)
            copy (1, 0),    // S2: C(S1)
            copy (2, 1)     // S3: C(S2)
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  Intuition: each step strictly extends the previous history.");
        System.out.println("  S3 has seen everything S2 has, which has seen everything S1 has.");

        ConflictDetector.printReport(ConflictDetector.analyseAll(rs.all()));
    }

    // -------------------------------------------------------------------------
    // VC-2: Immediate branch after shared copy
    // -------------------------------------------------------------------------

    private static void scenario_VC2_immediateBranch() {
        header("VC-2: Immediate branch after shared copy",
               "S2 and S3 both copy S1, then each writes independently.",
               "Classic fork: two diverging histories from a common ancestor.");

        var rs  = new ReplicaSet(3, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(0, 10),   // S1: W(Q) - initial write, common ancestor
            copy (1, 0),    // S2: C(S1) - both fork from here
            copy (2, 0),    // S3: C(S1) - both fork from here
            write(1, 20),   // S2: W(Q) - S2's independent write
            write(2, 30)    // S3: W(Q) - S3's independent write
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  Intuition: S2 and S3 both started from S1's state [1,0,0].");
        System.out.println("  Then each wrote independently. Neither knows about the other's write.");
        System.out.println("  S1 hasn't written since -> linear with both others? Check the report.");

        ConflictDetector.printReport(ConflictDetector.analyseAll(rs.all()));
    }

    // -------------------------------------------------------------------------
    // VC-3: Convergence after branching
    // -------------------------------------------------------------------------

    private static void scenario_VC3_convergenceAfterBranch() {
        header("VC-3: Convergence after branching",
               "S2 and S3 branch (write independently), then S2 copies S3.",
               "Shows how a copy collapses a branch into a linear history.");

        var rs  = new ReplicaSet(3, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(0, 10),   // S1: W(Q) - shared start
            copy (1, 0),    // S2: C(S1)
            copy (2, 0),    // S3: C(S1)
            write(1, 20),   // S2: W(Q) - S2 writes independently
            write(2, 30),   // S3: W(Q) - S3 writes independently
            // At this point S2 and S3 are BRANCHING - we could print the report here
            copy (1, 2)     // S2: C(S3) - S2 adopts S3's history
            // Now S2 has seen both its own write AND S3's write -> dominates S3
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  Intuition: after S2: C(S3), S2's clock incorporates S3's history.");
        System.out.println("  S2 now dominates S3 -> the branch is resolved (from S2's perspective).");
        System.out.println("  S1 is still behind both - it missed all the action.");

        ConflictDetector.printReport(ConflictDetector.analyseAll(rs.all()));
    }

    // -------------------------------------------------------------------------
    // VC-4: Why timestamps alone fail
    // -------------------------------------------------------------------------

    private static void scenario_VC4_whyTimestampsFail() {
        header("VC-4: Why wall-clock timestamps cannot detect conflicts",
               "Two replicas modified at the 'same time' but with branching history.",
               "A timestamp-based system would not detect the conflict.");

        /*
         * S1 and S2 both start from S3's value at time T.
         * Both write at time T+1. A timestamp system sees two replicas
         * both with timestamp T+1 and cannot tell which is "correct",
         * nor can it detect that they diverged.
         *
         * Vector clocks see: S1=[1,0,1] and S2=[0,1,1] - clearly concurrent.
         */
        var rs  = new ReplicaSet(3, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(2, 100),   // S3: W(Q) at time T  - shared ancestor
            copy (0, 2),     // S1: C(S3) at time T - both start here
            copy (1, 2),     // S2: C(S3) at time T - both start here
            write(0, 200),   // S1: W(Q) at time T+1 - independent write
            write(1, 300)    // S2: W(Q) at time T+1 - independent write
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  A timestamp system would see: S1.ts = T+1, S2.ts = T+1.");
        System.out.println("  It cannot tell which is newer - they're equal.");
        System.out.println("  It certainly cannot detect that they're CONCURRENT (branching).");
        System.out.println("  Vector clocks make the branching explicit via the witness indices.");

        ConflictDetector.printReport(ConflictDetector.analyseAll(rs.all()));
    }

    // -------------------------------------------------------------------------
    // VC-5: Four sites - multi-hop propagation
    // -------------------------------------------------------------------------

    private static void scenario_VC5_fourSites() {
        header("VC-5: Four sites - multi-hop max() propagation",
               "History flows: S1->S2->S3, while S4 writes independently.",
               "Exercises the max() rule across multiple propagation hops.");

        var rs  = new ReplicaSet(4, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(0, 10),   // S1: W(Q)
            write(3, 50),   // S4: W(Q) - independent, parallel to S1's chain
            copy (1, 0),    // S2: C(S1) - picks up S1's history
            write(1, 20),   // S2: W(Q)
            copy (2, 1),    // S3: C(S2) - picks up S1+S2's history transitively
            write(2, 30)    // S3: W(Q)
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  S3 has transitively seen S1's write (via S2) even though S3");
        System.out.println("  never directly copied from S1. The max() rule propagates history.");
        System.out.println("  S4 wrote in parallel and was never seen by anyone -> branches with all.");

        ConflictDetector.printReport(ConflictDetector.analyseAll(rs.all()));
    }

    // -------------------------------------------------------------------------
    // VC-6: Cascading copies and full convergence
    // -------------------------------------------------------------------------

    private static void scenario_VC6_cascadingCopies() {
        header("VC-6: Cascading copies - full convergence",
               "Sites branch, then a chain of copies brings all into agreement.",
               "Final state: all replicas identical -> no conflicts anywhere.");

        var rs  = new ReplicaSet(3, 0);
        var run = new ScheduleRunner(rs.all());

        run.run(List.of(
            write(0, 10),   // S1: W(Q)
            write(1, 20),   // S2: W(Q) - S1 and S2 are now branching
            write(2, 30),   // S3: W(Q) - S3 is branching with everyone
            copy (0, 1),    // S1: C(S2) - S1 merges S2's history
            copy (0, 2),    // S1: C(S3) - S1 merges S3's history -> S1 now dominates
            copy (1, 0),    // S2: C(S1) - S2 gets S1's merged state
            copy (2, 0)     // S3: C(S1) - S3 gets S1's merged state
        ));

        run.printFinalState();

        System.out.println();
        System.out.println("  After S1 copies both S2 and S3, S1 has seen all writes.");
        System.out.println("  S2 and S3 then copy S1 -> all three now have identical clocks.");
        System.out.println("  Full convergence: all pairs LINEAR (or IDENTICAL).");

        ConflictDetector.printReport(ConflictDetector.analyseAll(rs.all()));
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static void header(String title, String... notes) {
        System.out.printf("%n%n");
        System.out.println("==================================================================");
        System.out.println("  " + title);
        for (String note : notes) System.out.println("  " + note);
        System.out.println("==================================================================");
        System.out.println();
    }
}
