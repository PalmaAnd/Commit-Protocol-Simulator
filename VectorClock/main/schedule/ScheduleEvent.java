package main.schedule;

/**
 * One event in a schedule: either a local write or a copy-from.
 *
 * Use the static factory methods for readability:
 *
 *   ScheduleEvent.write(0, 42)   // S1: W(Q) with value 42
 *   ScheduleEvent.copy(1, 0)     // S2: C(S1)
 */
public record ScheduleEvent(
        Type   type,
        int    targetSiteIndex,   // site performing the operation (0-based)
        int    sourceSiteIndex,   // source site for COPY_FROM (−1 for LOCAL_WRITE)
        int    writeValue         // new value for LOCAL_WRITE (0 for COPY_FROM)
) {

    public enum Type { LOCAL_WRITE, COPY_FROM }

    /**
     * Local write at targetSite with the given value.
     *
     * @param targetSite  0-based site index (S1 -> 0, S2 -> 1, ...)
     * @param value       the new value of Q
     */
    public static ScheduleEvent write(int targetSite, int value) {
        return new ScheduleEvent(Type.LOCAL_WRITE, targetSite, -1, value);
    }

    /**
     * Copy from sourceSite to targetSite.
     *
     * @param targetSite  0-based site index of the destination
     * @param sourceSite  0-based site index of the source
     */
    public static ScheduleEvent copy(int targetSite, int sourceSite) {
        return new ScheduleEvent(Type.COPY_FROM, targetSite, sourceSite, 0);
    }
}
