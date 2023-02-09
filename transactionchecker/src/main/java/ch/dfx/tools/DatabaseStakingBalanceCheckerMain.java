package ch.dfx.tools;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.EnvironmentEnum;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.H2DBManagerImpl;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class DatabaseStakingBalanceCheckerMain {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseStakingBalanceCheckerMain.class);

  private static final String IDENTIFIER = "balancechecker";

  /**
   *
   */
  public static void main(String[] args) {
    try {
      Class.forName("org.h2.Driver");

      // ...
      boolean isMainnet = Stream.of(args).anyMatch(a -> "--mainnet".equals(a));
      boolean isStagnet = Stream.of(args).anyMatch(a -> "--stagnet".equals(a));
      boolean isTestnet = Stream.of(args).anyMatch(a -> "--testnet".equals(a));

      // ...
      NetworkEnum network = TransactionCheckerUtils.getNetwork(isMainnet, isStagnet, isTestnet);
      EnvironmentEnum environment = TransactionCheckerUtils.getEnvironment();

      // ...
      System.setProperty("logFilename", TransactionCheckerUtils.getLog4jFilename(IDENTIFIER, network));
      TransactionCheckerUtils.initLog4j("log4j2.xml");

      // ...
      TransactionCheckerUtils.setupGlobalProvider(network, environment);

      // ...
      LOGGER.debug("=".repeat(80));
      LOGGER.debug("Network: " + network);
      LOGGER.debug("Environment: " + environment);

      // ...
      H2DBManager databaseManager = new H2DBManagerImpl();
      Connection connection = databaseManager.openConnection();

      // ...
      DatabaseBalanceHelper stakingBalanceHelper = new DatabaseBalanceHelper(network);
      stakingBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);

      DatabaseBalanceHelper yieldmachineBalanceHelper = new DatabaseBalanceHelper(network);
      yieldmachineBalanceHelper.openStatements(connection, TOKEN_YIELDMACHINE_SCHEMA);

      checkStakingBalance(stakingBalanceHelper);
      checkYieldmachineDFIBalance(yieldmachineBalanceHelper);
      checkYieldmachineDUSDBalance(yieldmachineBalanceHelper);

      stakingBalanceHelper.closeStatements();
      yieldmachineBalanceHelper.closeStatements();
      databaseManager.closeConnection(connection);
    } catch (Exception e) {
      LOGGER.error("Fatal Error", e);
      System.exit(-1);
    }
  }

  /**
   * 
   */
  private static void checkStakingBalance(@Nonnull DatabaseBalanceHelper stakingBalanceHelper) throws DfxException {
    LOGGER.debug("checkStakingBalance()");

    Map<String, LockCsvData> lockDataMap = readLockCsv();

    List<StakingDTO> stakingDTOList = stakingBalanceHelper.getStakingDTOList(TokenEnum.DFI);

    BigDecimal myTotalStakingBalance = BigDecimal.ZERO;
    BigDecimal lockTotalStakingBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : stakingDTOList) {
      String customerAddress = stakingDTO.getCustomerAddress();

      String myStakingDepositAddress = stakingDTO.getDepositAddress();

      if (null == myStakingDepositAddress) {
        myStakingDepositAddress = "";
      }

      BigDecimal myStakingBalance = stakingDTO.getVin().subtract(stakingDTO.getVout()).setScale(2, RoundingMode.HALF_UP);
      myTotalStakingBalance = myTotalStakingBalance.add(myStakingBalance);

      // ...
      LockCsvData lockCsvData = lockDataMap.get(customerAddress);

      if (null == lockCsvData) {
        if (0 != myStakingBalance.compareTo(BigDecimal.ZERO)) {
          LOGGER.error("No Lock Data found: customer=" + customerAddress + " / " + myStakingBalance);
        }
      } else {
        String lockStakingDepositAddress = lockCsvData.stakingDepositAddress;
        BigDecimal lockStakingBalance = lockCsvData.stakingBalance;

        lockTotalStakingBalance = lockTotalStakingBalance.add(lockStakingBalance);

        if (!myStakingDepositAddress.equals(lockStakingDepositAddress)) {
          LOGGER.error("Different deposit addresses: customer=" + customerAddress + " / " + myStakingDepositAddress + " / " + lockStakingDepositAddress);
        } else if (0 != myStakingBalance.compareTo(lockStakingBalance)) {
          LOGGER.error(
              "Different balance: customer=" + customerAddress + " / deposit=" + myStakingDepositAddress
                  + " / " + myStakingBalance + " / " + lockStakingBalance);
        }
      }
    }

    // ...
    LOGGER.debug("[DFI Staking Balance] My Balance / LOCK Balance: " + myTotalStakingBalance + " / " + lockTotalStakingBalance);
  }

  /**
   * 
   */
  private static void checkYieldmachineDFIBalance(@Nonnull DatabaseBalanceHelper yieldmachineBalanceHelper) throws DfxException {
    LOGGER.debug("checkYieldmachineDFIBalance()");

    Map<String, LockCsvData> lockDataMap = readLockCsv();

    List<StakingDTO> stakingDTOList = yieldmachineBalanceHelper.getStakingDTOList(TokenEnum.DFI);

    BigDecimal myTotalYieldmachineBalance = BigDecimal.ZERO;
    BigDecimal lockTotalYieldmachineBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : stakingDTOList) {
      String customerAddress = stakingDTO.getCustomerAddress();

      String myYieldmachineDepositAddress = stakingDTO.getDepositAddress();

      if (null == myYieldmachineDepositAddress) {
        myYieldmachineDepositAddress = "";
      }

      BigDecimal myStakingBalance = stakingDTO.getVin().subtract(stakingDTO.getVout()).setScale(2, RoundingMode.HALF_UP);
      myTotalYieldmachineBalance = myTotalYieldmachineBalance.add(myStakingBalance);

      // ...
      LockCsvData lockCsvData = lockDataMap.get(customerAddress);

      if (null == lockCsvData) {
        if (0 != myStakingBalance.compareTo(BigDecimal.ZERO)) {
          LOGGER.error("No Lock Data found: customer=" + customerAddress + " / " + myStakingBalance);
        }
      } else {
        String lockYieldmachineDepositAddress = lockCsvData.yieldmachineDepositAddress;
        BigDecimal lockYieldmachineBalance = lockCsvData.yieldmachineDFIBalance;

        lockTotalYieldmachineBalance = lockTotalYieldmachineBalance.add(lockYieldmachineBalance);

        if (!myYieldmachineDepositAddress.equals(lockYieldmachineDepositAddress)) {
          LOGGER.error(
              "Different deposit addresses: customer=" + customerAddress + " / " + myYieldmachineDepositAddress + " / " + lockYieldmachineDepositAddress);
        } else if (0 != myStakingBalance.compareTo(lockYieldmachineBalance)) {
          LOGGER.error(
              "Different balance: customer=" + customerAddress + " / deposit=" + myYieldmachineDepositAddress
                  + " / " + myStakingBalance + " / " + lockYieldmachineBalance);
        }
      }
    }

    // ...
    LOGGER.debug("[DFI Yieldmachine Balance] My Balance / LOCK Balance: " + myTotalYieldmachineBalance + " / " + lockTotalYieldmachineBalance);
  }

  /**
   * 
   */
  private static void checkYieldmachineDUSDBalance(@Nonnull DatabaseBalanceHelper yieldmachineBalanceHelper) throws DfxException {
    LOGGER.debug("checkYieldmachineDUSDBalance()");

    Map<String, LockCsvData> lockDataMap = readLockCsv();

    List<StakingDTO> stakingDTOList = yieldmachineBalanceHelper.getStakingDTOList(TokenEnum.DUSD);

    BigDecimal myTotalYieldmachineBalance = BigDecimal.ZERO;
    BigDecimal lockTotalYieldmachineBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : stakingDTOList) {
      String customerAddress = stakingDTO.getCustomerAddress();

      String myYieldmachineDepositAddress = stakingDTO.getDepositAddress();

      if (null == myYieldmachineDepositAddress) {
        myYieldmachineDepositAddress = "";
      }

      BigDecimal myStakingBalance = stakingDTO.getVin().subtract(stakingDTO.getVout()).setScale(2, RoundingMode.HALF_UP);
      myTotalYieldmachineBalance = myTotalYieldmachineBalance.add(myStakingBalance);

      // ...
      LockCsvData lockCsvData = lockDataMap.get(customerAddress);

      if (null == lockCsvData) {
        if (0 != myStakingBalance.compareTo(BigDecimal.ZERO)) {
          LOGGER.error("No Lock Data found: customer=" + customerAddress + " / " + myStakingBalance);
        }
      } else {
        String lockYieldmachineDepositAddress = lockCsvData.yieldmachineDepositAddress;
        BigDecimal lockYieldmachineBalance = lockCsvData.yieldmachineDUSDBalance;

        lockTotalYieldmachineBalance = lockTotalYieldmachineBalance.add(lockYieldmachineBalance);

        if (!myYieldmachineDepositAddress.equals(lockYieldmachineDepositAddress)) {
          LOGGER.error(
              "Different deposit addresses: customer=" + customerAddress + " / " + myYieldmachineDepositAddress + " / " + lockYieldmachineDepositAddress);
        } else if (0 != myStakingBalance.compareTo(lockYieldmachineBalance)) {
          LOGGER.error(
              "Different balance: customer=" + customerAddress + " / deposit=" + myYieldmachineDepositAddress
                  + " / " + myStakingBalance + " / " + lockYieldmachineBalance);
        }
      }
    }

    // ...
    LOGGER.debug("[DUSD Yieldmachine Balance] My Balance / LOCK Balance: " + myTotalYieldmachineBalance + " / " + lockTotalYieldmachineBalance);
  }

  /**
   * 
   */
  private static Map<String, LockCsvData> readLockCsv() throws DfxException {
    Map<String, LockCsvData> lockDataMap = new HashMap<>();

    File csvFile = new File("C:\\Users\\Bernd\\Downloads", "LOCK Balances pro User alle Assets - User_Balances.csv");

    int lineNumber = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(csvFile, StandardCharsets.UTF_8))) {

      String line = null;

      // Skip the first three lines ...
      reader.readLine();
      lineNumber++;
      reader.readLine();
      lineNumber++;
      reader.readLine();
      lineNumber++;

      // ...
      while (null != (line = reader.readLine())) {
        lineNumber++;

        String[] entries = line.split(",");

        LockCsvData lockCsvData = new LockCsvData();

        lockCsvData.ownerAddress = entries[0];
        lockCsvData.stakingDepositAddress = entries[1];
        lockCsvData.stakingBalance = new BigDecimal(convertToNumber(entries[2]));

        if (4 <= entries.length) {
          lockCsvData.yieldmachineDepositAddress = entries[3];
        }

        if (5 <= entries.length) {
          lockCsvData.yieldmachineDFIBalance = new BigDecimal(convertToNumber(entries[4]));
        }

        if (6 <= entries.length) {
          lockCsvData.yieldmachineDUSDBalance = new BigDecimal(convertToNumber(entries[5]));
        }

        lockDataMap.put(lockCsvData.ownerAddress, lockCsvData);
      }
    } catch (Exception e) {
      throw new DfxException("readLockCsv: lineNumber=" + lineNumber, e);
    }

    return lockDataMap;
  }

  /**
   * 
   */
  private static String convertToNumber(@Nonnull String text) {
    return text.replaceAll("[^0-9\\.]", "");
  }

  /**
   * 
   */
  private static class LockCsvData {
    private String ownerAddress = "";
    private String stakingDepositAddress = "";
    private BigDecimal stakingBalance = BigDecimal.ZERO;
    private String yieldmachineDepositAddress = "";
    private BigDecimal yieldmachineDFIBalance = BigDecimal.ZERO;
    private BigDecimal yieldmachineDUSDBalance = BigDecimal.ZERO;

    /**
     * 
     */
    @Override
    public String toString() {
      return TransactionCheckerUtils.toJson(this);
    }
  }
}
