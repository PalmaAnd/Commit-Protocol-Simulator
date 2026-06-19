# Vector Clock Engine

## Setup

Requires Java 17+. No external dependencies.

```bash
# Compile (from the folder containing vectorclock/)
javac -d out $(find . -name "*.java")

# Run
java -cp out main.Main
```

## Project Structure

```
vectorclock/
├-- Main.java                          <- entry point, all 7 scenarios
├-- core/
│   ├-- main.java               <- the vector clock data structure + comparison rules
│   ├-- Replica.java                   <- one site: holds value + clock, applies W/C ops
│   └-- ReplicaSet.java                <- factory: N zero-initialised replicas
├-- schedule/
│   ├-- ScheduleEvent.java             <- one step: write(site, val) or copy(dest, src)
│   └-- ScheduleRunner.java            <- executes a schedule, prints each step
└-- conflict/
    └-- ConflictDetector.java          <- analyses all pairs: IDENTICAL / LINEAR / BRANCHING
```

---

## Pre-built Scenarios

| Scenario | What it demonstrates |
|----------|----------------------|
| EXAM     | Exact Figure 1 from Midterm II - verified against expected clocks |
| VC-1     | Pure linear chain - no branching anywhere |
| VC-2     | Immediate fork after shared copy - classic branch |
| VC-3     | Convergence after branching - how a copy collapses a branch |
| VC-4     | Why timestamps alone cannot detect conflicts |
| VC-5     | Four sites - multi-hop transitive max() propagation |
| VC-6     | Cascading copies leading to full convergence - all pairs LINEAR |

---

## Exercises

---

### Exercise A - Hand-trace Before Running

For each schedule below, compute the final vector clocks **by hand** using the two rules:

```
W(Q) at Si:    Vi[i] <- Vi[i] + 1
C(Sj) at Si:   Vi[i] <- Vi[i] + 1   then   ∀k: Vi[k] <- max(Vi[k], Vj[k])
```

Then create a `ReplicaSet` and `ScheduleRunner` in `Main.java` to verify.

**Schedule A1** (2 sites, init [0,0]):
```
S1: W(Q)
S2: C(S1)
S1: W(Q)
S2: W(Q)
```

Predict: V1 = ?, V2 = ?
Predict pair (S1,S2): LINEAR or BRANCHING?

**Schedule A2** (3 sites, init [0,0,0]):
```
S1: W(Q)
S2: W(Q)
S1: C(S2)
S3: C(S1)
S3: W(Q)
```

Predict: V1 = ?, V2 = ?, V3 = ?
Which pairs branch? Which are linear?

**Schedule A3** (3 sites, init [0,0,0]):
```
S1: W(Q)
S2: C(S1)
S2: W(Q)
S2: W(Q)
S3: C(S2)
S1: W(Q)
```

Predict all three final clocks and all three pair relationships.

---

### Exercise B - Implement `ConflictDetector.findAllWitnesses()`

The current `buildBranchingEvidence()` finds only the *first* pair of witness
indices (x, y) proving a branch. Add a method that finds **all** witnesses:

```java
/**
 * For a branching pair (Va, Vb), return every (x, y) index pair such that:
 *   Va[x] < Vb[x]  AND  Va[y] > Vb[y]
 *
 * The first witness is sufficient to prove branching, but all witnesses
 * together give a fuller picture of how the histories diverged.
 *
 * @return list of int[]{x, y} witness pairs; empty if not branching
 */
public static List<int[]> findAllWitnesses(VectorClock va, VectorClock vb) {
    // TODO
}
```

Test it on the EXAM scenario pair (Q1, Q2):
- V1 = [2,0,0], V2 = [1,3,2]
- Expected witnesses include (0,1): V1[0]=2 > V2[0]=1  AND  V1[1]=0 < V2[1]=3
- Are there other witnesses?

---

### Exercise C - Implement `main.merge()`

