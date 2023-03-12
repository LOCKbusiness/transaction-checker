package ch.dfx.reporting.transparency.data;

import ch.dfx.TransactionCheckerUtils;

/**
 * 
 */
public class ReportDTO {
  private DFISheetDTO dfiSheetDTO = null;
  private DUSDSheetDTO dusdSheetDTO = null;
  private TokenSheetDTO tokenSheetDTO = null;

  /**
   * 
   */
  public ReportDTO() {
  }

  public DFISheetDTO getDfiSheetDTO() {
    return dfiSheetDTO;
  }

  public void setDfiSheetDTO(DFISheetDTO dfiSheetDTO) {
    this.dfiSheetDTO = dfiSheetDTO;
  }

  public DUSDSheetDTO getDusdSheetDTO() {
    return dusdSheetDTO;
  }

  public void setDusdSheetDTO(DUSDSheetDTO dusdSheetDTO) {
    this.dusdSheetDTO = dusdSheetDTO;
  }

  public TokenSheetDTO getTokenSheetDTO() {
    return tokenSheetDTO;
  }

  public void setTokenSheetDTO(TokenSheetDTO tokenSheetDTO) {
    this.tokenSheetDTO = tokenSheetDTO;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
