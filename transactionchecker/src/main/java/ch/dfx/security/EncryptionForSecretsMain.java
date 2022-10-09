package ch.dfx.security;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class EncryptionForSecretsMain {
  private static final Logger LOGGER = LogManager.getLogger(EncryptionForSecretsMain.class);

  /**
   * 
   */
  public static void main(String[] args) {
    try {
      // ...
      System.setProperty("logFilename", "security");
      TransactionCheckerUtils.initLog4j("log4j2-security.xml");

      // just for convienience ...
//      encryptAllSecretProperties(args);

      // ...
      boolean isEncryption = Stream.of(args).anyMatch(a -> "--encrypt".equals(a));
      boolean isDecryption = Stream.of(args).anyMatch(a -> "--decrypt".equals(a));
      Optional<String> optionalPasswordArgument = Stream.of(args).filter(a -> a.startsWith("--password=")).findFirst();
      Optional<String> optionalFileArgument = Stream.of(args).filter(a -> a.startsWith("--file=")).findFirst();

      // ...
      if ((!isEncryption && !isDecryption)
          || optionalPasswordArgument.isEmpty()
          || optionalFileArgument.isEmpty()) {
        LOGGER.error("usage: [--encrypt] [--decrypt] --password=[PASSWORD] --file=[FILE]");
      } else {
        LOGGER.debug("=".repeat(80));
        File inputFile = new File(optionalFileArgument.get().split("=")[1]);
        String password = optionalPasswordArgument.get().split("=")[1];

        if (isEncryption) {
          doFileEncryption(inputFile, password);
        } else if (isDecryption) {
          doFileDecryption(inputFile, password);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }

    System.exit(0);
  }

  /**
   * 
   */
  private static void encryptAllSecretProperties(@Nonnull String[] args) throws DfxException {
    Optional<String> optionalPasswordArgument = Stream.of(args).filter(a -> a.startsWith("--password=")).findFirst();

    if (optionalPasswordArgument.isEmpty()) {
      LOGGER.error("usage: --password=[PASSWORD]");
    } else {
      // ...
      String rootDirectory = "M:\\Development\\workspace\\lockbusiness\\transaction-checker\\transactionchecker\\config\\properties";

      List<File> fileList = new ArrayList<>();

      fileList.add(new File(rootDirectory, "testnet\\config.secret.properties"));
      fileList.add(new File(rootDirectory, "testnet\\macos\\config.secret.properties"));
      fileList.add(new File(rootDirectory, "testnet\\windows\\config.secret.properties"));

      fileList.add(new File(rootDirectory, "mainnet\\config.secret.properties"));
      fileList.add(new File(rootDirectory, "mainnet\\macos\\config.secret.properties"));
      fileList.add(new File(rootDirectory, "mainnet\\windows\\config.secret.properties"));

      String password = optionalPasswordArgument.get().split("=")[1];

      for (File inputFile : fileList) {
        doFileEncryption(inputFile, password);
      }
    }
  }

  /**
   * 
   */
  private static void doFileEncryption(
      @Nonnull File textInputFile,
      @Nonnull String password) throws DfxException {
    LOGGER.trace("doFileEncryption() ...");

    EncryptionForSecrets encryption = new EncryptionForSecrets();

    LOGGER.debug("Original File: " + textInputFile);

    File encryptedFile = encryption.encrypt(textInputFile, password);
    LOGGER.debug("Encrypted File: " + encryptedFile.getAbsolutePath());

    doFileDecryption(encryptedFile, password);
  }

  /**
   * 
   */
  private static void doFileDecryption(
      @Nonnull File encryptedFile,
      @Nonnull String password) throws DfxException {
    LOGGER.trace("doFileDecryption() ...");

    EncryptionForSecrets encryption = new EncryptionForSecrets();

    Properties decryptedProperties = encryption.decrypt(encryptedFile, password);

    if (null == decryptedProperties) {
      LOGGER.error("File '" + encryptedFile + "' cannot be decrypted ...");
    } else {
      LOGGER.debug("Decrypted: " + decryptedProperties);
    }
  }
}
