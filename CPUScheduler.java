import java.util.ArrayList;
import java.util.List;

/**
 * Pure scheduling logic for the CPU Scheduling module (no Swing code).
 *
 * <p>Supports FCFS, SJF, Priority and Round Robin. SJF and Priority can run either
 * non-preemptive or preemptive (SJF preemptive = shortest remaining time first).
 * For Priority, a <b>higher</b> priority number means a <b>higher</b> priority.
 * Arrival, burst, priority and quantum values may be decimals.</p>
 */
public final class CPUScheduler {
    private CPUScheduler() {}

    /** One continuous stretch of CPU time. {@code process} is the row index, or -1 when the CPU is idle. */
    public static final class Segment {
        public final int process;
        public final double start;
        public final double end;

        Segment(int process, double start, double end) {
            this.process = process;
            this.start = start;
            this.end = end;
        }
    }

    /** Per-process timings (indexed like the input arrays) plus the Gantt segments. */
    public static final class Result {
        public final double[] completion;
        public final double[] turnaround;
        public final double[] waiting;
        public final double[] response;
        public final List<Segment> gantt = new ArrayList<>();

        Result(int n) {
            completion = new double[n];
            turnaround = new double[n];
            waiting = new double[n];
            response = new double[n];
        }

        public double averageWaiting() {
            return average(waiting);
        }

        public double averageTurnaround() {
            return average(turnaround);
        }

        private static double average(double[] values) {
            double sum = 0;
            for (double value : values) {
                sum += value;
            }
            return values.length == 0 ? 0 : sum / values.length;
        }
    }

    /**
     * Runs the chosen algorithm.
     *
     * @param algorithm  "FCFS", "SJF", "Priority" or "Round Robin"
     * @param preemptive only used by SJF and Priority
     * @param quantum    only used by Round Robin (must be greater than 0)
     */
    public static Result run(String algorithm, boolean preemptive, double quantum,
                             double[] arrival, double[] burst, double[] priority) {
        if ("Round Robin".equals(algorithm)) {
            return roundRobin(quantum, arrival, burst);
        }
        return byPriority(algorithm, preemptive, arrival, burst, priority);
    }

    /** Compatibility overload for callers that use whole-number scheduling inputs. */
    public static Result run(String algorithm, boolean preemptive, int quantum,
                             int[] arrival, int[] burst, int[] priority) {
        double[] decimalArrival = new double[arrival.length];
        double[] decimalBurst = new double[burst.length];
        double[] decimalPriority = new double[priority.length];
        for (int i = 0; i < arrival.length; i++) {
            decimalArrival[i] = arrival[i];
            decimalBurst[i] = burst[i];
            decimalPriority[i] = priority[i];
        }
        return run(algorithm, preemptive, quantum, decimalArrival, decimalBurst, decimalPriority);
    }

    // ------------------------------------------------- FCFS / SJF / Priority

    /** Event-driven simulation; a running process is only replaced when {@code preemptive} is true. */
    private static Result byPriority(String algorithm, boolean preemptive,
                                     double[] arrival, double[] burst, double[] priority) {
        int n = arrival.length;
        Result result = new Result(n);
        double[] remaining = burst.clone();
        double[] firstStart = new double[n];
        java.util.Arrays.fill(firstStart, -1);
        boolean canPreempt = preemptive && !"FCFS".equals(algorithm);

        double time = 0;
        int finished = 0;
        int running = -1;
        while (finished < n) {
            if (running < 0) {
                running = pick(algorithm, time, arrival, remaining, priority);
                if (running < 0) {
                    // Nothing has arrived yet: the CPU idles until the next arrival.
                    double next = nextArrival(arrival, remaining, time);
                    addSegment(result, -1, time, next);
                    time = next;
                    continue;
                }
                if (firstStart[running] < 0) {
                    firstStart[running] = time;
                }
            }

            // Run until the process finishes, or (when preemption is allowed) until the next arrival.
            double finish = time + remaining[running];
            double next = nextArrival(arrival, remaining, time);
            double end = canPreempt && next < finish ? next : finish;
            boolean completes = end == finish;
            addSegment(result, running, time, end);
            remaining[running] = completes ? 0 : finish - end;
            time = end;

            if (completes) {
                result.completion[running] = time;
                finished++;
                running = -1;
            } else if (canPreempt) {
                running = -1; // Re-evaluate now that a new process has arrived.
            }
        }
        fillTimings(result, arrival, burst, firstStart);
        return result;
    }