Sometimes two replicas need to merge without one "copying" the other (the copy
operation always increments the destination's own counter). Add a pure merge:

```java
/**
 * Return a new VectorClock that is the component-wise maximum of this and other.
 * Does NOT increment any site's counter - this is a pure merge.
 *
 * Use case: computing the "latest known state" combining two clocks without
 * recording a new write event.
 *
 * @param other  clock to merge with
 * @return       new clock with max(this[k], other[k]) for each k
 */
public VectorClock merge(VectorClock other) {
    // TODO
}
```

Then add a scenario that uses `merge()` to compute the "latest possible state"
after a network partition heals - both partitions write independently, then
merge their clocks to see the combined history.

---

### Exercise D - Implement `Reconciler`

Add a `Reconciler` class that resolves LINEAR conflicts automatically and
flags BRANCHING ones for manual resolution:

```java
package main.conflict;

import main.core.Replica;
import java.util.List;

public class Reconciler {

    public enum Strategy { LAST_WRITER_WINS, KEEP_HIGHER_VALUE, FLAG_FOR_USER }

    /**
     * Given a list of replicas and a strategy, attempt reconciliation.
     *
     * LINEAR pairs:   always auto-resolved by keeping the dominant replica.
     * BRANCHING pairs: resolved by strategy (or flagged if FLAG_FOR_USER).
     *
     * @return a new Replica representing the reconciled state, or null if
     *         manual intervention is needed.
     */
    public static Replica reconcile(Replica a, Replica b, Strategy strategy) {
        // TODO:
        // 1. Analyse the pair using ConflictDetector.analyse()
        // 2. If IDENTICAL or LINEAR: return the dominant replica
        // 3. If BRANCHING:
        //    - LAST_WRITER_WINS: compare clock sums, keep the one with higher sum
        //    - KEEP_HIGHER_VALUE: return the replica with the higher .getValue()
        //    - FLAG_FOR_USER: print a message and return null
    }
}
```

Test it on scenario VC-2 (the immediate branch):
- After S2 and S3 write independently, reconcile (S2, S3) with each strategy
- Do any strategies give a "correct" answer here? Which don't, and why?

---

### Exercise E - Build a Custom Schedule from the Exam Diagram

The exam may present you with a schedule diagram like Figure 1. Practice
converting diagrams to code by building this new one:

```
S1       S2       S3
                  W(Q)
         C(S3)
W(Q)
         W(Q)
                  C(S2)
W(Q)
         C(S1)
```

1. Read the diagram top-to-bottom, left-to-right for events within each column.
2. Write each event as `write(siteIndex, value)` or `copy(dest, src)` calls.
3. Run the schedule.
4. Before running, predict by hand which pairs will branch.

Hint: trace which sites have seen each other's writes at each step.

---

### Exercise F - Stress Test: N Sites, M Writes

Write a method that generates a random schedule for N sites and M total events,
then analyses all N*(N-1)/2 pairs:

```java
public static List<ScheduleEvent> randomSchedule(int numSites, int numEvents, long seed) {
    // Use java.util.Random with the seed for reproducibility.
    // Each event: pick a random target site.
    //   50% chance: W(Q) with a random value
    //   50% chance: C(Sj) from a random OTHER site
    // Return the list of events.
}
```

Run it for (N=5, M=20) and count how many pairs are:
- BRANCHING vs LINEAR vs IDENTICAL

Then add one more copy event at the end that copies the "most advanced" site
(highest sum of clock entries) to all others. How many conflicts remain?

---

## Key Rules to Memorise

```
W(Q) at Si:          Vi[i] <- Vi[i] + 1
C(Sj) at Si:         Vi[i] <- Vi[i] + 1
                      ∀k: Vi[k] <- max(Vi[k], Vj[k])
```

| Relationship | Condition | Action |
|---|---|---|
| IDENTICAL | ∀k: Vi[k] = Vj[k] | No conflict |
| LINEAR | One dominates (∀k: Vi[k] ≥ Vj[k]) | Keep dominant, discard other |
| BRANCHING | ∃x,y: Vi[x] < Vj[x] AND Vi[y] > Vj[y] | Must reconcile |

**Common mistakes:**
- Forgetting to increment the **destination** site's own counter in C(Sj)
- Applying max() before the increment (wrong order - increment first, then max)
- Confusing "dominates" with "strictly greater" - `[2,1,0]` dominates `[1,1,0]` even though they're equal in position 1
