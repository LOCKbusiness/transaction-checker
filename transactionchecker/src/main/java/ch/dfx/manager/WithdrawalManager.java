package ch.dfx.manager;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionRawTxDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.data.SignedMessageCheckDTO;
import ch.dfx.manager.data.SignedMessageCheckDTOList;

/**
 * 
 */
public class WithdrawalManager {
  private static final Logger LOGGER = LogManager.getLogger(WithdrawalManager.class);

  // ...
  private static final String SIGN_MESSAGE_FORMAT = "Withdraw_${amount}_${asset}_from_${address}_staking_id_${stakingId}_withdrawal_id_${withdrawalId}";

  private static final Pattern SIGN_MESSAGE_PATTERN =
      Pattern.compile("^Withdraw_(\\d+\\.?\\d*)_(.+)_from_(.+)_staking_id_(\\d+)_withdrawal_id_(\\d+)$", Pattern.DOTALL);
  private static final Matcher SIGN_MESSAGE_MATCHER = SIGN_MESSAGE_PATTERN.matcher("");

  // ...
  private final Path jsonSignatureCheckFile;

  // ...
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public WithdrawalManager(
      @Nonnull String network,
      @Nonnull DefiDataProvider dataProvider) {
    this.jsonSignatureCheckFile = Path.of("", "data", "javascript", network, "message-verification.json");

    this.dataProvider = dataProvider;
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkSignMessageFormat(@Nonnull TransactionWithdrawalDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("checkSignMessageFormat() ...");

    TransactionWithdrawalDTOList checkedWithdrawalTransactionDTOList = new TransactionWithdrawalDTOList();

    for (TransactionWithdrawalDTO withdrawalTransactionDTO : withdrawalTransactionDTOList) {
      if (checkSignMessageFormat(withdrawalTransactionDTO)) {
        checkedWithdrawalTransactionDTOList.add(withdrawalTransactionDTO);
      }
    }

    return checkedWithdrawalTransactionDTOList;
  }

  /**
   * 
   */
  private boolean checkSignMessageFormat(@Nonnull TransactionWithdrawalDTO withdrawalTransactionDTO) {
    LOGGER.trace("checkSignMessageFormat() ...");

    // ...
    try {
      OpenTransactionDTO openTransactionDTO = withdrawalTransactionDTO.getOpenTransactionDTO();

      OpenTransactionRawTxDTO openTransactionRawTxDTO = openTransactionDTO.getRawTx();
      String hex = openTransactionRawTxDTO.getHex();

      // ...
      DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);
//    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

      // ...
      String openTransactionId = openTransactionDTO.getId();
      String rawTransactionId = transactionData.getTxid();

      if (!Objects.equals(openTransactionId, rawTransactionId)) {
        setCheckSignatureMessage(withdrawalTransactionDTO, "transaction id not matches");
        return false;
      }

      // ...
      PendingWithdrawalDTO pendingWithdrawalDTO = withdrawalTransactionDTO.getPendingWithdrawalDTO();

      String signMessage = pendingWithdrawalDTO.getSignMessage();

      SIGN_MESSAGE_MATCHER.reset(signMessage);

      if (!SIGN_MESSAGE_MATCHER.matches()) {
        setCheckSignatureMessage(withdrawalTransactionDTO, "unknown sign message format");
        return false;
      }

      String signMessageAmount = SIGN_MESSAGE_MATCHER.group(1);
      String signMessageAsset = SIGN_MESSAGE_MATCHER.group(2);
      String signMessageAddress = SIGN_MESSAGE_MATCHER.group(3);
      String signMessageStakingId = SIGN_MESSAGE_MATCHER.group(4);

      // ...
      BigDecimal transactionOutAmount = getTransactionOutAmount(transactionData, signMessageAddress);

      boolean isSignMessageAmountValid =
          checkSignMessageAmount(new BigDecimal(signMessageAmount), pendingWithdrawalDTO.getAmount(), transactionOutAmount);

      if (!isSignMessageAmountValid) {
        setCheckSignatureMessage(withdrawalTransactionDTO, "invalid amount");
        return false;
      }

      // ...
      String checkSignMessage = SIGN_MESSAGE_FORMAT;

      checkSignMessage = checkSignMessage.replace("${amount}", signMessageAmount);
      checkSignMessage = checkSignMessage.replace("${asset}", signMessageAsset);
      checkSignMessage = checkSignMessage.replace("${address}", signMessageAddress);
      checkSignMessage = checkSignMessage.replace("${stakingId}", signMessageStakingId);
      checkSignMessage = checkSignMessage.replace("${withdrawalId}", withdrawalTransactionDTO.getId().toString());

      LOGGER.debug(signMessage);
      LOGGER.debug(checkSignMessage);

      if (!signMessage.equals(checkSignMessage)) {
        setCheckSignatureMessage(withdrawalTransactionDTO, "sign message mismatch");
        return false;
      }

      withdrawalTransactionDTO.setAddress(signMessageAddress);
      withdrawalTransactionDTO.setState(TransactionWithdrawalStateEnum.SIGN_MESSAGE_FORMAT_CHECKED);

      return true;
    } catch (Exception e) {
      LOGGER.error("checkSignMessageFormat", e);
      setCheckSignatureMessage(withdrawalTransactionDTO, e.getMessage());
      return false;
    }
  }

