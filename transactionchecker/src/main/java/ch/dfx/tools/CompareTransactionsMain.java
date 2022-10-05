package ch.dfx.tools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.PayoutManagerUtils;

/**
 * ============================================================================
 * Small Tool to compare the OCEAN Transactions with the Transactions detected
 * via the Defichain (Full Node)...
 * ============================================================================
 */
public class CompareTransactionsMain {
  private static final Logger LOGGER = LogManager.getLogger(CompareTransactionsMain.class);

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  /**
   * 
   */
  public static void main(String[] args) throws Exception {
    String environment = PayoutManagerUtils.getEnvironment().name().toLowerCase();

    // ...
    System.setProperty("logFilename", "comparetransaction-" + environment);
    PayoutManagerUtils.initLog4j("log4j2-payoutmanager.xml");

    // ...
    String checkPath = "logs";

    File oceanCheckFile = new File(checkPath, "ocean-transactions.txt");
    File defichainCheckFile = new File(checkPath, "defichain-transactions.txt");

    LOGGER.debug("OCEAN     Check File: " + oceanCheckFile.getAbsolutePath());
    LOGGER.debug("DEFICHAIN Check File: " + defichainCheckFile.getAbsolutePath());
    LOGGER.debug("");

    BasicFileAttributes oceanFileAttributes = Files.readAttributes(oceanCheckFile.toPath(), BasicFileAttributes.class);
    BasicFileAttributes defichainFileAttributes = Files.readAttributes(defichainCheckFile.toPath(), BasicFileAttributes.class);

    LOGGER.debug("OCEAN     Last Modified Time: " + convert(oceanFileAttributes.lastModifiedTime()));
    LOGGER.debug("DEFICHAIN Last Modified Time: " + convert(defichainFileAttributes.lastModifiedTime()));
    LOGGER.debug("");

    List<String> oceanCheckList = Files.readAllLines(oceanCheckFile.toPath());
    List<String> defichainCheckList = Files.readAllLines(defichainCheckFile.toPath());

    int oceanCheckListSize = oceanCheckList.size();
    int defichainCheckListSize = defichainCheckList.size();

    LOGGER.debug("OCEAN     Checklist Size: " + oceanCheckListSize);
    LOGGER.debug("DEFICHAIN Checklist Size: " + defichainCheckListSize);
    LOGGER.debug("");

    if (oceanCheckListSize != defichainCheckListSize) {
      LOGGER.error("ERROR: Checklist Size different ...");
    }

    for (int i = 0; i < defichainCheckList.size(); i++) {
      String oceanCheck = oceanCheckList.get(i);
      String defichainCheck = defichainCheckList.get(i);

      if (!oceanCheckList.contains(defichainCheck)) {
        LOGGER.error("ERROR: " + oceanCheck);
        LOGGER.error("ERROR: " + defichainCheck);
      }
    }
  }

  /**
   * 
   */
  private static String convert(FileTime fileTime) {
    LocalDateTime localDateTime =
        fileTime
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    return localDateTime.format(DATE_FORMATTER);
  }
}
