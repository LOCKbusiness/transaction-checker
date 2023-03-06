package ch.dfx.reporting;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class MasternodeReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeReporting.class);

  // ...
  private static final BigDecimal MASTERNODE_BALANCE = new BigDecimal(20000);

  // ...
  private BigDecimal totalBalance = BigDecimal.ZERO;

  /**
   * 
   */
  public MasternodeReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);
  }

  /**
   * 
   */
  public BigDecimal getTotalBalance() {
    return totalBalance;
  }

  /**
   * 
   */
  public RowDataList createRowDataList() throws DfxException {
    LOGGER.trace("createRowDataList()");

    // ...
    totalBalance = BigDecimal.ZERO;

    // ...
    RowDataList rowDataList = new RowDataList(2);

    // ...
    List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = databaseBlockHelper.getMasternodeWhitelistDTOList();

    for (MasternodeWhitelistDTO masternodeWhitelistDTO : masternodeWhitelistDTOList) {
      String state = masternodeWhitelistDTO.getState();

      if ("ENABLED".equals(state)
          || "PRE_ENABLED".equals(state)
          || "PRE_RESIGNED".equals(state)) {
        RowData rowData = new RowData();
        rowData.addCellData(new CellData().setValue(masternodeWhitelistDTO.getOwnerAddress()));
        rowData.addCellData(new CellData().setValue(MASTERNODE_BALANCE));

        rowDataList.add(rowData);

        totalBalance = totalBalance.add(MASTERNODE_BALANCE);
      }
    }

    return rowDataList;
  }
}
