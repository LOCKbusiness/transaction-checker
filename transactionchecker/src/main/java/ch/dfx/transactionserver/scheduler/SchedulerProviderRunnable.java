package ch.dfx.transactionserver.scheduler;

/**
 * 
 */
public interface SchedulerProviderRunnable extends Runnable {
  public String getName();

  public boolean isProcessing();
}
