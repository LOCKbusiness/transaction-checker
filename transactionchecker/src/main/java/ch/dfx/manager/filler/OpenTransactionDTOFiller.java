package ch.dfx.manager.filler;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionRawTxDTO;
import ch.dfx.api.enumeration.ApiTransactionTypeEnum;
import ch.dfx.common.errorhandling.DefiChainException;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.ManagerUtils;

/**
 * 
 */
public class OpenTransactionDTOFiller {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionDTOFiller.class);

  // ...
  private final ApiAccessHandler apiAccessHandler;
  private final DefiMessageHandler messageHandler;
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public OpenTransactionDTOFiller(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiMessageHandler messageHandler,
      @Nonnull DefiDataProvider dataProvider) {
    this.apiAccessHandler = apiAccessHandler;
    this.messageHandler = messageHandler;
    this.dataProvider = dataProvider;
  }

  /**
   * Set all used data fields to empty values or default values,
   * if they are not set via the API ...
   */
  public void fillEmptyData(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("fillEmptyData()");

    // ID ...
    openTransactionDTO.setId(TransactionCheckerUtils.emptyIfNull(openTransactionDTO.getId()));

    // Signature ...
    openTransactionDTO.setIssuerSignature(TransactionCheckerUtils.emptyIfNull(openTransactionDTO.getIssuerSignature()));

    // Raw Transaction ...
    OpenTransactionRawTxDTO openTransactionRawTxDTO = openTransactionDTO.getRawTx();

    if (null == openTransactionRawTxDTO) {
      openTransactionRawTxDTO = new OpenTransactionRawTxDTO();
      openTransactionDTO.setRawTx(openTransactionRawTxDTO);
    }

    openTransactionRawTxDTO.setHex(TransactionCheckerUtils.emptyIfNull(openTransactionRawTxDTO.getHex()));

    // Type ...
    OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

    if (null != openTransactionPayloadDTO) {
      ApiTransactionTypeEnum apiTransactionType =
          ApiTransactionTypeEnum.createByApiType(openTransactionPayloadDTO.getType());

      openTransactionDTO.setType(apiTransactionType);
    }
  }

  /**
   * 
   */
  public OpenTransactionDTOList fillChainTransactionDetail(@Nonnull OpenTransactionDTOList openTransactionDTOList) throws DfxException {
    LOGGER.trace("fillChainTransactionDetail()");

    OpenTransactionDTOList filledOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doFillChainTransactionDetail(openTransactionDTO)) {
        filledOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return filledOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doFillChainTransactionDetail(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("doFillChainTransactionDetail()");

    boolean isValid;

    try {
      String hex = openTransactionDTO.getRawTx().getHex();

      isValid = !StringUtils.isEmpty(hex);

      if (isValid) {
        DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);
        DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

        openTransactionDTO.setTransactionData(transactionData);
        openTransactionDTO.setTransactionCustomData(customData);
      }
    } catch (DefiChainException e) {
      isValid = false;
    }

    if (!isValid) {
      openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - invalid raw transaction");
      ManagerUtils.sendInvalidated(messageHandler, apiAccessHandler, openTransactionDTO);
    }

    return isValid;
  }
}