  /**
   * 
   */
  private void setCheckSignatureMessage(
      @Nonnull TransactionWithdrawalDTO withdrawalTransactionDTO,
      @Nonnull String messageInfo) {
    LOGGER.trace("setCheckSignatureMessage() ...");

    String message = "[Withdrawal] ID: " + withdrawalTransactionDTO.getId() + " - " + messageInfo;

    withdrawalTransactionDTO.setStateReason(message);
    withdrawalTransactionDTO.setState(TransactionWithdrawalStateEnum.INVALID);
  }

  /**
   * 
   */
  private boolean checkSignMessageAmount(
      @Nonnull BigDecimal signMessageAmount,
      @Nonnull BigDecimal pendingWithdrawalAmount,
      @Nonnull BigDecimal transactionOutAmount) {
    LOGGER.trace("checkSignMessageAmount() ...");

    boolean isValid = false;

    if (signMessageAmount.equals(pendingWithdrawalAmount)) {
      if (signMessageAmount.setScale(8, RoundingMode.DOWN).equals(transactionOutAmount.setScale(8, RoundingMode.DOWN))) {
        isValid = true;
      }
    }

    return isValid;
  }

  /**
   * 
   */
  private BigDecimal getTransactionOutAmount(
      @Nonnull DefiTransactionData transactionData,
      @Nonnull String address) {
    LOGGER.trace("getTransactionOutAmount() ...");

    BigDecimal outAmount = BigDecimal.ZERO;

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

    for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
      DefiTransactionScriptPubKeyData transactionScriptPubKeyData = transactionVoutData.getScriptPubKey();

      if (null != transactionScriptPubKeyData) {
        List<String> addressList = transactionScriptPubKeyData.getAddresses();

        if (addressList.contains(address)) {
          outAmount = outAmount.add(transactionVoutData.getValue());
        }
      }
    }

    return outAmount;
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkSignMessageSignature(@Nonnull TransactionWithdrawalDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("checkSignMessageSignature() ...");

    TransactionWithdrawalDTOList checkedWithdrawalTransactionDTOList = new TransactionWithdrawalDTOList();

    // ...
    if (!withdrawalTransactionDTOList.isEmpty()) {
      SignedMessageCheckDTOList uncheckedSignedMessageCheckDTOList = createSignedMessageCheckDTOList(withdrawalTransactionDTOList);
      SignedMessageCheckDTOList checkedSignedMessageCheckDTOList = checkSignature(uncheckedSignedMessageCheckDTOList);

      // ...
      Map<Integer, TransactionWithdrawalDTO> idToWithdrawalTransactionDTOMap = new HashMap<>();
      withdrawalTransactionDTOList.forEach(dto -> idToWithdrawalTransactionDTOMap.put(dto.getId(), dto));

      Map<Integer, SignedMessageCheckDTO> idToCheckedSignedMessageCheckDTOMap = new HashMap<>();
      checkedSignedMessageCheckDTOList.forEach(dto -> idToCheckedSignedMessageCheckDTOMap.put(dto.getId(), dto));

      for (Entry<Integer, TransactionWithdrawalDTO> idToWithdrawalTransactionDTOMapEntry : idToWithdrawalTransactionDTOMap.entrySet()) {
        Integer id = idToWithdrawalTransactionDTOMapEntry.getKey();
        TransactionWithdrawalDTO withdrawalTransactionDTO = idToWithdrawalTransactionDTOMapEntry.getValue();

        SignedMessageCheckDTO checkedSignedMessageCheckDTO = idToCheckedSignedMessageCheckDTOMap.get(id);

        if (null != checkedSignedMessageCheckDTO
            && checkedSignedMessageCheckDTO.isValid()) {
          withdrawalTransactionDTO.setState(TransactionWithdrawalStateEnum.SIGNATURE_CHECKED);
          checkedWithdrawalTransactionDTOList.add(withdrawalTransactionDTO);
        } else {
          withdrawalTransactionDTO.setStateReason("[Withdrawal] ID: " + withdrawalTransactionDTO.getId() + " - invalid signature");
          withdrawalTransactionDTO.setState(TransactionWithdrawalStateEnum.INVALID);
        }
      }
    }

    return checkedWithdrawalTransactionDTOList;
  }

