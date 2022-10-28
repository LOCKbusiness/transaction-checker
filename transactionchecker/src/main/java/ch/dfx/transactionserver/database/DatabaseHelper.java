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

  private PreparedStatement stakingByLiquidityAddressNumberSelectStatement = null;
  private PreparedStatement stakingByDepositAddressNumberSelectStatement = null;
  private PreparedStatement stakingByCustomerAddressNumberSelectStatement = null;

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
      String addressByNumberSelectSql = "SELECT * FROM public.address WHERE number = ?";
      addressByNumberSelectStatement = connection.prepareStatement(addressByNumberSelectSql);

      String addressByAddressSelectSql = "SELECT * FROM public.address WHERE address = ?";
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
          "SELECT"
              + " l.*,"
              + " a.address"
              + " FROM"
              + " public.liquidity l"
              + " JOIN public.address a ON"
              + " l.address_number = a.number"
              + " WHERE l.address_number = ?";

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

      // Staking ...
      String stakingByLiquidityAddressNumberSelectSql =
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
              + " s.customer_address_number = a3.number"
              + " WHERE s.liquidity_address_number = ?";
      stakingByLiquidityAddressNumberSelectStatement = connection.prepareStatement(stakingByLiquidityAddressNumberSelectSql);

      String stakingByDepositAddressNumberSelectSql =
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
              + " s.customer_address_number = a3.number"
              + " WHERE s.deposit_address_number = ?";
      stakingByDepositAddressNumberSelectStatement = connection.prepareStatement(stakingByDepositAddressNumberSelectSql);

      String stakingByCustomerAddressNumberSelectSql =
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
              + " s.customer_address_number = a3.number"
              + " WHERE s.customer_address_number = ?";
      stakingByCustomerAddressNumberSelectStatement = connection.prepareStatement(stakingByCustomerAddressNumberSelectSql);

      // Masternode ...
      String masternodeWhitelistByOwnerAddressSelectSql = "SELECT * FROM public.masternode_whitelist WHERE owner_address=?";
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

      stakingByLiquidityAddressNumberSelectStatement.close();
      stakingByDepositAddressNumberSelectStatement.close();
      stakingByCustomerAddressNumberSelectStatement.close();

      masternodeWhitelistByOwnerAddressSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public @Nonnull AddressDTO getAddressDTOByNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getAddressDTOByNumber() ...");

    try {
      addressByNumberSelectStatement.setInt(1, addressNumber);

      AddressDTO addressDTO = getAddressDTO(addressByNumberSelectStatement);

      if (null == addressDTO) {
        throw new DfxException("Address not found by number " + addressNumber);
      }

      return addressDTO;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getAddressDTOByNumber", e);
    }
  }

  /**
   * 
   */
  public @Nonnull AddressDTO getAddressDTOByAddress(@Nonnull String address) throws DfxException {
    LOGGER.trace("getAddressDTOByAddress() ...");

    try {
      addressByAddressSelectStatement.setString(1, address);

      AddressDTO addressDTO = getAddressDTO(addressByAddressSelectStatement);

      if (null == addressDTO) {
        throw new DfxException("Address not found by address " + address);
      }

      return addressDTO;
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
        LiquidityDTO liquidityDTO = new LiquidityDTO();

        liquidityDTO.setAddressNumber(resultSet.getInt("address_number"));
        liquidityDTO.setAddress(resultSet.getString("address"));
        liquidityDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        liquidityDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        liquidityDTOList.add(liquidityDTO);
      }
      return liquidityDTOList;
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
        liquidityDTO = new LiquidityDTO();

        liquidityDTO.setAddressNumber(resultSet.getInt("address_number"));
        liquidityDTO.setAddress(resultSet.getString("address"));
        liquidityDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        liquidityDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));
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
  public @Nonnull List<DepositDTO> getDepositDTOList() throws DfxException {
    LOGGER.trace("getDepositDTOList() ...");

    try {
      List<DepositDTO> depositDTOList = new ArrayList<>();

      ResultSet resultSet = depositSelectStatement.executeQuery();

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
  public @Nullable MasternodeWhitelistDTO getMasternodeWhitelistDTOByOwnerAddress(@Nonnull String ownerAddress) throws DfxException {
    LOGGER.trace("getMasternodeWhitelistDTOByOwnerAddress() ...");

    try {
      MasternodeWhitelistDTO masternodeWhitelistDTO = null;

      masternodeWhitelistByOwnerAddressSelectStatement.setString(1, ownerAddress);

      ResultSet resultSet = masternodeWhitelistByOwnerAddressSelectStatement.executeQuery();

      if (resultSet.next()) {
        masternodeWhitelistDTO = new MasternodeWhitelistDTO();

        masternodeWhitelistDTO.setWalletId(resultSet.getInt("wallet_id"));
        masternodeWhitelistDTO.setIdx(resultSet.getInt("idx"));
        masternodeWhitelistDTO.setOwnerAddress(resultSet.getString("owner_address"));
      }

      resultSet.close();

      return masternodeWhitelistDTO;
    } catch (Exception e) {
      throw new DfxException("getMasternodeWhitelistDTOByOwnerAddress", e);
    }
  }
}
