package ch.dfx.manager.checker.withdrawal;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.manager.ManagerUtils;
import ch.dfx.manager.data.SignedMessageCheckDTO;
import ch.dfx.manager.data.SignedMessageCheckDTOList;

/**
 * 
 */
public class SignMessageSignatureChecker {
  private static final Logger LOGGER = LogManager.getLogger(SignMessageSignatureChecker.class);

  // ...
  private final Path jsonSignatureCheckFile;

  /**
   * 
   */
  public SignMessageSignatureChecker(@Nonnull NetworkEnum network) {
    Objects.requireNonNull(network, "null 'network' not allowed");

    this.jsonSignatureCheckFile = Path.of("", "data", "javascript", network.toString(), "message-verification.json");
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkSignMessageSignature(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkSignMessageSignature()");

    TransactionWithdrawalDTOList checkedTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    // ...
    if (!transactionWithdrawalDTOList.isEmpty()) {
      SignedMessageCheckDTOList uncheckedSignedMessageCheckDTOList = createSignedMessageCheckDTOList(transactionWithdrawalDTOList);
      SignedMessageCheckDTOList checkedSignedMessageCheckDTOList = checkSignature(uncheckedSignedMessageCheckDTOList);

      // ...
      Map<Integer, TransactionWithdrawalDTO> idToTransactionWithdrawalDTOMap = new HashMap<>();
      transactionWithdrawalDTOList.forEach(dto -> idToTransactionWithdrawalDTOMap.put(dto.getId(), dto));

      Map<Integer, SignedMessageCheckDTO> idToCheckedSignedMessageCheckDTOMap = new HashMap<>();
      checkedSignedMessageCheckDTOList.forEach(dto -> idToCheckedSignedMessageCheckDTOMap.put(dto.getId(), dto));

      for (Entry<Integer, TransactionWithdrawalDTO> idToTransactionWithdrawalDTOMapEntry : idToTransactionWithdrawalDTOMap.entrySet()) {
        Integer id = idToTransactionWithdrawalDTOMapEntry.getKey();
        TransactionWithdrawalDTO transactionWithdrawalDTO = idToTransactionWithdrawalDTOMapEntry.getValue();

        SignedMessageCheckDTO checkedSignedMessageCheckDTO = idToCheckedSignedMessageCheckDTOMap.get(id);

        if (null != checkedSignedMessageCheckDTO
            && checkedSignedMessageCheckDTO.isValid()) {
          transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.SIGNATURE_CHECKED);
          checkedTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
        } else {
          ManagerUtils.setWithdrawalCheckInvalidReason(transactionWithdrawalDTO, "invalid signature");
        }
      }
    }

    return checkedTransactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private SignedMessageCheckDTOList createSignedMessageCheckDTOList(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("createSignedMessageCheckDTOList()");

    SignedMessageCheckDTOList signedMessageCheckDTOList = new SignedMessageCheckDTOList();

    for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
      PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();

      SignedMessageCheckDTO signedMessageCheckDTO = new SignedMessageCheckDTO();

      signedMessageCheckDTO.setId(pendingWithdrawalDTO.getId());
      signedMessageCheckDTO.setMessage(pendingWithdrawalDTO.getSignMessage());
      signedMessageCheckDTO.setAddress(transactionWithdrawalDTO.getCustomerAddress());
      signedMessageCheckDTO.setSignature(pendingWithdrawalDTO.getSignature());

      signedMessageCheckDTOList.add(signedMessageCheckDTO);
    }

    return signedMessageCheckDTOList;
  }

  /**
   * 
   */
  private SignedMessageCheckDTOList checkSignature(@Nonnull SignedMessageCheckDTOList uncheckedSignedMessageCheckDTOList) {
    LOGGER.trace("checkSignature()");

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
  private void writeSignatureCheckFile(@Nonnull SignedMessageCheckDTOList signedMessageCheckDTOList) throws DfxException {
    LOGGER.trace("writeCheckFile()");

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
    LOGGER.trace("executeSignatureCheck()");

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

  /**
   * 
   */
  private SignedMessageCheckDTOList readSignatureCheckFile() throws DfxException {
    LOGGER.trace("readSignatureCheckFile()");
    return TransactionCheckerUtils.fromJson(jsonSignatureCheckFile.toFile(), SignedMessageCheckDTOList.class);
  }
}
