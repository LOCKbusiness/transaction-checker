package ch.dfx.manager;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionCustomTypeEnum;
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
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.checker.transaction.DuplicateChecker;
import ch.dfx.manager.checker.transaction.SignatureChecker;
import ch.dfx.manager.checker.transaction.SizeChecker;
import ch.dfx.manager.checker.transaction.TypeChecker;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class OpenTransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManager.class);

  // ...
  private final ApiAccessHandler apiAccessHandler;
  private final H2DBManager databaseManager;
  private final DefiDataProvider dataProvider;

  private final DatabaseHelper dataHelper;
  private final DefiMessageHandler messageHandler;
  private final WithdrawalManager withdrawalManager;

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
    this.databaseManager = databaseManager;
    this.dataProvider = dataProvider;

    this.dataHelper = new DatabaseHelper();
    this.messageHandler = new DefiMessageHandler(dataProvider);
    this.withdrawalManager = new WithdrawalManager(network, databaseManager, dataProvider);

    this.typeChecker = new TypeChecker(apiAccessHandler, dataProvider);
    this.sizeChecker = new SizeChecker(apiAccessHandler, dataProvider);
    this.duplicateChecker = new DuplicateChecker(apiAccessHandler, dataProvider, databaseManager);
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

    LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
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
    fillTypeOfOpenTransactionDTO(openTransactionDTO);
  }

  /**
   * 
   */
  private void fillTypeOfOpenTransactionDTO(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("fillTypeOfOpenTransactionDTO()");

    // Type ...
    OpenTransactionTypeEnum openTransactionType = OpenTransactionTypeEnum.UNKNOWN;

    OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

    if (null == openTransactionPayloadDTO) {
      openTransactionType = OpenTransactionTypeEnum.UTXO;
    } else {
      // TODO: New method, if the type is received within the payload ...
      ApiTransactionTypeEnum apiTransactionType =
          ApiTransactionTypeEnum.createByApiType(openTransactionPayloadDTO.getType());

      if (null != apiTransactionType) {
        openTransactionType = apiTransactionType.getOpenTransactionType();
      }

      // TODO: Old method, will be replaced by the method before ...
      if (OpenTransactionTypeEnum.UNKNOWN == openTransactionType) {
        if (null != openTransactionPayloadDTO.getOwnerWallet()) {
          openTransactionType = getTypeOfOpenTransactionWithOwnerWallet(openTransactionDTO);
        } else if (null != openTransactionPayloadDTO.getId()) {
          openTransactionType = OpenTransactionTypeEnum.WITHDRAWAL;
        }
      }
    }

    openTransactionDTO.setType(openTransactionType);
  }

  /**
   * 
   */
  private OpenTransactionTypeEnum getTypeOfOpenTransactionWithOwnerWallet(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("fillTypeOfOpenTransactionWithOwnerWallet()");

    OpenTransactionTypeEnum openTransactionType = OpenTransactionTypeEnum.UNKNOWN;

    try {
      String hex = openTransactionDTO.getRawTx().getHex();
      DefiCustomData decodeCustomTransaction = dataProvider.decodeCustomTransaction(hex);

      OpenTransactionCustomTypeEnum openTransactionCustomType =
          OpenTransactionCustomTypeEnum.createByChainType(decodeCustomTransaction.getType());

      if (null != openTransactionCustomType) {
        ApiTransactionTypeEnum apiTransactionType = openTransactionCustomType.getApiTransactionType();
        openTransactionType = apiTransactionType.getOpenTransactionType();
      } else {
        openTransactionType = OpenTransactionTypeEnum.UTXO;
      }
    } catch (Exception e) {
      LOGGER.error("fillTypeOfOpenTransactionWithOwnerWallet", e);
    }

    return openTransactionType;
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
   * YIELD_MASCHINE
   * WITHDRAWAL
   */
  private void processOpenTransactionAndWithdrawal(
      @Nonnull OpenTransactionDTOList workOpenTransactionDTOList,
      @Nonnull PendingWithdrawalDTOList apiPendingWithdrawalDTOList) throws DfxException {
    LOGGER.trace("processOpenTransactionAndWithdrawal()");

    // ...
    OpenTransactionDTOList utxoOpenTransactionDTOList = new OpenTransactionDTOList();
    OpenTransactionDTOList masternodeOpenTransactionDTOList = new OpenTransactionDTOList();
    OpenTransactionDTOList yieldMaschineOpenTransactionDTOList = new OpenTransactionDTOList();
    OpenTransactionDTOList withdrawalOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : workOpenTransactionDTOList) {
      OpenTransactionTypeEnum openTransactionType = openTransactionDTO.getType();

      switch (openTransactionType) {
        case UTXO: {
          utxoOpenTransactionDTOList.add(openTransactionDTO);
          break;
        }

        case MASTERNODE: {
          masternodeOpenTransactionDTOList.add(openTransactionDTO);
          break;
        }

        case YIELD_MASCHINE: {
          yieldMaschineOpenTransactionDTOList.add(openTransactionDTO);
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
    processOpenYieldMaschineTransaction(yieldMaschineOpenTransactionDTOList);
    processOpenWithdrawalTransaction(withdrawalOpenTransactionDTOList, apiPendingWithdrawalDTOList);
  }

  /**
   * 
   */
  private void processOpenUtxoTransaction(@Nonnull OpenTransactionDTOList utxoOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenUtxoTransaction()");

    for (OpenTransactionDTO openTransactionDTO : utxoOpenTransactionDTOList) {
      String hex = openTransactionDTO.getRawTx().getHex();

      DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);

      Set<String> voutAddressSet = getVoutAddressSet(transactionData);

      if (checkMasternodeWhitelist(new ArrayList<>(voutAddressSet))) {
        sendVerified(openTransactionDTO);
      } else {
        openTransactionDTO.setInvalidatedReason("[UTXO Transaction] ID: " + openTransactionDTO.getId() + " - address not in whitelist");
        sendInvalidated(openTransactionDTO);
      }
    }
  }

  /**
   * 
   */
  private void processOpenMasternodeTransaction(@Nonnull OpenTransactionDTOList masternodeOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenMasternodeTransaction()");

    for (OpenTransactionDTO openTransactionDTO : masternodeOpenTransactionDTOList) {
      String hex = openTransactionDTO.getRawTx().getHex();

      DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);

      Set<String> voutAddressSet = getVoutAddressSet(transactionData);

      if (checkMasternodeWhitelist(new ArrayList<>(voutAddressSet))) {
        sendVerified(openTransactionDTO);
      } else {
        openTransactionDTO.setInvalidatedReason("[Masternode Transaction] ID: " + openTransactionDTO.getId() + " - address not in whitelist");
        sendInvalidated(openTransactionDTO);
      }
    }
  }

  /**
   * 
   */
  private void processOpenYieldMaschineTransaction(@Nonnull OpenTransactionDTOList yieldMaschineOpenTransactionDTOList) throws DfxException {
    LOGGER.trace("processOpenYieldMaschineTransaction()");

    for (OpenTransactionDTO openTransactionDTO : yieldMaschineOpenTransactionDTOList) {
      String hex = openTransactionDTO.getRawTx().getHex();

      DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);

      Set<String> voutAddressSet = getVoutAddressSet(transactionData);

      if (checkMasternodeWhitelist(new ArrayList<>(voutAddressSet))) {
        sendVerified(openTransactionDTO);
      } else {
        openTransactionDTO.setInvalidatedReason("[Yield Maschine Transaction] ID: " + openTransactionDTO.getId() + " - address not in whitelist");
        sendInvalidated(openTransactionDTO);
      }
    }
  }

  /**
   * 
   */
  private Set<String> getVoutAddressSet(@Nonnull DefiTransactionData transactionData) {
    Set<String> voutAddressSet = new HashSet<>();

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

    for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
      DefiTransactionScriptPubKeyData transactionScriptPubKeyData = transactionVoutData.getScriptPubKey();

      if (null != transactionScriptPubKeyData) {
        List<String> addressList = transactionScriptPubKeyData.getAddresses();

        if (null != addressList) {
          voutAddressSet.addAll(addressList);
        }
      }
    }

    return voutAddressSet;
  }

  /**
   * 
   */
  private boolean checkMasternodeWhitelist(List<String> voutAddressList) {
    // ...
    boolean isValid;

    // ...
    BitSet bitSet = new BitSet(voutAddressList.size());
    bitSet.clear();

    // ...
    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      dataHelper.openStatements(connection);

      // ...
      Set<String> liquidityAddressSet = new HashSet<>();
      List<StakingAddressDTO> stakingAddressDTOList = dataHelper.getStakingAddressDTOList();
      stakingAddressDTOList.forEach(dto -> liquidityAddressSet.add(dto.getLiquidityAddress()));

      // ...
      for (int i = 0; i < voutAddressList.size(); i++) {
        String voutAddress = voutAddressList.get(i);

        if (liquidityAddressSet.contains(voutAddress)) {
          bitSet.set(i);
        } else {
          MasternodeWhitelistDTO masternodeWhitelistDTOByOwnerAddress =
              dataHelper.getMasternodeWhitelistDTOByOwnerAddress(voutAddress);

          if (null != masternodeWhitelistDTOByOwnerAddress
              && voutAddress.equals(masternodeWhitelistDTOByOwnerAddress.getOwnerAddress())) {
            bitSet.set(i);
          }
        }
      }

      isValid = bitSet.cardinality() == voutAddressList.size();

      dataHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkMasternodeWhitelist", e);
      isValid = false;
    } finally {
      databaseManager.closeConnection(connection);
    }

    return isValid;
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

      if (OpenTransactionTypeEnum.WITHDRAWAL == openTransactionDTO.getType()
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
