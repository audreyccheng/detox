package shield.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility Class for Stack Dumping
 */
public class CallStack {

  private HashSet<String> callStack;
  private Logger logger;

  public CallStack(Logger logger) {

    this.callStack = new HashSet<String>();
    this.logger = logger;
  }

  public static  String dumpAllStackTraces() {

    String res = "";
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread
        .getAllStackTraces().entrySet()) {
      res += (entry.getKey().getId() + ":\n");
      for (StackTraceElement element : entry.getValue()) {
        res += ("\t " + element + "\n");
      }
    }
    return res;

  }

  public static String dumpTrace(long id) {

    String res = "";
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread
        .getAllStackTraces().entrySet()) {
      if (entry.getKey().getId() == id) {
        res += (entry.getKey().getId() + ":\n");
        for (StackTraceElement element : entry.getValue()) {
          res += (System.currentTimeMillis() + " \t " + element + "\n");
        }
        break;
      }
    }
    return res;

  }

  public void printStack() {

    Iterator<String> it = callStack.iterator();
    logger.log(Level.SEVERE, "BEGIN STACK TRACE ");
    int i = 0;
    while (it.hasNext()) {
      logger.log(Level.SEVERE, i + ": " + it.next());
      i++;
    }
    logger.log(Level.SEVERE, "END STACK TRACE");
  }

  public void putEntry(String name) {

    callStack.add(name + Thread.currentThread());
  }

  public void putExit(String name) {
    callStack.remove(name + Thread.currentThread());
  }

  public static String dumpTrace() {
    return dumpTrace(Thread.currentThread().getId());
  }
}