    /** Picks the best ready process for the algorithm, or -1 when none has arrived. */
    private static int pick(String algorithm, double time, double[] arrival, double[] remaining,
                            double[] priority) {
        int best = -1;
        for (int i = 0; i < arrival.length; i++) {
            if (remaining[i] > 0 && arrival[i] <= time && (best < 0 || better(algorithm, i, best,
                    arrival, remaining, priority))) {
                best = i;
            }
        }
        return best;
    }

    /** True when process {@code a} should run before {@code b}. Ties fall back to arrival, then row order. */
    private static boolean better(String algorithm, int a, int b, double[] arrival,
                                  double[] remaining, double[] priority) {
        int comparison = 0;
        if ("SJF".equals(algorithm)) {
            comparison = Double.compare(remaining[a], remaining[b]);
        } else if ("Priority".equals(algorithm)) {
            comparison = Double.compare(priority[b], priority[a]); // higher number first
        }
        if (comparison == 0) {
            comparison = Double.compare(arrival[a], arrival[b]);
        }
        return comparison < 0;
    }

    // ------------------------------------------------------------ Round Robin

    private static Result roundRobin(double quantum, double[] arrival, double[] burst) {
        int n = arrival.length;
        Result result = new Result(n);
        double[] remaining = burst.clone();
        double[] firstStart = new double[n];
        java.util.Arrays.fill(firstStart, -1);
        boolean[] queued = new boolean[n];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();

        double time = 0;
        int finished = 0;
        enqueueArrivals(queue, queued, arrival, time);
        while (finished < n) {
            if (queue.isEmpty()) {
                // Idle until the next arrival.
                double next = nextArrival(arrival, remaining, time);
                addSegment(result, -1, time, next);
                time = next;
                enqueueArrivals(queue, queued, arrival, time);
                continue;
            }
            int current = queue.poll();
            if (firstStart[current] < 0) {
                firstStart[current] = time;
            }
            double slice = Math.min(quantum, remaining[current]);
            boolean completes = slice >= remaining[current];
            addSegment(result, current, time, time + slice);
            time += slice;
            remaining[current] = completes ? 0 : remaining[current] - slice;
            // Newly arrived processes join the queue before the pre-empted one returns to its tail.
            enqueueArrivals(queue, queued, arrival, time);
            if (!completes) {
                queue.add(current);
            } else {
                result.completion[current] = time;
                finished++;
            }
        }
        fillTimings(result, arrival, burst, firstStart);
        return result;
    }

    private static void enqueueArrivals(java.util.ArrayDeque<Integer> queue, boolean[] queued,
                                        double[] arrival, double time) {
        // Selection by arrival time keeps the order stable for processes arriving together.
        while (true) {
            int next = -1;
            for (int i = 0; i < arrival.length; i++) {
                if (!queued[i] && arrival[i] <= time && (next < 0 || arrival[i] < arrival[next])) {
                    next = i;
                }
            }
            if (next < 0) {
                return;
            }
            queued[next] = true;
            queue.add(next);
        }
    }

    // ---------------------------------------------------------------- shared

    /** The earliest arrival time strictly after {@code time} among unfinished processes. */
    private static double nextArrival(double[] arrival, double[] remaining, double time) {
        double next = Double.POSITIVE_INFINITY;
        for (int i = 0; i < arrival.length; i++) {
            if (remaining[i] > 0 && arrival[i] > time && arrival[i] < next) {
                next = arrival[i];
            }
        }
        return next;
    }

    /** Appends a segment, merging it with the previous one when it continues the same process. */
    private static void addSegment(Result result, int process, double start, double end) {
        if (end <= start) {
            return;
        }
        if (!result.gantt.isEmpty()) {
            Segment last = result.gantt.get(result.gantt.size() - 1);
            if (last.process == process && last.end == start) {
                result.gantt.set(result.gantt.size() - 1, new Segment(process, last.start, end));
                return;
            }
        }
        result.gantt.add(new Segment(process, start, end));
    }

    /** Turnaround = completion - arrival, waiting = turnaround - burst, response = first run - arrival. */
    private static void fillTimings(Result result, double[] arrival, double[] burst, double[] firstStart) {
        for (int i = 0; i < arrival.length; i++) {
            result.turnaround[i] = result.completion[i] - arrival[i];
            result.waiting[i] = result.turnaround[i] - burst[i];
            result.response[i] = firstStart[i] - arrival[i];
        }
    }
}
