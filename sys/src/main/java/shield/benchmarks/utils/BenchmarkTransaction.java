package shield.benchmarks.utils;

import java.util.Random;
import shield.client.ClientBase;

public abstract class BenchmarkTransaction {


  /**
   * Reference to client (will actually
   * execute the transaction)
   */
  protected ClientBase client;

  Random random = new Random();

  /**
   * Executes the transaction, first generating
   * input and then retrying as many times as is
   * necessary
   */
  public int run() {
      boolean success = false;
      int nbAborts = 0 ;
      int time = 10;
      double backOff = 0;
      while (!success) {
        success = tryRun();
        if (!success) {
          System.out.println("Aborting!");
          nbAborts++;

          // Backoff
          if (client.getConfig().USE_BACKOFF) {
            try {
              backOff = random.nextInt((int) Math.pow(2, nbAborts));
              System.out.println("Backing Off " + backOff);
              Thread.sleep((int) backOff);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }

        /*if (nbAborts>10) {
          System.err.println("Giving up this transaction");
          break; // Hack
        } */
      }
      return nbAborts;
  }
  /**
   * Attempts to execute one instance of the
   * transaction
   */
  public abstract boolean tryRun();


}
