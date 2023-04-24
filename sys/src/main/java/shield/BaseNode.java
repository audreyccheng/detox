package shield;

import org.json.simple.parser.ParseException;
import shield.config.NodeConfiguration;
import shield.network.NetworkManager;
import shield.network.messages.Msg;
import shield.util.Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

import static shield.util.Logging.Level.CRITICAL;

/**
 * @author ncrooks
 *
 * This is the main class: a "block" is a node in the system, and can be extended to implement which
 * ever functionality necessary (both client and server). The only requirement is that all classes
 * that extend Block implement the {@link handleMsg} function to process and demultiplex incoming
 * messages
 */
public abstract class BaseNode {

  protected NetworkManager networkManager = null;
  protected NodeConfiguration config = null;
  protected ForkJoinPool workpool = null;

  protected BaseNode(String configFileName)
      throws InterruptedException, ParseException, IOException {
    assert(config == null);
     config =  config == null? new NodeConfiguration(configFileName) : config;
     init();
  }

  protected BaseNode(NodeConfiguration config) throws InterruptedException {
    this.config = config;
    init();
  }

  protected void init() throws InterruptedException {
   workpool = new ForkJoinPool(config.N_WORKER_THREADS,
        ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    networkManager = NetworkManager.createNetworkManager(this, config.NODE_IP_ADDRESS,
        config.NODE_LISTENING_PORT);
  }

  protected BaseNode(String configFileName, Map<Long, ReadWriteLock> keyLocks)
          throws InterruptedException, IOException, ParseException {
    assert(config == null);
    config = config == null ? new NodeConfiguration(configFileName) : config;
    init();
  }

  protected BaseNode(String configFileName, int port, int uid)
      throws InterruptedException, IOException, ParseException {
    assert(config == null);
    config = config == null ? new NodeConfiguration(configFileName) : config;
    config.NODE_LISTENING_PORT = port;
    config.NODE_UID = uid;
    init();
  }

  protected BaseNode(String configFileName, String address, int port, int uid)
      throws InterruptedException, IOException, ParseException {
    assert(config == null);
    config = config == null ? new NodeConfiguration(configFileName) : config;
    config.NODE_LISTENING_PORT = port;
    config.NODE_IP_ADDRESS = address;
    config.NODE_UID = uid;
    init();
  }

  /**
   * Test Constructor - initializes all constants to default parameters. Should not be used other
   * than for testing
   */
  public BaseNode() throws InterruptedException {
    config = config == null ? new NodeConfiguration() : config;
    init();
  }

  public BaseNode(String address, int port) throws InterruptedException {
    config = config == null ? new NodeConfiguration() : config;
    workpool = new ForkJoinPool(config.N_WORKER_THREADS,
        ForkJoinPool.defaultForkJoinWorkerThreadFactory, (Thread thread, Throwable throwable) -> {
        System.out.println(throwable.getMessage());
        System.out.println(throwable.getStackTrace());
        System.exit(-1);
    }, true);
    networkManager = NetworkManager.createNetworkManager(this, address, port);
  }


  public final NodeConfiguration getConfig() {
    return config;
  }

  public long getBlockId() {
    return config.NODE_UID;
  }

  public ForkJoinPool getWorkpool() {
    return workpool;
  }

  /**
   * Wrapper function to execute a task asynchronously in the workpool. This function is called if
   * the return type of the task to execute is U. Exceptions are unchecked.
   */
  public <U> CompletableFuture<U> executeAsyncUnchecked(Supplier<U> fn) {
    CompletableFuture<U> f = CompletableFuture.supplyAsync(fn, workpool);
    return f;
  }

  public CompletableFuture<Void> executeAsyncUnchecked(Runnable fn) {
    return CompletableFuture.runAsync(fn, workpool);
  }


  /**
   * Wrapper function to execute a task asynchronously in the workpool. This function is called if
   * the return type of the task to execute is U. Exceptions are handled via the supplied exception
   * handler.
   */

  public <U> CompletableFuture<U> executeAsync(Supplier<U> fn,
      Function<Throwable, U> exceptionHandler) {
    CompletableFuture<U> f = CompletableFuture.supplyAsync(fn, workpool);
    return f.exceptionally(exceptionHandler);
  }

  /**
   * Wrapper function to execute a task asynchronously in the workpool. This function is called if
   * the return type of the task to execute is void. Exceptions are handled via the supplied
   * exception handler.
   */
  public CompletableFuture<Void> executeAsync(Runnable fn,
      Function<Throwable, Void> exceptionHandler) {
    CompletableFuture<Void> f = CompletableFuture.runAsync(fn, workpool);
    return f.exceptionally(exceptionHandler);
  }


  /**
   * Wrapper function to execute a task asynchronously in the workpool, and register a callback when
   * the task finishes. This method is called when the function to be executed returns an element of
   * type U Exceptions are unchecked
   */
  public <U, T> CompletableFuture<T> executeAsyncWithCallbackUnchecked(
      Supplier<U> fn, Function<U, CompletableFuture<T>> fn2) {
    return CompletableFuture.supplyAsync(fn, workpool).thenComposeAsync(fn2,
        workpool);
  }


  /**
   * Wrapper function to execute a task asynchronously in the workpool, and register a callback when
   * the task finishes. This method is called when the function to be executed returns void
   */
  public CompletableFuture<Void> executeAsyncWithCallback(Runnable fn,
      Function<Void, CompletableFuture<Void>> fn2,
      Function<Throwable, Void> exceptionHandler) {
    CompletableFuture<Void> f = CompletableFuture.runAsync(fn, workpool)
        .thenComposeAsync(fn2, workpool);
    return f.exceptionally(exceptionHandler);
  }


  /**
   * Wrapper function to execute a task asynchronously in the workpool, and wait for the result.
   * This method is called when the function to be executed returns an element of type U. Due to the
   * join() call, this method can result in thread starvation, and may results to threads waiting
   * idly in the threadpool
   *
   * WARNING: Due to the join call, this function may cause thread starvation as it will cause
   * threads to block
   *
   * @param fn: function takes no arguments and returns an object of type U
   */
  public <U> U executeAsyncAndWaitUnchecked(Supplier<U> fn) {
    return CompletableFuture.supplyAsync(fn, workpool).join();
  }

  /**
   * Wrapper function to execute a task asynchronously in the workpool, and wait for the result.
   * This method is called when the function to be executed returns an element of type U
   *
   * WARNING: Due to the join call, this function may cause thread starvation as it will cause
   * threads to block
   *
   * @param fn function that takes no argument and returns void
   */
  public void executeAsyncAndWaitUnchecked(Runnable fn) {
    CompletableFuture.runAsync(fn, workpool).join();
  }

  /**
   * Returns a default exception handler parametrized for the return type of the called function
   */
  public <U> Function<Throwable, U> getDefaultExpHandler(U retType) {
    return (err -> {
      logErr(Thread.currentThread().getStackTrace()[1].getMethodName() + " "
          + err.getMessage(), CRITICAL);
      System.exit(-1);
      return null;
    });
  }

  /**
   * Returns a default exception handler parametrized for void functions
   */
  public Function<Throwable, Void> getDefaultExpHandler() {
    return (err -> {
      err.printStackTrace();
      logErr(Thread.currentThread().getStackTrace()[1].getMethodName() + " "
          + err.getMessage(), CRITICAL);
      System.exit(-1);
      return null;
    });
  }

  /**
   * Wrapper function to send message
   *
   * @param msg message to send
   * @param addr host,port of destination
   */
  public void sendMsg(Msg.Message msg, InetSocketAddress addr) {
    networkManager.sendMsg(msg, addr);
  }

  /**
   * Wrapper function to send message and force flush
   *
   * @param msg message to send
   * @param addr host,port of destination
   */
  public void sendMsg(Msg.Message msg, InetSocketAddress addr, boolean flush) {
    networkManager.sendMsg(msg, addr, flush);
  }


  public Logging getLogger() {
    return config.logger;
  }

  public void logOut(Object msg, Logging.Level level) {
    getLogger().printOut(msg, level);
  }

  public void logErr(Object msg, Logging.Level level) {
    getLogger().printErr(msg, level);
  }

  public void logOut(Object msg) {
    getLogger().printOut(msg);
  }

  public void logErr(Object msg) {
    getLogger().printErr(msg);
  }

  /**
   * Message processing function
   */
  public abstract void handleMsg(Msg.Message msg);

}
