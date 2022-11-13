package ch.dfx.transactionserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.data.StakingWithdrawalReservedDTO;
import ch.dfx.transactionserver.data.TransactionDTO;

/**
 * 
 */
public class DatabaseHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseHelper.class);

  private PreparedStatement transactionByIdSelectStatement = null;

  private PreparedStatement addressByNumberSelectStatement = null;
  private PreparedStatement addressByAddressSelectStatement = null;

  private PreparedStatement stakingAddressSelectStatement = null;
  private PreparedStatement stakingAddressByLiquidityAddressNumberSelectStatement = null;

  private PreparedStatement depositSelectStatement = null;
  private PreparedStatement depositByLiquidityAddressNumberSelectStatement = null;

  private PreparedStatement balanceSelectStatement = null;
  private PreparedStatement balanceByAddressNumberSelectStatement = null;

  private PreparedStatement stakingSelectStatement = null;
  private PreparedStatement stakingByLiquidityAddressNumberSelectStatement = null;
  private PreparedStatement stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement = null;
  private PreparedStatement stakingByDepositAddressNumberSelectStatement = null;
  private PreparedStatement stakingByCustomerAddressNumberSelectStatement = null;

  private PreparedStatement stakingWithdrawalReservedSelectStatement = null;

  private PreparedStatement masternodeSelectStatement = null;
  private PreparedStatement masternodeWhitelistByOwnerAddressSelectStatement = null;

  /**
   * 
   */
  public DatabaseHelper() {
  }

  /**
   * 
   */
  public void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Transaction ...
      String transactionByIdSelectSql = "SELECT * FROM public.transaction WHERE txid=?";
      transactionByIdSelectStatement = connection.prepareStatement(transactionByIdSelectSql);

      // Address ...
      String addressByNumberSelectSql = "SELECT * FROM public.address WHERE number=?";
      addressByNumberSelectStatement = connection.prepareStatement(addressByNumberSelectSql);

      String addressByAddressSelectSql = "SELECT * FROM public.address WHERE address=?";
      addressByAddressSelectStatement = connection.prepareStatement(addressByAddressSelectSql);

      // Liquidity ...
      String stakingAddressSelectSql =
          "SELECT"
              + " s.*,"
              + " a1.address AS liquidity_address,"
              + " a2.address AS reward_address,"
              + " FROM"
              + " public.staking_address s"
              + " JOIN public.address a1 ON"
              + " s.liquidity_address_number = a1.number"
              + " LEFT JOIN public.address a2 ON"
              + " s.reward_address_number = a2.number";
      stakingAddressSelectStatement = connection.prepareStatement(stakingAddressSelectSql);

      String stakingAddressByLiquidityAddressNumberSelectSql =
          stakingAddressSelectSql
              + " WHERE s.liquidity_address_number=? AND s.reward_address_number=-1";
      stakingAddressByLiquidityAddressNumberSelectStatement = connection.prepareStatement(stakingAddressByLiquidityAddressNumberSelectSql);

      // Deposit ...
      String depositSelectSql =
          "SELECT"
              + " d.*,"
              + " a1.address AS liquidity_address,"
              + " a2.address AS deposit_address,"
              + " a3.address AS customer_address"
              + " FROM public.deposit d"
              + " JOIN public.address a1 ON"
              + " d.liquidity_address_number = a1.number"
              + " JOIN public.address a2 ON"
              + " d.deposit_address_number = a2.number"
              + " JOIN public.address a3 ON"
              + " d.customer_address_number = a3.number";
      depositSelectStatement = connection.prepareStatement(depositSelectSql);

      String depositByLiquidityAddressNumberSelectSql =
          depositSelectSql
              + " WHERE d.liquidity_address_number=?";
      depositByLiquidityAddressNumberSelectStatement = connection.prepareStatement(depositByLiquidityAddressNumberSelectSql);

      // Balance ...
      String balanceSelectSql =
          "SELECT"
              + " b.*,"
              + " a.address"
              + " FROM"
              + " public.balance b"
              + " JOIN public.address a ON"
              + " b.address_number = a.number";
      balanceSelectStatement = connection.prepareStatement(balanceSelectSql);

      String balanceByAddressNumberSelectSql =
          balanceSelectSql
              + " WHERE b.address_number=?";
      balanceByAddressNumberSelectStatement = connection.prepareStatement(balanceByAddressNumberSelectSql);

      // Staking ...
      String stakingSelectSql =
          "SELECT"
              + " s.*,"
              + " a1.address AS liquidity_address,"
              + " a2.address AS deposit_address,"
              + " a3.address AS customer_address"
              + " FROM public.staking s"
              + " JOIN public.address a1 ON"
              + " s.liquidity_address_number = a1.number"
              + " JOIN public.address a2 ON"
              + " s.deposit_address_number = a2.number"
              + " JOIN public.address a3 ON"
              + " s.customer_address_number = a3.number";
      stakingSelectStatement = connection.prepareStatement(stakingSelectSql);

      String stakingByLiquidityAddressNumberSelectSql =
          stakingSelectSql
              + " WHERE s.liquidity_address_number=?";
      stakingByLiquidityAddressNumberSelectStatement = connection.prepareStatement(stakingByLiquidityAddressNumberSelectSql);

      String stakingByLiquidityAddressNumberAndDepositAddressNumberSelectSql =
          stakingSelectSql
              + " WHERE s.liquidity_address_number=?"
              + " AND s.deposit_address_number=?";
      stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement =
          connection.prepareStatement(stakingByLiquidityAddressNumberAndDepositAddressNumberSelectSql);

      String stakingByDepositAddressNumberSelectSql =
          stakingSelectSql
              + " WHERE s.deposit_address_number=?";
      stakingByDepositAddressNumberSelectStatement = connection.prepareStatement(stakingByDepositAddressNumberSelectSql);

      String stakingByCustomerAddressNumberSelectSql =
          stakingSelectSql
              + " WHERE s.customer_address_number=?";
      stakingByCustomerAddressNumberSelectStatement = connection.prepareStatement(stakingByCustomerAddressNumberSelectSql);

      // Staking Withdrawal Reserved ...
      String stakingWithdrawalReservedSelectSql = "SELECT * FROM public.staking_withdrawal_reserved";
      stakingWithdrawalReservedSelectStatement = connection.prepareStatement(stakingWithdrawalReservedSelectSql);

      // Masternode ...
      String masternodeSelectSql = "SELECT * FROM public.masternode_whitelist";
      masternodeSelectStatement = connection.prepareStatement(masternodeSelectSql);

      String masternodeWhitelistByOwnerAddressSelectSql =
          masternodeSelectSql
              + " WHERE owner_address=?";
      masternodeWhitelistByOwnerAddressSelectStatement = connection.prepareStatement(masternodeWhitelistByOwnerAddressSelectSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  public void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements()");

    try {
      transactionByIdSelectStatement.close();

      addressByNumberSelectStatement.close();
      addressByAddressSelectStatement.close();

      stakingAddressSelectStatement.close();
      stakingAddressByLiquidityAddressNumberSelectStatement.close();

      depositSelectStatement.close();
      depositByLiquidityAddressNumberSelectStatement.close();

      balanceSelectStatement.close();
      balanceByAddressNumberSelectStatement.close();

      stakingSelectStatement.close();
      stakingByLiquidityAddressNumberSelectStatement.close();
      stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement.close();
      stakingByDepositAddressNumberSelectStatement.close();
      stakingByCustomerAddressNumberSelectStatement.close();

      stakingWithdrawalReservedSelectStatement.close();

      masternodeSelectStatement.close();
      masternodeWhitelistByOwnerAddressSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public @Nullable TransactionDTO getTransactionDTOById(@Nonnull String transactionId) throws DfxException {
    LOGGER.trace("getTransactionDTOById()");

    try {
      TransactionDTO transactionDTO = null;

      transactionByIdSelectStatement.setString(1, transactionId);

      ResultSet resultSet = transactionByIdSelectStatement.executeQuery();

      if (resultSet.next()) {
        transactionDTO = new TransactionDTO(
            resultSet.getInt("block_number"),
            resultSet.getInt("number"),
            resultSet.getString("txid"));

        transactionDTO.keepInternalState();
      }

      resultSet.close();

      return transactionDTO;
    } catch (Exception e) {
      throw new DfxException("getTransactionDTOById", e);
    }

  }

  /**
   * 
   */
  public @Nullable AddressDTO getAddressDTOByNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getAddressDTOByNumber()");

    try {
      addressByNumberSelectStatement.setInt(1, addressNumber);

      return getAddressDTO(addressByNumberSelectStatement);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressDTOByNumber", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressDTO getAddressDTOByAddress(@Nonnull String address) throws DfxException {
    LOGGER.trace("getAddressDTOByAddress()");

    try {
      addressByAddressSelectStatement.setString(1, address);

      return getAddressDTO(addressByAddressSelectStatement);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressDTOByAddress", e);
    }
  }

  /**
   * 
   */
  private @Nullable AddressDTO getAddressDTO(@Nonnull PreparedStatement statement) throws DfxException {
    LOGGER.trace("getAddressDTO()");

    try {
      AddressDTO addressDTO = null;

      ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        addressDTO = new AddressDTO(
            resultSet.getInt("number"),
            resultSet.getString("address"),
            resultSet.getString("hex"));

        addressDTO.keepInternalState();
      }

      resultSet.close();

      return addressDTO;
    } catch (Exception e) {
      throw new DfxException("getAddressDTO", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingAddressDTO> getStakingAddressDTOList() throws DfxException {
    LOGGER.trace("getStakingAddressList()");

    try {
      List<StakingAddressDTO> stakingAddressDTOList = new ArrayList<>();

      ResultSet resultSet = stakingAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        stakingAddressDTOList.add(createStakingAddressDTO(resultSet));
      }

      resultSet.close();

      return stakingAddressDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getStakingAddressList", e);
    }
  }

  /**
   * 
   */
  public @Nullable StakingAddressDTO getStakingAddressDTOByLiquidityAddressNumber(int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("getStakingAddressDTOByLiquidityAddressNumber()");

    try {
      StakingAddressDTO stakingAddressDTO = null;

      stakingAddressByLiquidityAddressNumberSelectStatement.setInt(1, liquidityAddressNumber);

      ResultSet resultSet = stakingAddressByLiquidityAddressNumberSelectStatement.executeQuery();

      if (resultSet.next()) {
        stakingAddressDTO = createStakingAddressDTO(resultSet);
      }

      resultSet.close();

      return stakingAddressDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getStakingAddressDTOByLiquidityAddressNumber", e);
    }
  }

  /**
   * 
   */
  private StakingAddressDTO createStakingAddressDTO(@Nonnull ResultSet resultSet) throws DfxException {
    LOGGER.trace("createStakingAddressDTO()");

    try {
      StakingAddressDTO stakingAddressDTO = new StakingAddressDTO();

      stakingAddressDTO.setLiquidityAddressNumber(resultSet.getInt("liquidity_address_number"));
      stakingAddressDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));

      stakingAddressDTO.setRewardAddressNumber(resultSet.getInt("reward_address_number"));
      stakingAddressDTO.setRewardAddress(resultSet.getString("reward_address"));

      stakingAddressDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
      stakingAddressDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

      stakingAddressDTO.keepInternalState();

      return stakingAddressDTO;
    } catch (Exception e) {
      throw new DfxException("createStakingAddressDTO", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<DepositDTO> getDepositDTOList() throws DfxException {
    LOGGER.trace("getDepositDTOList()");

    try {
      return getDepositDTOList(depositSelectStatement);
    } catch (Exception e) {
      throw new DfxException("getDepositDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<DepositDTO> getDepositDTOListByLiquidityAddressNumber(int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("getDepositDTOListByLiquidityAddressNumber()");

    try {
      depositByLiquidityAddressNumberSelectStatement.setInt(1, liquidityAddressNumber);

      return getDepositDTOList(depositByLiquidityAddressNumberSelectStatement);
    } catch (Exception e) {
      throw new DfxException("getDepositDTOListByLiquidityAddressNumber", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<DepositDTO> getDepositDTOList(@Nonnull PreparedStatement statement) throws DfxException {
    LOGGER.trace("getDepositDTOList()");

    try {
      List<DepositDTO> depositDTOList = new ArrayList<>();

      ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        DepositDTO depositDTO = new DepositDTO();

        depositDTO.setLiquidityAddressNumber(resultSet.getInt("liquidity_address_number"));
        depositDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));
        depositDTO.setDepositAddressNumber(resultSet.getInt("deposit_address_number"));
        depositDTO.setDepositAddress(resultSet.getString("deposit_address"));
        depositDTO.setCustomerAddressNumber(resultSet.getInt("customer_address_number"));
        depositDTO.setCustomerAddress(resultSet.getString("customer_address"));

        depositDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        depositDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        depositDTO.keepInternalState();

        depositDTOList.add(depositDTO);
      }

      resultSet.close();

      return depositDTOList;
    } catch (Exception e) {
      throw new DfxException("getDepositDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<BalanceDTO> getBalanceDTOList() throws DfxException {
    LOGGER.trace("getBalanceDTOList()");

    try {
      List<BalanceDTO> balanceDTOList = new ArrayList<>();

      ResultSet resultSet = balanceSelectStatement.executeQuery();

      while (resultSet.next()) {
        balanceDTOList.add(createBalanceDTO(resultSet));
      }

      resultSet.close();

      return balanceDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getBalanceDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nullable BalanceDTO getBalanceDTOByAddressNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getBalanceDTOByAddressNumber()");

    try {
      balanceByAddressNumberSelectStatement.setInt(1, addressNumber);

      BalanceDTO balanceDTO = null;

      ResultSet resultSet = balanceByAddressNumberSelectStatement.executeQuery();

      if (resultSet.next()) {
        balanceDTO = createBalanceDTO(resultSet);
      }

      resultSet.close();

      return balanceDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getBalanceDTOByAddressNumber", e);
    }
  }

  /**
   * 
   */
  private BalanceDTO createBalanceDTO(@Nonnull ResultSet resultSet) throws DfxException {
    LOGGER.trace("createBalanceDTO()");

    try {
      BalanceDTO balanceDTO = new BalanceDTO(resultSet.getInt("address_number"));

      balanceDTO.setBlockNumber(resultSet.getInt("block_number"));
      balanceDTO.setTransactionCount(resultSet.getInt("transaction_count"));

      balanceDTO.setVout(resultSet.getBigDecimal("vout"));
      balanceDTO.setVin(resultSet.getBigDecimal("vin"));

      balanceDTO.keepInternalState();

      return balanceDTO;
    } catch (Exception e) {
      throw new DfxException("createBalanceDTO", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingDTO> getStakingDTOList() throws DfxException {
    LOGGER.trace("getStakingDTOList()");

    try {
      return getStakingDTOList(stakingSelectStatement);
    } catch (Exception e) {
      throw new DfxException("getStakingDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingDTO> getStakingDTOListByLiquidityAdressNumber(int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDTOListByLiquidityAdressNumber()");

    try {
      stakingByLiquidityAddressNumberSelectStatement.setInt(1, liquidityAddressNumber);

      return getStakingDTOList(stakingByLiquidityAddressNumberSelectStatement);
    } catch (Exception e) {
      throw new DfxException("getStakingDTOListByLiquidityAdressNumber", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingDTO> getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber(
      int liquidityAddressNumber,
      int depositAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber()");

    try {
      stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement.setInt(1, liquidityAddressNumber);
      stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement.setInt(2, depositAddressNumber);

      return getStakingDTOList(stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement);
    } catch (Exception e) {
      throw new DfxException("getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingDTO> getStakingDTOListByDepositAddressNumber(int depositAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDTOListByDepositAddressNumber()");

    try {
      stakingByDepositAddressNumberSelectStatement.setInt(1, depositAddressNumber);

      return getStakingDTOList(stakingByDepositAddressNumberSelectStatement);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getStakingDTOListByDepositAddressNumber", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingDTO> getStakingDTOListByCustomerAddressNumber(int customerAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDTOListByCustomerAddressNumber()");

    try {
      stakingByCustomerAddressNumberSelectStatement.setInt(1, customerAddressNumber);

      return getStakingDTOList(stakingByCustomerAddressNumberSelectStatement);
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getStakingDTOListByCustomerAddressNumber", e);
    }
  }

  /**
   * 
   */
  private @Nonnull List<StakingDTO> getStakingDTOList(@Nonnull PreparedStatement statement) throws DfxException {
    LOGGER.trace("getStakingDTOList()");

    try {
      List<StakingDTO> stakingDTOList = new ArrayList<>();

      ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        StakingDTO stakingDTO = new StakingDTO(
            resultSet.getInt("liquidity_address_number"),
            resultSet.getInt("deposit_address_number"),
            resultSet.getInt("customer_address_number"));

        stakingDTO.setLiquidityAddress(resultSet.getString("liquidity_address"));
        stakingDTO.setDepositAddress(resultSet.getString("deposit_address"));
        stakingDTO.setCustomerAddress(resultSet.getString("customer_address"));

        stakingDTO.setLastInBlockNumber(resultSet.getInt("last_in_block_number"));
        stakingDTO.setVin(resultSet.getBigDecimal("vin"));
        stakingDTO.setLastOutBlockNumber(resultSet.getInt("last_out_block_number"));
        stakingDTO.setVout(resultSet.getBigDecimal("vout"));

        stakingDTO.keepInternalState();

        stakingDTOList.add(stakingDTO);
      }

      resultSet.close();

      return stakingDTOList;
    } catch (Exception e) {
      throw new DfxException("getStakingDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<StakingWithdrawalReservedDTO> getStakingWithdrawalReservedDTOList() throws DfxException {
    LOGGER.trace("getStakingWithdrawalReservedDTOList()");

    try {
      List<StakingWithdrawalReservedDTO> stakingWithdrawalReservedDTOList = new ArrayList<>();

      ResultSet resultSet = stakingWithdrawalReservedSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingWithdrawalReservedDTO stakingWithdrawalReservedDTO = new StakingWithdrawalReservedDTO();

        stakingWithdrawalReservedDTO.setWithdrawalId(resultSet.getInt("withdrawal_id"));
        stakingWithdrawalReservedDTO.setTransactionId(resultSet.getString("transaction_id"));
        stakingWithdrawalReservedDTO.setCustomerAddress(resultSet.getString("customer_address"));
        stakingWithdrawalReservedDTO.setVout(resultSet.getBigDecimal("vout"));
        stakingWithdrawalReservedDTO.setCreateTime(resultSet.getTimestamp("create_time"));

        stakingWithdrawalReservedDTOList.add(stakingWithdrawalReservedDTO);
      }

      resultSet.close();

      return stakingWithdrawalReservedDTOList;
    } catch (Exception e) {
      throw new DfxException("getStakingWithdrawalReservedDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<MasternodeWhitelistDTO> getMasternodeWhitelistDTOList() throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOList()");

    try {
      List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = new ArrayList<>();

      ResultSet resultSet = masternodeSelectStatement.executeQuery();

      while (resultSet.next()) {
        masternodeWhitelistDTOList.add(createMasternodeWhitelistDTO(resultSet));
      }

      resultSet.close();

      return masternodeWhitelistDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getMasternodeWhitelistDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nullable MasternodeWhitelistDTO getMasternodeWhitelistDTOByOwnerAddress(@Nonnull String ownerAddress) throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOByOwnerAddress()");

    try {
      MasternodeWhitelistDTO masternodeWhitelistDTO = null;

      masternodeWhitelistByOwnerAddressSelectStatement.setString(1, ownerAddress);

      ResultSet resultSet = masternodeWhitelistByOwnerAddressSelectStatement.executeQuery();

      if (resultSet.next()) {
        masternodeWhitelistDTO = createMasternodeWhitelistDTO(resultSet);
      }

      resultSet.close();

      return masternodeWhitelistDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getMasternodeWhitelistDTOByOwnerAddress", e);
    }
  }

  /**
   * 
   */
  private MasternodeWhitelistDTO createMasternodeWhitelistDTO(@Nonnull ResultSet resultSet) throws DfxException {
    LOGGER.trace("createMasternodeWhitelistDTO()");

    try {
      MasternodeWhitelistDTO masternodeWhitelistDTO =
          new MasternodeWhitelistDTO(
              resultSet.getInt("wallet_id"),
              resultSet.getInt("idx"),
              resultSet.getString("owner_address"));

      masternodeWhitelistDTO.setTransactionId(resultSet.getString("txid"));
      masternodeWhitelistDTO.setOperatorAddress(resultSet.getString("operator_address"));
      masternodeWhitelistDTO.setRewardAddress(resultSet.getString("reward_address"));
      masternodeWhitelistDTO.setCreationBlockNumber(resultSet.getInt("creation_block_number"));
      masternodeWhitelistDTO.setResignBlockNumber(resultSet.getInt("resign_block_number"));
      masternodeWhitelistDTO.setState(resultSet.getString("state"));

      masternodeWhitelistDTO.keepInternalState();

      return masternodeWhitelistDTO;
    } catch (Exception e) {
      throw new DfxException("createMasternodeWhitelistDTO", e);
    }
  }
}
