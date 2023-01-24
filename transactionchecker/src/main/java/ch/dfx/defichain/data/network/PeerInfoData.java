package ch.dfx.defichain.data.network;

import java.util.List;

import ch.dfx.common.TransactionCheckerUtils;

/**
 * 
 */
public class PeerInfoData {
  private Integer id = null;

  private String addr = null;
  private String addlocal = null;
  private String addbind = null;

  private String version = null;
  private String subver = null;

  private Long synced_headers = null;
  private Long synced_blocks = null;

  private List<Long> inflight = null;

  /**
   * 
   */
  public PeerInfoData() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getAddr() {
    return addr;
  }

  public void setAddr(String addr) {
    this.addr = addr;
  }

  public String getAddlocal() {
    return addlocal;
  }

  public void setAddlocal(String addlocal) {
    this.addlocal = addlocal;
  }

  public String getAddbind() {
    return addbind;
  }

  public void setAddbind(String addbind) {
    this.addbind = addbind;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getSubver() {
    return subver;
  }

  public void setSubver(String subver) {
    this.subver = subver;
  }

  public Long getSynced_headers() {
    return synced_headers;
  }

  public void setSynced_headers(Long synced_headers) {
    this.synced_headers = synced_headers;
  }

  public Long getSynced_blocks() {
    return synced_blocks;
  }

  public void setSynced_blocks(Long synced_blocks) {
    this.synced_blocks = synced_blocks;
  }

  public List<Long> getInflight() {
    return inflight;
  }

  public void setInflight(List<Long> inflight) {
    this.inflight = inflight;
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }

}
