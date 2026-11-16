import java.util.ArrayList;
import java.util.List;

/**
 * Pure scheduling logic for the CPU Scheduling module (no Swing code).
 *
 * <p>Supports FCFS, SJF, Priority and Round Robin. SJF and Priority can run either
 * non-preemptive or preemptive (SJF preemptive = shortest remaining time first).
 * For Priority, a <b>higher</b> priority number means a <b>higher</b> priority.
 * Time advances in whole units, so all inputs are integers.</p>
 */
public final class CPUScheduler {
    private CPUScheduler() {}

    /** One continuous stretch of CPU time. {@code process} is the row index, or -1 when the CPU is idle. */
    public static final class Segment {
        public final int process;
        public final int start;
        public final int end;

        Segment(int process, int start, int end) {
            this.process = process;
            this.start = start;
            this.end = end;
        }
    }

    /** Per-process timings (indexed like the input arrays) plus the Gantt segments. */
    public static final class Result {
        public final int[] completion;
        public final int[] turnaround;
        public final int[] waiting;
        public final int[] response;
        public final List<Segment> gantt = new ArrayList<>();

        Result(int n) {
            completion = new int[n];
            turnaround = new int[n];
            waiting = new int[n];
            response = new int[n];
        }

        public double averageWaiting() {
            return average(waiting);
        }

        public double averageTurnaround() {
            return average(turnaround);
        }

        private static double average(int[] values) {
            int sum = 0;
            for (int value : values) {
                sum += value;
            }
            return values.length == 0 ? 0 : (double) sum / values.length;
        }
    }

    /**
     * Runs the chosen algorithm.
     *
     * @param algorithm  "FCFS", "SJF", "Priority" or "Round Robin"
     * @param preemptive only used by SJF and Priority
     * @param quantum    only used by Round Robin (must be at least 1)
     */
    public static Result run(String algorithm, boolean preemptive, int quantum,
                             int[] arrival, int[] burst, int[] priority) {
        if ("Round Robin".equals(algorithm)) {
            return roundRobin(quantum, arrival, burst);
        }
        return byPriority(algorithm, preemptive, arrival, burst, priority);
    }

    // ------------------------------------------------- FCFS / SJF / Priority

    /** Unit-by-unit simulation; a running process is only replaced when {@code preemptive} is true. */
    private static Result byPriority(String algorithm, boolean preemptive,
                                     int[] arrival, int[] burst, int[] priority) {
        int n = arrival.length;
        Result result = new Result(n);
        int[] remaining = burst.clone();
        int[] firstStart = new int[n];
        java.util.Arrays.fill(firstStart, -1);
        boolean canPreempt = preemptive && !"FCFS".equals(algorithm);

        int time = 0;
        int finished = 0;
        int running = -1;
        while (finished < n) {
            // A running process keeps the CPU unless preemption is allowed.
            int chosen = (running >= 0 && !canPreempt) ? running
                    : pick(algorithm, time, arrival, burst, remaining, priority);
            if (chosen < 0) {
                addSegment(result, -1, time, time + 1); // nothing has arrived yet: CPU idles
                time++;
                continue;
            }
            if (firstStart[chosen] < 0) {
                firstStart[chosen] = time;
            }
            running = chosen;
            addSegment(result, chosen, time, time + 1);
            time++;
            remaining[chosen]--;
            if (remaining[chosen] == 0) {
                result.completion[chosen] = time;
                finished++;
                running = -1;
            }
        }
        fillTimings(result, arrival, burst, firstStart);
        return result;
    }

    /** Picks the best ready process for the algorithm, or -1 when none has arrived. */
    private static int pick(String algorithm, int time, int[] arrival, int[] burst,
                            int[] remaining, int[] priority) {
        int best = -1;
        for (int i = 0; i < arrival.length; i++) {
            if (remaining[i] > 0 && arrival[i] <= time && (best < 0 || better(algorithm, i, best,
                    arrival, burst, remaining, priority))) {
                best = i;
            }
        }
        return best;
    }

    /** True when process {@code a} should run before {@code b}. Ties fall back to arrival, then row order. */
    private static boolean better(String algorithm, int a, int b, int[] arrival, int[] burst,
                                  int[] remaining, int[] priority) {
        int comparison = 0;
        if ("SJF".equals(algorithm)) {
            comparison = Integer.compare(remaining[a], remaining[b]);
        } else if ("Priority".equals(algorithm)) {
            comparison = Integer.compare(priority[b], priority[a]); // higher number first
        }
        if (comparison == 0) {
            comparison = Integer.compare(arrival[a], arrival[b]);
        }
        return comparison < 0;
    }

    // ------------------------------------------------------------ Round Robin

    private static Result roundRobin(int quantum, int[] arrival, int[] burst) {
        int n = arrival.length;
        Result result = new Result(n);
        int[] remaining = burst.clone();
        int[] firstStart = new int[n];
        java.util.Arrays.fill(firstStart, -1);
        boolean[] queued = new boolean[n];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();

        int time = 0;
        int finished = 0;
        enqueueArrivals(queue, queued, arrival, time);
        while (finished < n) {
            if (queue.isEmpty()) {
                addSegment(result, -1, time, time + 1); // idle until the next arrival
                time++;
                enqueueArrivals(queue, queued, arrival, time);
                continue;
            }
            int current = queue.poll();
            if (firstStart[current] < 0) {
                firstStart[current] = time;
            }
            int slice = Math.min(quantum, remaining[current]);
            addSegment(result, current, time, time + slice);
            time += slice;
            remaining[current] -= slice;
            // Newly arrived processes join the queue before the pre-empted one returns to its tail.
            enqueueArrivals(queue, queued, arrival, time);
            if (remaining[current] > 0) {
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
                                        int[] arrival, int time) {
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

    /** Appends a segment, merging it with the previous one when it continues the same process. */
    private static void addSegment(Result result, int process, int start, int end) {
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
    private static void fillTimings(Result result, int[] arrival, int[] burst, int[] firstStart) {
        for (int i = 0; i < arrival.length; i++) {
            result.turnaround[i] = result.completion[i] - arrival[i];
            result.waiting[i] = result.turnaround[i] - burst[i];
            result.response[i] = firstStart[i] - arrival[i];
        }
    }
}
