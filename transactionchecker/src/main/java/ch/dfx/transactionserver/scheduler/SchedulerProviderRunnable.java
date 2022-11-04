package ch.dfx.transactionserver.scheduler;

/**
 * 
 */
public interface SchedulerProviderRunnable extends Runnable {
  public boolean isProcessing();
}
