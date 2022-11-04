package ch.dfx.process;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import ch.dfx.process.data.ProcessInfoDTO;

/**
 * 
 */
public interface ProcessInfoService extends Remote {
  void sendProcessInfo(@Nonnull ProcessInfoDTO processInfoDTO) throws RemoteException;
}
