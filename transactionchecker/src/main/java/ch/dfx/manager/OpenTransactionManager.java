package ch.dfx.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionTypeEnum;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.checker.transaction.CustomAddressChecker;
import ch.dfx.manager.checker.transaction.DuplicateChecker;
import ch.dfx.manager.checker.transaction.MasternodeWhitelistChecker;
import ch.dfx.manager.checker.transaction.SignatureChecker;
import ch.dfx.manager.checker.transaction.SizeChecker;
import ch.dfx.manager.checker.transaction.TypeChecker;
import ch.dfx.manager.checker.transaction.VaultWhitelistChecker;
import ch.dfx.manager.checker.transaction.VoutAddressChecker;
import ch.dfx.manager.filler.OpenTransactionDTOFiller;
import ch.dfx.manager.filler.PendingWithdrawalDTOFiller;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class OpenTransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManager.class);

  // ...
  private final ApiAccessHandler apiAccessHandler;
  private final DefiDataProvider dataProvider;

  private final DefiMessageHandler messageHandler;
  private final WithdrawalManager withdrawalManager;

  private final OpenTransactionDTOFiller openTransactionDTOFiller;
  private final PendingWithdrawalDTOFiller pendingWithdrawalDTOFiller;

  private final VoutAddressChecker voutAddressChecker;
  private final CustomAddressChecker customAddressChecker;

  private final TypeChecker typeChecker;
  private final SizeChecker sizeChecker;
  private final DuplicateChecker duplicateChecker;
  private final SignatureChecker signatureChecker;

  /**
   * 
   */
  public OpenTransactionManager(
      @Nonnull NetworkEnum network,
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull H2DBManager databaseManager,
      @Nonnull DefiDataProvider dataProvider) {
    this.apiAccessHandler = apiAccessHandler;
    this.dataProvider = dataProvider;

    this.messageHandler = new DefiMessageHandler(dataProvider);
    this.withdrawalManager = new WithdrawalManager(network, databaseManager, dataProvider);

    this.openTransactionDTOFiller = new OpenTransactionDTOFiller(apiAccessHandler, messageHandler, dataProvider);
    this.pendingWithdrawalDTOFiller = new PendingWithdrawalDTOFiller();

    MasternodeWhitelistChecker masternodeWhitelistChecker = new MasternodeWhitelistChecker(network, databaseManager);
    VaultWhitelistChecker vaultWhitelistChecker = new VaultWhitelistChecker(network, databaseManager);
    this.customAddressChecker = new CustomAddressChecker(apiAccessHandler, messageHandler, masternodeWhitelistChecker, vaultWhitelistChecker);
    this.voutAddressChecker = new VoutAddressChecker(apiAccessHandler, messageHandler, masternodeWhitelistChecker);

    this.typeChecker = new TypeChecker(apiAccessHandler, messageHandler, dataProvider);
    this.sizeChecker = new SizeChecker(apiAccessHandler, messageHandler);
    this.duplicateChecker = new DuplicateChecker(network, apiAccessHandler, messageHandler, databaseManager);
    this.signatureChecker = new SignatureChecker(apiAccessHandler, messageHandler);
  }

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.debug("execute()");

    long startTime = System.currentTimeMillis();

    // ...
    OpenTransactionDTOList apiOpenTransactionDTOList = apiAccessHandler.getOpenTransactionDTOList();
    PendingWithdrawalDTOList apiPendingWithdrawalDTOList = apiAccessHandler.getPendingWithdrawalDTOList();

    // ...
    LOGGER.debug(
        "[API] Transaction / Withdrawal Size: "
            + apiOpenTransactionDTOList.size() + " / " + apiPendingWithdrawalDTOList.size());

    // ...
    apiOpenTransactionDTOList.forEach(dto -> openTransactionDTOFiller.fillEmptyData(dto));
    apiPendingWithdrawalDTOList.forEach(dto -> pendingWithdrawalDTOFiller.fillEmptyData(dto));

    // ...
    OpenTransactionDTOList workOpenTransactionDTOList = openTransactionDTOFiller.fillChainTransactionDetail(apiOpenTransactionDTOList);

    workOpenTransactionDTOList = processOpenTransaction(workOpenTransactionDTOList);

    processOpenTransactionAndWithdrawal(workOpenTransactionDTOList, apiPendingWithdrawalDTOList);

    LOGGER.debug("[OpenTransactionManager] runtime: " + (System.currentTimeMillis() - startTime));
  }

  /**
   * Check 1: Check Type
   * Check 2: Transaction Hex Size
   * Check 3: Transaction (Withdrawal) Duplicated
   * Check 4: Transaction Signature
   */
  private OpenTransactionDTOList processOpenTransaction(@Nonnull OpenTransactionDTOList apiOpenTransactionDTOList) {
    LOGGER.trace("processOpenTransaction()");

    // Check 1: Check Type ...
    OpenTransactionDTOList workOpenTransactionDTOList = typeChecker.checkType(apiOpenTransactionDTOList);

    // Check 2: Transaction Hex Size ...
    workOpenTransactionDTOList = sizeChecker.checkSize(workOpenTransactionDTOList);

    // Check 3: Transaction (Withdrawal) Duplicated ...
    workOpenTransactionDTOList = duplicateChecker.checkDuplicated(workOpenTransactionDTOList);

    // Check 4: Transaction Signature ...
    return signatureChecker.checkTransactionSignature(workOpenTransactionDTOList);
  }

  /**
   * UTXO
   * MASTERNODE
   * YIELD_MACHINE
   * WITHDRAWAL
   */
  private void processOpenTransactionAndWithdrawal(
      @Nonnull OpenTransactionDTOList workOpenTransactionDTOList,
      @Nonnull PendingWithdrawalDTOList apiPendingWithdrawalDTOList) throws DfxException {
    LOGGER.trace("processOpenTransactionAndWithdrawal()");

    // ...
    OpenTransactionDTOList utxoOpenTransactionDTOList = new OpenTransactionDTOList();
    OpenTransactionDTOList masternodeOpenTransactionDTOList = new OpenTransactionDTOList();
    OpenTransactionDTOList yieldMachineOpenTransactionDTOList = new OpenTransactionDTOList();
    OpenTransactionDTOList withdrawalOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : workOpenTransactionDTOList) {
      OpenTransactionTypeEnum openTransactionType = openTransactionDTO.getType().getOpenTransactionType();

      switch (openTransactionType) {
        case UTXO: {
          utxoOpenTransactionDTOList.add(openTransactionDTO);
          break;
        }

        case MASTERNODE: {
          masternodeOpenTransactionDTOList.add(openTransactionDTO);
          break;
        }

        case YIELD_MACHINE: {
          yieldMachineOpenTransactionDTOList.add(openTransactionDTO);
          break;
        }

        case WITHDRAWAL: {
          withdrawalOpenTransactionDTOList.add(openTransactionDTO);
          break;
        }

        default:
          LOGGER.error("Unknown Transaction Type: " + openTransactionType);
      }
    }

    // ...
    processOpenUtxoTransaction(utxoOpenTransactionDTOList);
    processOpenMasternodeTransaction(masternodeOpenTransactionDTOList);
    processOpenYieldMachineTransaction(yieldMachineOpenTransactionDTOList);
    processOpenWithdrawalTransaction(withdrawalOpenTransactionDTOList, apiPendingWithdrawalDTOList);
  }

  /**
   * 
   */
  private void processOpenUtxoTransaction(@Nonnull OpenTransactionDTOList utxoOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenUtxoTransaction()");

    OpenTransactionDTOList workOpenTransactionDTOList = voutAddressChecker.checkVoutAddress(utxoOpenTransactionDTOList);

    for (OpenTransactionDTO openTransactionDTO : workOpenTransactionDTOList) {
      ManagerUtils.sendVerified(messageHandler, apiAccessHandler, openTransactionDTO);
    }
  }

  /**
   * 
   */
  private void processOpenMasternodeTransaction(@Nonnull OpenTransactionDTOList masternodeOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenMasternodeTransaction()");

    OpenTransactionDTOList workOpenTransactionDTOList = voutAddressChecker.checkVoutAddress(masternodeOpenTransactionDTOList);

    for (OpenTransactionDTO openTransactionDTO : workOpenTransactionDTOList) {
      ManagerUtils.sendVerified(messageHandler, apiAccessHandler, openTransactionDTO);
    }
  }

  /**
   * Check 1: Check Vout Address
   * Check 2: Check Custom Address
   */
  private void processOpenYieldMachineTransaction(
      @Nonnull OpenTransactionDTOList yieldMachineOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenYieldMachineTransaction()");

    // Check 1: Check Vout Address ...
    OpenTransactionDTOList workOpenTransactionDTOList = voutAddressChecker.checkVoutAddress(yieldMachineOpenTransactionDTOList);

    // Check 2: Check Custom Address ...
    workOpenTransactionDTOList = customAddressChecker.checkCustomAddress(workOpenTransactionDTOList);

    // ...
    for (OpenTransactionDTO openTransactionDTO : workOpenTransactionDTOList) {
      ManagerUtils.sendVerified(messageHandler, apiAccessHandler, openTransactionDTO);
    }
  }

  /**
   * 
   */
  private void processOpenWithdrawalTransaction(
      @Nonnull OpenTransactionDTOList withdrawalOpenTransactionDTOList,
      @Nonnull PendingWithdrawalDTOList apiPendingWithdrawalDTOList) throws DfxException {
    LOGGER.trace("processOpenWithdrawalTransaction()");

    // ...
    TransactionWithdrawalDTOList transactionWithdrawalDTOList =
        createTransactionWithdrawalDTOList(withdrawalOpenTransactionDTOList, apiPendingWithdrawalDTOList);

    if (!transactionWithdrawalDTOList.isEmpty()) {
      processPendingWithdrawal(transactionWithdrawalDTOList);
      send(transactionWithdrawalDTOList);
    }
  }

  /**
   * List of all Transactions and the corresponding Withdrawals ...
   */
  private TransactionWithdrawalDTOList createTransactionWithdrawalDTOList(
      @Nonnull OpenTransactionDTOList openTransactionDTOList,
      @Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) throws DfxException {
    LOGGER.trace("createTransactionWithdrawalDTOList()");

    TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    Map<Integer, OpenTransactionDTO> withdrawalIdToOpenTransactionDTOMap = createWithdrawalIdToOpenTransactionDTOMap(openTransactionDTOList);
    Map<Integer, PendingWithdrawalDTO> withdrawalIdToPendingWithdrawalDTOMap = createWithdrawalIdToPendingWithdrawalDTOMap(pendingWithdrawalDTOList);

    for (Entry<Integer, OpenTransactionDTO> withdrawalIdToOpenTransactionDTOMapEntry : withdrawalIdToOpenTransactionDTOMap.entrySet()) {
      Integer withdrawalId = withdrawalIdToOpenTransactionDTOMapEntry.getKey();

      OpenTransactionDTO openTransactionDTO = withdrawalIdToOpenTransactionDTOMapEntry.getValue();
      PendingWithdrawalDTO pendingWithdrawalDTO = withdrawalIdToPendingWithdrawalDTOMap.get(withdrawalId);

      if (null != pendingWithdrawalDTO) {
        TransactionWithdrawalDTO transactionWithdrawalDTO = new TransactionWithdrawalDTO();

        transactionWithdrawalDTO.setId(withdrawalId);
        transactionWithdrawalDTO.setOpenTransactionDTO(openTransactionDTO);
        transactionWithdrawalDTO.setPendingWithdrawalDTO(pendingWithdrawalDTO);

        transactionWithdrawalDTOList.add(transactionWithdrawalDTO);
      } else {
        openTransactionDTO.setInvalidatedReason("[Withdrawal Transaction] ID: " + openTransactionDTO.getId() + " - withdrawal id not found");
        ManagerUtils.sendInvalidated(messageHandler, apiAccessHandler, openTransactionDTO);
      }
    }

    return transactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private Map<Integer, OpenTransactionDTO> createWithdrawalIdToOpenTransactionDTOMap(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("createWithdrawalIdToOpenTransactionDTOMap()");

    Map<Integer, OpenTransactionDTO> withdrawalIdToOpenTransactionDTOMap = new HashMap<>();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      Integer withdrawalId = getWithdrawalId(openTransactionDTO);

      if (OpenTransactionTypeEnum.WITHDRAWAL == openTransactionDTO.getType().getOpenTransactionType()
          && null != withdrawalId) {
        withdrawalIdToOpenTransactionDTOMap.put(withdrawalId, openTransactionDTO);
      }
    }

    return withdrawalIdToOpenTransactionDTOMap;
  }

  /**
   * 
   */
  private Map<Integer, PendingWithdrawalDTO> createWithdrawalIdToPendingWithdrawalDTOMap(@Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) {
    LOGGER.trace("createWithdrawalIdToPendingWithdrawalDTOMap()");

    Map<Integer, PendingWithdrawalDTO> withdrawalIdToPendingWithdrawalDTOMap = new HashMap<>();

    for (PendingWithdrawalDTO pendingWithdrawalDTO : pendingWithdrawalDTOList) {
      Integer pendingWithdrawalId = pendingWithdrawalDTO.getId();

      if (null != pendingWithdrawalId) {
        withdrawalIdToPendingWithdrawalDTOMap.put(pendingWithdrawalId, pendingWithdrawalDTO);
      }
    }

    return withdrawalIdToPendingWithdrawalDTOMap;
  }

  /**
   * 
   */
  private @Nullable Integer getWithdrawalId(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("getWithdrawalId()");

    Integer withdrawalId = null;

    OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

    if (null != openTransactionPayloadDTO) {
      withdrawalId = openTransactionPayloadDTO.getId();
    }

    return withdrawalId;
  }

  /**
   * Check 1: Check the sign message format
   * Check 2: Check the message signature
   * Check 3: Check staking balance
   */
  private void processPendingWithdrawal(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("processPendingWithdrawal()");

    // Check 1: Check the sign message format ...
    TransactionWithdrawalDTOList checkTransactionWithdrawalDTOList = withdrawalManager.checkSignMessageFormat(transactionWithdrawalDTOList);

    // Check 2: Check the message signature ...
    checkTransactionWithdrawalDTOList = withdrawalManager.checkSignMessageSignature(checkTransactionWithdrawalDTOList);

    // Check 3: Check staking balance ...
    withdrawalManager.checkStakingBalance(checkTransactionWithdrawalDTOList);
  }

  /**
   * 
   */
  private void send(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) throws DfxException {
    LOGGER.trace("send()");

    for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
      OpenTransactionDTO openTransactionDTO = transactionWithdrawalDTO.getOpenTransactionDTO();

      if (TransactionWithdrawalStateEnum.BALANCE_CHECKED == transactionWithdrawalDTO.getState()) {
        ManagerUtils.sendVerified(messageHandler, apiAccessHandler, openTransactionDTO);
      } else {
        openTransactionDTO.setInvalidatedReason(transactionWithdrawalDTO.getStateReason());
        ManagerUtils.sendInvalidated(messageHandler, apiAccessHandler, openTransactionDTO);
      }
    }
  }
}
