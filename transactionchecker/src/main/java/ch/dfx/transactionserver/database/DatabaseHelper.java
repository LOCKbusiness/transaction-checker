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
import ch.dfx.transactionserver.data.LiquidityDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingDTO;

/**
 * 
 */
public class DatabaseHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseHelper.class);

  private PreparedStatement addressByNumberSelectStatement = null;
  private PreparedStatement addressByAddressSelectStatement = null;

  private PreparedStatement liquiditySelectStatement = null;
  private PreparedStatement liquidityByAddressNumberSelectStatement = null;

  private PreparedStatement depositSelectStatement = null;
  private PreparedStatement depositByLiquidityAddressNumberSelectStatement = null;

  private PreparedStatement balanceSelectStatement = null;
  private PreparedStatement balanceByAddressNumberSelectStatement = null;

  private PreparedStatement stakingSelectStatement = null;
  private PreparedStatement stakingByLiquidityAddressNumberSelectStatement = null;
  private PreparedStatement stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement = null;
  private PreparedStatement stakingByDepositAddressNumberSelectStatement = null;
  private PreparedStatement stakingByCustomerAddressNumberSelectStatement = null;

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
    LOGGER.trace("openStatements() ...");

    try {
      // Address ...
      String addressByNumberSelectSql = "SELECT * FROM public.address WHERE number=?";
      addressByNumberSelectStatement = connection.prepareStatement(addressByNumberSelectSql);

      String addressByAddressSelectSql = "SELECT * FROM public.address WHERE address=?";
      addressByAddressSelectStatement = connection.prepareStatement(addressByAddressSelectSql);

      // Liquidity ...
      String liquiditySelectSql =
          "SELECT"
              + " l.*,"
              + " a.address"
              + " FROM"
              + " public.liquidity l"
              + " JOIN public.address a ON"
              + " l.address_number = a.number";
      liquiditySelectStatement = connection.prepareStatement(liquiditySelectSql);

      String liquidityByAddressNumberSelectSql =
          liquiditySelectSql
              + " WHERE l.address_number=?";
      liquidityByAddressNumberSelectStatement = connection.prepareStatement(liquidityByAddressNumberSelectSql);

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
    LOGGER.trace("closeStatements() ...");

    try {
      addressByNumberSelectStatement.close();
      addressByAddressSelectStatement.close();

      liquiditySelectStatement.close();
      liquidityByAddressNumberSelectStatement.close();

      depositSelectStatement.close();
      depositByLiquidityAddressNumberSelectStatement.close();

      balanceSelectStatement.close();
      balanceByAddressNumberSelectStatement.close();

      stakingSelectStatement.close();
      stakingByLiquidityAddressNumberSelectStatement.close();
      stakingByLiquidityAddressNumberAndDepositAddressNumberSelectStatement.close();
      stakingByDepositAddressNumberSelectStatement.close();
      stakingByCustomerAddressNumberSelectStatement.close();

      masternodeSelectStatement.close();
      masternodeWhitelistByOwnerAddressSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public @Nullable AddressDTO getAddressDTOByNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getAddressDTOByNumber() ...");

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
    LOGGER.trace("getAddressDTOByAddress() ...");

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
    LOGGER.trace("getAddressDTO() ...");

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
  public @Nonnull List<LiquidityDTO> getLiquidityDTOList() throws DfxException {
    LOGGER.trace("getLiquidityDTOList() ...");

    try {
      List<LiquidityDTO> liquidityDTOList = new ArrayList<>();

      ResultSet resultSet = liquiditySelectStatement.executeQuery();

      while (resultSet.next()) {
        liquidityDTOList.add(createLiquidityDTO(resultSet));
      }
      return liquidityDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getLiquidityDTOList", e);
    }
  }

  /**
   * 
   */
  public @Nonnull LiquidityDTO getLiquidityDTOByAddressNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getLiquidityDTOByAddressNumber() ...");

    try {
      LiquidityDTO liquidityDTO = null;

      liquidityByAddressNumberSelectStatement.setInt(1, addressNumber);

      ResultSet resultSet = liquidityByAddressNumberSelectStatement.executeQuery();

      if (resultSet.next()) {
        liquidityDTO = createLiquidityDTO(resultSet);
      }

      if (null == liquidityDTO) {
        throw new DfxException("Liquidity not found by address " + addressNumber);
      }

      resultSet.close();

      return liquidityDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getLiquidityDTOByAddressNumber", e);
    }
  }

  /**
   * 
   */
  private LiquidityDTO createLiquidityDTO(@Nonnull ResultSet resultSet) throws DfxException {
    LOGGER.trace("createLiquidityDTO() ...");

    try {
      LiquidityDTO liquidityDTO = new LiquidityDTO();

      liquidityDTO.setAddressNumber(resultSet.getInt("address_number"));
      liquidityDTO.setAddress(resultSet.getString("address"));
      liquidityDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
      liquidityDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

      liquidityDTO.keepInternalState();

      return liquidityDTO;
    } catch (Exception e) {
      throw new DfxException("createLiquidityDTO", e);
    }
  }

  /**
   * 
   */
  public @Nonnull List<DepositDTO> getDepositDTOList() throws DfxException {
    LOGGER.trace("getDepositDTOList() ...");

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
    LOGGER.trace("getDepositDTOListByLiquidityAddressNumber() ...");

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
    LOGGER.trace("getDepositDTOList() ...");

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
    LOGGER.trace("getBalanceDTOList() ...");

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
    LOGGER.trace("getBalanceDTOByAddressNumber() ...");

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
    LOGGER.trace("createBalanceDTO() ...");

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
    LOGGER.trace("getStakingDTOList() ...");

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
    LOGGER.trace("getStakingDTOListByLiquidityAdressNumber() ...");

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
    LOGGER.trace("getStakingDTOListByLiquidityAdressNumberAndDepositAddressNumber() ...");

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
    LOGGER.trace("getStakingDTOListByDepositAddressNumber() ...");

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
    LOGGER.trace("getStakingDTOListByCustomerAddressNumber() ...");

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
    LOGGER.trace("getStakingDTOList() ...");

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
  public @Nonnull List<MasternodeWhitelistDTO> getMasternodeWhitelistDTOList() throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOList() ...");

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
    LOGGER.trace("getMasternodeWhitelistDTOByOwnerAddress() ...");

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
    LOGGER.trace("createMasternodeWhitelistDTO() ...");

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
