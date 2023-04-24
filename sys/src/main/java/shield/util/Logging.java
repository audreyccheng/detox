package shield.util;

/**
 * @author ncrooks
 *
 * Logging Class: allows easy turn off/on of output based on desired level Automatically adds info
 * about thread id/class/method name. All code must use printErr/printOut in project for clean
 * management of debug output
 */
public class Logging {

  public enum Level {
    FINE, INFO, WARNING, CRITICAL, NONE
  }

  private static Logging logger;
  private Level logging_level;

  /**
   * @param log_level. Any output with inferior log level will be ignored
   * @param on. If set to false, no output will be generated
   */
  public static Logging getLogger(Level log_level) {
    if (logger == null) {
      logger = new Logging(log_level);
    } else {
      if (log_level != logger.logging_level) {
        logger.printErr(
            "WARNING: Trying to reinitialize logger with different logging level. Will be ignored",
            Logging.Level.CRITICAL);
      }
    }
    return logger;
  }


  private Logging(Level logging_level) {
    this.logging_level = logging_level;
    if (this.logging_level.compareTo(Level.NONE) == 0) {
      printErr("WARNING: no output will be generated", Logging.Level.INFO);
    }
  }

  /**
   * Utility method to generate method name, class which called the printOut/printErr methods
   *
   * @return formatted string of callee context
   */
  private String GetCalleeContext() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    String methodName = stackTrace[4].getClassName();
    String className = stackTrace[4].getMethodName();
    String file = stackTrace[4].getFileName();
    String lineNumber = Integer.toString(stackTrace[4].getLineNumber());
    String output = "[Thread-id:" + Thread.currentThread().getId() + "/"
        + className + ":" + methodName + "-" + file + ":" + lineNumber + "]";
    return output;
  }

  /**
   * Flush to stdout, except if log level set to NONE
   */

  public void printOut(Object m) {
    if (logging_level.compareTo(Level.NONE) != 0) {
      String msg = GetCalleeContext() + m.toString();
      System.out.println(msg);
    }
  }

  /**
   * Flush to stderr, except if log level set to NONE
   */
  public void printErr(Object m) {
    if (logging_level.compareTo(Level.NONE) != 0) {
      String msg = GetCalleeContext() + m;
      System.err.println(msg);
    }
  }

  /**
   * Flush to stderr if greater than current log level
   */

  public void printOut(Object m, Level log_level) {
    if (log_level.compareTo(this.logging_level) >= 0) {
      String msg = GetCalleeContext() + m;
      System.out.println(msg);
    }
  }

  /**
   * Flush to stdout if greater than current log level
   */

  public void printErr(Object m, Level log_level) {
    if (log_level.compareTo(this.logging_level) >= 0) {
      String msg = GetCalleeContext() + m.toString();
      System.err.println(msg);
    }
  }
}
