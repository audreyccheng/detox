package shield.benchmarks.utils;

public class TrxStats {

  private long executeCount = 0;
  private long maxAborts = 0;
  private long totAborts = 0;
  private long timeExecuted= 0;

  public void addTransaction(int nbAborts, long executed) {
    executeCount++;
    maxAborts = nbAborts > maxAborts? nbAborts: maxAborts;
    totAborts+=nbAborts;
    timeExecuted+=executed;
  }

  public long getExecuteCount() {
    return executeCount;
  }

  public long getTimeExecuted() {
    return timeExecuted;
  }

  /**
   * Merges stats from a parallel threads. This TrxStats represents the aggregation of all threads.
   * @param stats Stats from a benchmark thread.
   */
  public void mergeTxnStats(TrxStats stats) {
    this.executeCount += stats.executeCount;
    this.maxAborts = Math.max(stats.maxAborts, this.maxAborts);
    this.totAborts += stats.totAborts;

    // Add time executed; not total elapsed time (that would be max()),
    // but total time summed across all threads for avg. latency calculation
    this.timeExecuted += stats.timeExecuted;
  }

  public String getStats() {
    return executeCount + " " + totAborts + " " + maxAborts + " " + ((float)timeExecuted/executeCount);
  }


}