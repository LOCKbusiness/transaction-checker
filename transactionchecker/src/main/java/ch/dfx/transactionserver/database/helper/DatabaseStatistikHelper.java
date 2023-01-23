package ch.dfx.transactionserver.database.helper;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.StatistikDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;

/**
 * 
 */
public class DatabaseStatistikHelper {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseStatistikHelper.class);

  // ...
  private PreparedStatement statistikSelectByTokenStatement = null;

  // ...
  private final NetworkEnum network;

  /**
   * 
   */
  public DatabaseStatistikHelper(@Nonnull NetworkEnum network) {
    this.network = network;
  }

  /**
   * 
   */
  public void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String statistikSelectByTokenSql =
          "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".statistik WHERE token_number=?";

      statistikSelectByTokenStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, statistikSelectByTokenSql));
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
      statistikSelectByTokenStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  public List<StatistikDTO> getStatistikDTOList(@Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getStatistikDTOList()");

    try {
      List<StatistikDTO> statistikDTOList = new ArrayList<>();

      statistikSelectByTokenStatement.setInt(1, token.getNumber());

      ResultSet resultSet = statistikSelectByTokenStatement.executeQuery();

      while (resultSet.next()) {
        StatistikDTO statistikDTO =
            new StatistikDTO(
                resultSet.getDate("day").toLocalDate(),
                resultSet.getInt("token_number"));

        statistikDTO.setDepositCount(resultSet.getInt("deposit_count"));
        statistikDTO.setDepositVin(resultSet.getBigDecimal("deposit_vin"));
        statistikDTO.setDepositVout(resultSet.getBigDecimal("deposit_vout"));

        statistikDTOList.add(statistikDTO);
      }

      resultSet.close();

      return statistikDTOList;
    } catch (Exception e) {
      throw new DfxException("getStatistikDTOList", e);
    }
  }
}