  /**
   * 
   */
  private SignedMessageCheckDTOList createSignedMessageCheckDTOList(@Nonnull TransactionWithdrawalDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("createSignedMessageCheckDTOList() ...");

    SignedMessageCheckDTOList signedMessageCheckDTOList = new SignedMessageCheckDTOList();

    for (TransactionWithdrawalDTO withdrawalTransactionDTO : withdrawalTransactionDTOList) {
      if (TransactionWithdrawalStateEnum.SIGN_MESSAGE_FORMAT_CHECKED == withdrawalTransactionDTO.getState()) {
        PendingWithdrawalDTO pendingWithdrawalDTO = withdrawalTransactionDTO.getPendingWithdrawalDTO();

        SignedMessageCheckDTO signedMessageCheckDTO = new SignedMessageCheckDTO();

        signedMessageCheckDTO.setId(pendingWithdrawalDTO.getId());
        signedMessageCheckDTO.setMessage(pendingWithdrawalDTO.getSignMessage());
        signedMessageCheckDTO.setAddress(withdrawalTransactionDTO.getAddress());
        signedMessageCheckDTO.setSignature(pendingWithdrawalDTO.getSignature());

        signedMessageCheckDTOList.add(signedMessageCheckDTO);
      }
    }

    return signedMessageCheckDTOList;
  }

  /**
   * 
   */
  private SignedMessageCheckDTOList checkSignature(@Nonnull SignedMessageCheckDTOList uncheckedSignedMessageCheckDTOList) {
    LOGGER.trace("checkSignature() ...");

    try {
      writeSignatureCheckFile(uncheckedSignedMessageCheckDTOList);

      int exitCode = executeSignatureCheck();

      if (0 == exitCode) {
        return readSignatureCheckFile();
      }
    } catch (Exception e) {
      LOGGER.error("checkSignature", e);
    }

    return new SignedMessageCheckDTOList();
  }

  /**
   * 
   */
  private SignedMessageCheckDTOList readSignatureCheckFile() throws DfxException {
    LOGGER.trace("readSignatureCheckFile() ...");
    return TransactionCheckerUtils.fromJson(jsonSignatureCheckFile.toFile(), SignedMessageCheckDTOList.class);
  }

  /**
   * 
   */
  private void writeSignatureCheckFile(@Nonnull SignedMessageCheckDTOList signedMessageCheckDTOList) throws DfxException {
    LOGGER.trace("writeCheckFile() ...");

    try {
      if (!signedMessageCheckDTOList.isEmpty()) {
        Files.createDirectories(jsonSignatureCheckFile.getParent());
        Files.writeString(jsonSignatureCheckFile, signedMessageCheckDTOList.toString());
      }
    } catch (Exception e) {
      throw new DfxException("writeCheckFile", e);
    }
  }

  /**
   * 
   */
  private int executeSignatureCheck() throws DfxException {
    LOGGER.trace("executeSignatureCheck() ...");

    try {
      ProcessBuilder processBuilder = new ProcessBuilder();

      File javascriptExecutable;

      if (TransactionCheckerUtils.isWindows()) {
        javascriptExecutable = new File("javascript", "app-win.exe");
      } else {
        javascriptExecutable = new File("javascript", "app-macos");
      }

      File jsonSignatureCheckFilePath = jsonSignatureCheckFile.getParent().toFile();

      LOGGER.debug("JavaScript Executable: " + javascriptExecutable.getAbsolutePath());
      LOGGER.debug("JSON Check File Path: " + jsonSignatureCheckFilePath.getAbsolutePath());

      processBuilder.command(javascriptExecutable.getAbsolutePath(), jsonSignatureCheckFilePath.getAbsolutePath());

      processBuilder.redirectErrorStream(true);
      processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

      Process process = processBuilder.start();

      InputStream inputStream = process.getInputStream();
      String inputlog = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      LOGGER.debug("sub process input log: " + inputlog);

      // ...
      int exitCode = process.waitFor();
      LOGGER.debug("Exit Code: " + exitCode);

      inputStream.close();

      return exitCode;
    } catch (Exception e) {
      throw new DfxException("executeSignatureCheck", e);
    }
  }
}
