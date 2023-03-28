package ch.dfx.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionRawTxDTO;
import ch.dfx.api.data.transaction.OpenTransactionTypeEnum;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
import ch.dfx.api.enumeration.ApiTransactionTypeEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.checker.transaction.CustomAddressChecker;
import ch.dfx.manager.checker.transaction.DuplicateChecker;
import ch.dfx.manager.checker.transaction.MasternodeWhitelistChecker;
import ch.dfx.manager.checker.transaction.SignatureChecker;
import ch.dfx.manager.checker.transaction.SizeChecker;
import ch.dfx.manager.checker.transaction.TypeChecker;
import ch.dfx.manager.checker.transaction.VoutAddressChecker;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class OpenTransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManager.class);

  // ...
  private final ApiAccessHandler apiAccessHandler;

  private final DefiMessageHandler messageHandler;
  private final WithdrawalManager withdrawalManager;

  private final MasternodeWhitelistChecker masternodeWhitelistChecker;
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

    this.messageHandler = new DefiMessageHandler(dataProvider);
    this.withdrawalManager = new WithdrawalManager(network, databaseManager, dataProvider);

    this.masternodeWhitelistChecker = new MasternodeWhitelistChecker(network, databaseManager);
    this.customAddressChecker = new CustomAddressChecker(apiAccessHandler, dataProvider, masternodeWhitelistChecker);
    this.voutAddressChecker = new VoutAddressChecker(apiAccessHandler, dataProvider, masternodeWhitelistChecker);

    this.typeChecker = new TypeChecker(apiAccessHandler, dataProvider);
    this.sizeChecker = new SizeChecker(apiAccessHandler, dataProvider);
    this.duplicateChecker = new DuplicateChecker(network, apiAccessHandler, dataProvider, databaseManager);
    this.signatureChecker = new SignatureChecker(apiAccessHandler, dataProvider);
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
    apiOpenTransactionDTOList.forEach(dto -> fillEmptyDataOfOpenTransactionDTO(dto));
    apiPendingWithdrawalDTOList.forEach(dto -> fillEmptyDataOfPendingWithdrawalDTO(dto));

    // ...
    OpenTransactionDTOList workOpenTransactionDTOList = processOpenTransaction(apiOpenTransactionDTOList);

    processOpenTransactionAndWithdrawal(workOpenTransactionDTOList, apiPendingWithdrawalDTOList);

    LOGGER.debug("[OpenTransactionManager] runtime: " + (System.currentTimeMillis() - startTime));
  }

  /**
   * Set all used data fields to empty values or default values,
   * if they are not set via the API ...
   */
  private void fillEmptyDataOfOpenTransactionDTO(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("fillEmptyDataOfReceivedDTO()");

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
   * Set all used data fields to empty values or default values,
   * if they are not set via the API ...
   */
  private void fillEmptyDataOfPendingWithdrawalDTO(@Nonnull PendingWithdrawalDTO pendingWithdrawalDTO) {
    LOGGER.trace("fillEmptyDataOfPendingWithdrawalDTO()");

    // ID ...
    pendingWithdrawalDTO.setId(TransactionCheckerUtils.zeroIfNull(pendingWithdrawalDTO.getId()));

    // Sign Message ...
    pendingWithdrawalDTO.setSignMessage(TransactionCheckerUtils.emptyIfNull(pendingWithdrawalDTO.getSignMessage()));

    // Signature ...
    pendingWithdrawalDTO.setSignature(TransactionCheckerUtils.emptyIfNull(pendingWithdrawalDTO.getSignature()));

    // Amount ...
    pendingWithdrawalDTO.setAmount(TransactionCheckerUtils.zeroIfNull(pendingWithdrawalDTO.getAmount()));

    // Token ...
    pendingWithdrawalDTO.setToken(TokenEnum.createWithText(pendingWithdrawalDTO.getAsset(), TokenEnum.DFI));
  }

  /**
   * Check 1: Check Typ
   * Check 2: Transaction Hex Size
   * Check 3: Transaction (Withdrawal) Duplicated
   * Check 4: Transaction Signature
   */
  private OpenTransactionDTOList processOpenTransaction(@Nonnull OpenTransactionDTOList apiOpenTransactionDTOList) {
    LOGGER.trace("processOpenTransaction()");

    // Check 1: Check Typ ...
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
      sendVerified(openTransactionDTO);
    }
  }

  /**
   * 
   */
  private void processOpenMasternodeTransaction(@Nonnull OpenTransactionDTOList masternodeOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenMasternodeTransaction()");

    OpenTransactionDTOList workOpenTransactionDTOList = voutAddressChecker.checkVoutAddress(masternodeOpenTransactionDTOList);

    for (OpenTransactionDTO openTransactionDTO : workOpenTransactionDTOList) {
      sendVerified(openTransactionDTO);
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
      sendVerified(openTransactionDTO);
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
        sendInvalidated(openTransactionDTO);
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
        sendVerified(openTransactionDTO);
      } else {
        openTransactionDTO.setInvalidatedReason(transactionWithdrawalDTO.getStateReason());
        sendInvalidated(openTransactionDTO);
      }
    }
  }

  /**
   * 
   */
  private void sendVerified(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendVerified()");

    String openTransactionHex = openTransactionDTO.getRawTx().getHex();
    String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

    OpenTransactionVerifiedDTO openTransactionVerifiedDTO = new OpenTransactionVerifiedDTO();
    openTransactionVerifiedDTO.setSignature(openTransactionCheckerSignature);

    apiAccessHandler.sendOpenTransactionVerified(openTransactionDTO.getId(), openTransactionVerifiedDTO);
  }

  /**
   * 
   */
  private void sendInvalidated(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendInvalidated()");

    String openTransactionHex = openTransactionDTO.getRawTx().getHex();
    String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

    OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO = new OpenTransactionInvalidatedDTO();
    openTransactionInvalidatedDTO.setSignature(openTransactionCheckerSignature);
    openTransactionInvalidatedDTO.setReason(openTransactionDTO.getInvalidatedReason());

    apiAccessHandler.sendOpenTransactionInvalidated(openTransactionDTO.getId(), openTransactionInvalidatedDTO);
  }
}
