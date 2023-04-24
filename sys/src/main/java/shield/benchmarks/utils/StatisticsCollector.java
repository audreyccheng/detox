package shield.benchmarks.utils;

import shield.util.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility class that collects statistics in the code, and asynchronously flushes out datapoints to
 * disk. Can be used to measure elapsed time between an addBegin() and an addEnd() call or to log
 * down a single point, by calling addPoint() instead NB: very addBegin must be followed by an
 * addEnd call. There cannot be multiple outstanding addBegin() calls
 *
 * TODO: I haven't measured the performance impact of doing this over simply printing to stdout.
 *
 * @author ncrooks
 */
public class StatisticsCollector {

  /**
   * Interval between two disk flushes
   */
  private final long sleep_time = 10;

  /**
   * List of start times without a matching end time
   */
  private ConcurrentHashMap<Integer, Long> startTimes =
      new ConcurrentHashMap<Integer, Long>();
  /**
   * List of end times
   */
  private ConcurrentHashMap<Integer, Pair<Long, Long>> endTimes =
      new ConcurrentHashMap<Integer, Pair<Long, Long>>();
  /**
   * List of single datapoints
   */
  private ConcurrentLinkedQueue<Pair<Float, Object>> dataPoints =
      new ConcurrentLinkedQueue<Pair<Float, Object>>();
  /**
   * Buffered writer
   */
  private BufferedWriter out = null;
  /**
   * Time at which this experiment started
   */
  private long beginExperimentTime = 0;
  /**
   * Number of points logged
   */
  private int nbTrx = 0;
  /**
   * File to which the data will be output
   */
  private String fileName = "";

  public StatisticsCollector(String fileName) {

    try {
      this.out = new BufferedWriter(new FileWriter(fileName));
      this.fileName = fileName;
    } catch (IOException e) {
      e.printStackTrace();
    }
    createWriteBack();
  }

  /**
   * Add begin point from which should start counting
   */
  public synchronized int addBegin() {

    if (beginExperimentTime == 0) {
      beginExperimentTime = System.nanoTime();
    }
    int key = nbTrx++;
    startTimes.put(key, System.nanoTime());
    return key;
  }

  public static int addBegin(StatisticsCollector stats) {
    if (stats != null) {
      return stats.addBegin();
    } else {
      return 0;
    }
  }

  public synchronized void addEnd(int key) {

    endTimes.put(key, new Pair<Long, Long>(System.nanoTime(), 0L));
  }

  public static void addEnd(StatisticsCollector stats, int end) {
    if (stats != null) {
      stats.addEnd(end);
    }
  }


  public synchronized void addPoint(Object obj) {

    if (beginExperimentTime == 0) {
      beginExperimentTime = System.nanoTime();
    }
    Pair<Float, Object> p = new Pair<Float, Object>(
        toMillis(System.nanoTime() - beginExperimentTime), obj);
    dataPoints.add(p);
  }

  public static void addPoint(StatisticsCollector stats, Object obj) {
    if (stats != null) {
      stats.addPoint(obj);
    }
  }


  public void createWriteBack() {

    Thread wb = new Thread() {
      public void run() {

        try {
          Set<Map.Entry<Integer, Long>> beginEntries = null;
          Set<Map.Entry<Integer, Pair<Long, Long>>> endEntries = null;
          Iterator<Map.Entry<Integer, Long>> iterator = null;
          Map.Entry<Integer, Long> start = null;
          Pair<Long, Long> end = null;
          while (true) {
            beginEntries = startTimes.entrySet();
            endEntries = endTimes.entrySet();
            iterator = beginEntries.iterator();
            while (iterator.hasNext()) {
              start = iterator.next();
              end = endTimes.get(start.getKey());
              if (end == null) {
                /* Not yet added */
              } else {
                writeResults(start.getKey(), start.getValue(), end.getLeft());
                startTimes.remove(start.getKey());
                endTimes.remove(start.getKey());
              }
            }
            Pair<Float, Object> point = null;
            while ((point = dataPoints.poll()) != null) {
              out.write(
                  point.getLeft() + " " + point.getRight().toString() + " \n");
              out.flush();
            }
            sleep(sleep_time);
          }
        } catch (Exception e) {
          System.exit(-1);
        }
      }
    };
    wb.setDaemon(true);
    wb.start();
  }

  public void end() {

    try {
      out.close();
    } catch (IOException ex) {
      System.exit(-1);
    }
  }

  public void writeResults(int trxId, long start, long end)
      throws IOException {

    if ((toMillis(end - start)) < 100) {
      out.write(trxId + " " + toMillis(start - beginExperimentTime) + " "
          + toMillis(end - start) + " " + " \n");
    } else {
      out.write(trxId + " " + toMillis(start - beginExperimentTime) + " "
          + toMillis(end - start) + " " + " \n");
    }

    out.flush();
  }

  public static float toMillis(long nano) {

    return (float) nano / (float) 1000000;
  }

  public String getFileName() {

    return fileName;
  }


}
