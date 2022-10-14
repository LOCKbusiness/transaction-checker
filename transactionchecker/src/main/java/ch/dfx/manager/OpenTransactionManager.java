package ch.dfx.manager;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiOpenTransactionHandler;
import ch.dfx.api.data.OpenTransactionDTO;
import ch.dfx.api.data.OpenTransactionDTOList;
import ch.dfx.api.data.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.OpenTransactionVerifiedDTO;
import ch.dfx.api.enumeration.InvalidReasonEnum;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.message.MessageHandler;

/**
 * 
 */
public class OpenTransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManager.class);

  private final DefiDataProvider dataProvider;
  private final MessageHandler messageHandler;

  /**
   * 
   */
  public OpenTransactionManager() {
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.messageHandler = new MessageHandler(dataProvider);
  }

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.trace("execute() ...");

    // ...
    ApiAccessHandler apiAccessHandler = new ApiAccessHandler();
    apiAccessHandler.signIn();

    // ...
    ApiOpenTransactionHandler openTransactionHandler = new ApiOpenTransactionHandler(apiAccessHandler, dataProvider);
    OpenTransactionDTOList openTransactionDTOList = openTransactionHandler.getOpenTransactionList();

    LOGGER.debug("Open Transaction size: " + openTransactionDTOList.size());

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      String openTransactionIssuerSignature = openTransactionDTO.getIssuerSignature();
      String openTransactionHex = openTransactionDTO.getRawTx().getHex();

      Boolean isValid = messageHandler.verifyMessage(openTransactionIssuerSignature, openTransactionHex);
      LOGGER.debug("Open Transaction Id: " + openTransactionDTO.getId() + " / " + isValid);

      String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

      if (BooleanUtils.isTrue(isValid)) {
        OpenTransactionVerifiedDTO openTransactionVerifiedDTO = new OpenTransactionVerifiedDTO();
        openTransactionVerifiedDTO.setSignature(openTransactionCheckerSignature);

        openTransactionHandler.sendOpenTransactionVerified(openTransactionDTO.getId(), openTransactionVerifiedDTO);
      } else {
        OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO = new OpenTransactionInvalidatedDTO();
        openTransactionInvalidatedDTO.setSignature(openTransactionCheckerSignature);
        openTransactionInvalidatedDTO.setReason(InvalidReasonEnum.INVALID_ISSUER_SIGNATURE.getReason());

        openTransactionHandler.sendOpenTransactionInvalidated(openTransactionDTO.getId(), openTransactionInvalidatedDTO);
      }
    }

    // ...
//    List<OpenTransactionMasternodeDTO> openTransactionMasternodeDTOList =
//        openTransactionHandler.getTransactionMasternodeDTOList();
//
//    if (!openTransactionMasternodeDTOList.isEmpty()) {
//      MasternodeBuilder masternodeBuilder = new MasternodeBuilder();
//      masternodeBuilder.build(openTransactionMasternodeDTOList);
//    }
//
//    // ...
//    openTransactionDTOList.stream()
//        .filter(dto -> OpenTransactionStateEnum.WORK == dto.getState())
//        .forEach(dto -> dto.setState(OpenTransactionStateEnum.PROCESSED));
//
//    openTransactionHandler.writeOpenTransactionDTOList(openTransactionDTOList);
  }
}
