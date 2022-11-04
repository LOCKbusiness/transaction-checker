package ch.dfx.transactionserver.scheduler;

import java.util.TimerTask;

/**
 * Not used at the moment ...
 */
public abstract class TimerProviderTask extends TimerTask {
  public abstract boolean isProcessing();
}
