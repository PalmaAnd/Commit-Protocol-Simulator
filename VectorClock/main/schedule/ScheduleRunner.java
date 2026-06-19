package main.schedule;

import main.core.Replica;

import java.util.List;

/**
 * Executes a schedule of W(Q) and C(Sj) operations across a set of replicas.
 *
 * A schedule is a list of {@link ScheduleEvent} objects. Each event targets
 * a specific site and is one of:
 *
 *   W(Q)    - local write at the target site
 *   C(Sj)   - copy from site j to the target site
 *
 * The runner executes events in order, printing each step and the resulting
 * clock state.  After the schedule completes, the final clock of each
 * replica is printed for conflict analysis.
 *
 * -- Reading a schedule diagram (from the exam / Figure 1) ----------------
 *
 *  Events in each column happen top-to-bottom. The column represents the site.
 *  Events in the same row happen simultaneously (not modelled here - we execute
 *  strictly in sequence per the exam convention, which treats concurrent events
 *  as having an arbitrary but fixed serial order).
 *
 *   S1       S2       S3
 *   W(Q)
 *            C(S1)              <- S2 copies from S1
 *            W(Q)
 *                     C(S1)    <- S3 copies from S1
 *                     W(Q)
 *            C(S3)              <- S2 copies from S3
 *   W(Q)
 */
public class ScheduleRunner {

    private final List<Replica> replicas;

    public ScheduleRunner(List<Replica> replicas) {
        this.replicas = replicas;
    }

    /**
     * Execute all events in the schedule in order.
     */
    public void run(List<ScheduleEvent> schedule) {
        System.out.println("  [Schedule execution]");
        System.out.printf("  %-5s %-8s %-8s%n", "Step", "Site", "Op");
        System.out.printf("  %-5s %-8s %-8s%n", "----", "----", "--");

        int step = 1;
        for (ScheduleEvent event : schedule) {
            System.out.printf("%n  Step %-3d: ", step++);
            executeEvent(event);
        }
    }

    private void executeEvent(ScheduleEvent event) {
        Replica target = replicas.get(event.targetSiteIndex());

        switch (event.type()) {
            case LOCAL_WRITE -> {
                System.out.printf("%s: W(Q) with value=%d%n",
                        target.getSiteLabel(), event.writeValue());
                target.localWrite(event.writeValue());
            }
            case COPY_FROM -> {
                Replica source = replicas.get(event.sourceSiteIndex());
                System.out.printf("%s: C(%s)%n",
                        target.getSiteLabel(), source.getSiteLabel());
                target.copyFrom(source);
            }
        }
    }

    /**
     * Print the final clock state of all replicas.
     */
    public void printFinalState() {
        System.out.println();
        System.out.println("  [Final replica state]");
        System.out.printf("  %-6s %-8s %-20s%n", "Site", "Value", "Vector Clock");
        System.out.printf("  %-6s %-8s %-20s%n", "----", "-----", "------------");
        for (Replica r : replicas) {
            System.out.printf("  %-6s %-8d %-20s%n",
                    r.getSiteLabel(), r.getValue(), r.clockRef());
        }
    }
}
