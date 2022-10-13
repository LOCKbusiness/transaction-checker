package ch.dfx.manager;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.ApiOpenTransactionHandler;
import ch.dfx.api.data.OpenTransactionDTOList;
import ch.dfx.api.data.OpenTransactionMasternodeDTO;
import ch.dfx.api.enumeration.OpenTransactionStateEnum;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.transactionserver.builder.MasternodeBuilder;

/**
 * 
 */
public class MasternodeManager {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeManager.class);

  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public MasternodeManager() {
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
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
    OpenTransactionDTOList openTransactionDTOList = openTransactionHandler.selectOpenTransactions();

    // ...
    List<OpenTransactionMasternodeDTO> openTransactionMasternodeDTOList =
        openTransactionHandler.getTransactionMasternodeDTOList();

    if (!openTransactionMasternodeDTOList.isEmpty()) {
      MasternodeBuilder masternodeBuilder = new MasternodeBuilder();
      masternodeBuilder.build(openTransactionMasternodeDTOList);
    }

    // ...
    openTransactionDTOList.stream()
        .filter(dto -> OpenTransactionStateEnum.WORK == dto.getState())
        .forEach(dto -> dto.setState(OpenTransactionStateEnum.PROCESSED));

    openTransactionHandler.writeOpenTransactionDTOList(openTransactionDTOList);
  }
}
