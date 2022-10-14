package ch.dfx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.OpenTransactionDTOList;
import ch.dfx.api.data.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.OpenTransactionMasternodeDTO;
import ch.dfx.api.data.OpenTransactionVerifiedDTO;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class ApiOpenTransactionHandler {
  private static final Logger LOGGER = LogManager.getLogger(ApiOpenTransactionHandler.class);

  private final ApiAccessHandler apiAccessHandler;
  private final DefiDataProvider dataProvider;

  private final List<OpenTransactionMasternodeDTO> transactionMasternodeDTOList;

  /**
   * 
   */
  public ApiOpenTransactionHandler(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider) {
    this.apiAccessHandler = apiAccessHandler;
    this.dataProvider = dataProvider;

    this.transactionMasternodeDTOList = new ArrayList<>();
  }

  public List<OpenTransactionMasternodeDTO> getTransactionMasternodeDTOList() {
    return transactionMasternodeDTOList;
  }

  /**
   * 
   */
  public OpenTransactionDTOList getOpenTransactionList() throws DfxException {
    LOGGER.debug("getOpenTransactionList() ...");

    // ...
    transactionMasternodeDTOList.clear();

    // ...
    OpenTransactionDTOList openTransactionDTOList = apiAccessHandler.getOpenTransactionDTOList();

//    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
//      String rawTransaction = openTransactionDTO.getRawTransaction();
//
//      DefiTransactionData transactionData = dataProvider.decodeRawTransaction(rawTransaction);
//      DefiCustomData customData = dataProvider.decodeCustomTransaction(rawTransaction);
//
//      if ("CreateMasternode".equals(customData.getType())) {
//        handleMasternodeTransactions(transactionData, customData);
//        openTransactionDTO.setState(OpenTransactionStateEnum.WORK);
//      } else {
//        openTransactionDTO.setState(OpenTransactionStateEnum.INVALID);
//      }
//    }

    return openTransactionDTOList;
  }

  /**
   * 
   */
  public void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) throws DfxException {
    LOGGER.trace("sendOpenTransactionVerified() ...");
    apiAccessHandler.sendOpenTransactionVerified(openTransactionId, openTransactionVerifiedDTO);
  }

  /**
   * 
   */
  public void sendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO) throws DfxException {
    LOGGER.trace("sendOpenTransactionVerified() ...");
    apiAccessHandler.sendOpenTransactionInvalidated(openTransactionId, openTransactionInvalidatedDTO);
  }

//  /**
//   * 
//   */
//  public void writeOpenTransactionDTOList(@Nonnull OpenTransactionDTOList openTransactionDTOList) throws DfxException {
//    LOGGER.trace("writeOpenTransactionDTOList() ...");
//    apiAccessHandler.postOpenTransactionDTOList(openTransactionDTOList);
//  }

  /**
   * 
   */
  private void handleMasternodeTransactions(
      @Nonnull DefiTransactionData transactionData,
      @Nonnull DefiCustomData customData) {
    LOGGER.debug("handleMasternodeTransactions() ...");

    String transactionId = transactionData.getTxid();

    String ownerAddress = getMasternodeOwnerAddress(transactionData);
    String operatorAddress = getMasternodeOperatorAddress(customData);

    OpenTransactionMasternodeDTO openTransactionMasternodeDTO = new OpenTransactionMasternodeDTO();

    openTransactionMasternodeDTO.setTransactionId(transactionId);
    openTransactionMasternodeDTO.setOwnerAddress(ownerAddress);
    openTransactionMasternodeDTO.setOperatorAddress(operatorAddress);

    transactionMasternodeDTOList.add(openTransactionMasternodeDTO);
  }

  /**
   * 
   */
  private @Nullable String getMasternodeOwnerAddress(@Nonnull DefiTransactionData transactionData) {
    LOGGER.debug("getMasternodeOwnerAddress() ...");

    String ownerAddress = null;

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

    for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
      DefiTransactionScriptPubKeyData transactionScriptPubKeyData = transactionVoutData.getScriptPubKey();

      List<String> addressesList = transactionScriptPubKeyData.getAddresses();

      if (null != addressesList
          && 1 == addressesList.size()) {
        ownerAddress = addressesList.get(0);
        break;
      }
    }

    return ownerAddress;
  }

  /**
   * 
   */
  private @Nullable String getMasternodeOperatorAddress(@Nonnull DefiCustomData customData) {
    LOGGER.debug("getMasternodeOperatorAddress() ...");

    String operatorAddress = null;

    Map<String, Object> resultsMap = customData.getResults();

    if (null != resultsMap) {
      operatorAddress = (String) resultsMap.get("masternodeoperator");
    }

    return operatorAddress;
  }
}
