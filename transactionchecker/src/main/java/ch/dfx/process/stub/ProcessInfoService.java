package ch.dfx.process.stub;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ch.dfx.process.data.ProcessInfoDTO;

/**
 * 
 */
public interface ProcessInfoService extends Remote {
  ProcessInfoDTO getProcessInfoDTO() throws RemoteException;
}
