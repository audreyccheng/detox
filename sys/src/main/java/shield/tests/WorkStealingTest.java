package shield.tests;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class WorkStealingTest {

  private static class CallableTask implements Callable<Integer> {

    private final CyclicBarrier barrier;

    CallableTask(CyclicBarrier barrier) {
      this.barrier = barrier;
    }

    @Override
    public Integer call() throws Exception {
      barrier.await();
      return 1;
    }
  }


  public static int testWorkStealing() throws Exception {
    final int parallelism = 4;
    final ForkJoinPool pool = new ForkJoinPool(parallelism);
    final CyclicBarrier barrier = new CyclicBarrier(parallelism);

    final List<CallableTask> callableTasks =
        Collections.nCopies(parallelism, new CallableTask(barrier));
    int result = pool.submit(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        int result = 0;
        // Deadlock in invokeAll(), rather than stealing work
        List<ForkJoinTask<Integer>> tasks =
            new LinkedList<ForkJoinTask<Integer>>();
        for (CallableTask t : callableTasks) {
          tasks.add(pool.submit(t));
        }
        for (ForkJoinTask<Integer> t : tasks) {
          result += t.join();
        }
        return result;
      }
    }).join();
    return result;
  }

  public static void main(String[] args) throws Exception {
    System.out.println("Result is " + testWorkStealing());
    System.out.println("Finished");
  }
}
