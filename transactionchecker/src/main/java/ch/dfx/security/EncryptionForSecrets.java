package ch.dfx.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AlgorithmParameters;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class EncryptionForSecrets {
  private static final Logger LOGGER = LogManager.getLogger(EncryptionForSecrets.class);

  private static final byte[] SALT = new String("Welcome to the real Crypto-World").getBytes();
  private static final int ITERATION_COUNT = 10000;
  private static final int KEY_LENGTH = 128;

  private static final int INTEGER_LENGTH = 4;

  /**
   * 
   */
  public SecretKeySpec createSecretKey(@Nonnull String password) throws DfxException {
    LOGGER.trace("createSecretKey()");
    Objects.requireNonNull(password, "null 'password' not allowed ...");

    try {
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

      PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH);
      SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

      return new SecretKeySpec(secretKey.getEncoded(), "AES");
    } catch (Exception e) {
      throw new DfxException("createSecretKey", e);
    }
  }

  /**
   * 
   */
  public File encrypt(
      @Nonnull File textInputFile,
      @Nonnull String password) throws DfxException {
    LOGGER.trace("encrypt()");
    Objects.requireNonNull(textInputFile, "null 'textInputFile' not allowed ...");
    Objects.requireNonNull(password, "null 'password' not allowed ...");

    try {
      SecretKeySpec secretKey = createSecretKey(password);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);

      AlgorithmParameters algorithmParameters = cipher.getParameters();
      IvParameterSpec ivParameterSpec = algorithmParameters.getParameterSpec(IvParameterSpec.class);

      return writeEncryptedFile(textInputFile, ivParameterSpec, cipher);
    } catch (Exception e) {
      throw new DfxException("encrypt", e);
    }
  }

  /**
   * 
   */
  private File writeEncryptedFile(
      @Nonnull File textInputFile,
      @Nonnull IvParameterSpec ivParameterSpec,
      @Nonnull Cipher cipher) throws DfxException {
    LOGGER.trace("writeEncryptedFile()");

    String absolutFilePath = textInputFile.getAbsolutePath();
    String fullPath = FilenameUtils.getFullPath(absolutFilePath);
    String baseName = FilenameUtils.getBaseName(absolutFilePath);
    String extension = FilenameUtils.getExtension(absolutFilePath);
    File encryptedOutputFile = new File(fullPath, baseName + ".enc");

    try (FileOutputStream fileOut = new FileOutputStream(encryptedOutputFile);
        CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
      // Write File Extension ...
      byte[] extensionAsBytes = extension.getBytes();
      fileOut.write(ByteBuffer.allocate(INTEGER_LENGTH).putInt(extensionAsBytes.length).array());
      fileOut.write(extensionAsBytes);

      // Write IV ...
      byte[] iv = ivParameterSpec.getIV();
      fileOut.write(ByteBuffer.allocate(INTEGER_LENGTH).putInt(iv.length).array());
      fileOut.write(iv);

      // Write Content ...
      byte[] fileAsByteArray = Files.readAllBytes(textInputFile.toPath());
      cipherOut.write(fileAsByteArray);
    } catch (Exception e) {
      throw new DfxException("writeEncryptedFile", e);
    }

    return encryptedOutputFile;
  }

  /**
   * 
   */
  public @Nullable String decrypt(
      @Nonnull File encryptedFile,
      @Nonnull String password) throws DfxException {
    LOGGER.trace("decrypt()");
    Objects.requireNonNull(encryptedFile, "null 'encryptedFile' not allowed ...");
    Objects.requireNonNull(password, "null 'password' not allowed ...");

    try {
      String result = null;

      SecretKeySpec secretKey = createSecretKey(password);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

      try (FileInputStream fileIn = new FileInputStream(encryptedFile)) {
        // Read File Extension ...
        byte[] extensionLengthBuffer = new byte[INTEGER_LENGTH];
        fileIn.read(extensionLengthBuffer);

        int extensionLength = ByteBuffer.wrap(extensionLengthBuffer).getInt();
        byte[] extensionBuffer = new byte[extensionLength];
        fileIn.read(extensionBuffer);

        // Read IV ...
        byte[] ivLengthBuffer = new byte[INTEGER_LENGTH];
        fileIn.read(ivLengthBuffer);

        int ivLength = ByteBuffer.wrap(ivLengthBuffer).getInt();
        byte[] ivBuffer = new byte[ivLength];
        fileIn.read(ivBuffer);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBuffer));

        // Read Content ...
        CipherInputStream cipherInputStream = new CipherInputStream(fileIn, cipher);
        result = new String(cipherInputStream.readAllBytes(), StandardCharsets.UTF_8);
        cipherInputStream.close();
      }

      return result;
    } catch (Exception e) {
      throw new DfxException("decrypt", e);
    }
  }
}
