package ch.dfx.tools.data;

import ch.dfx.transactionserver.data.DatabaseDTO;

/**
 * 
 */
public class DatabaseData extends DatabaseDTO {

  // ...
  private static String jdbcUrlPrefix = "jdbc:h2:";

  // ...
  private String tcpHost = null;
  private String tcpPort = null;
  private String h2DBName = null;

  private String username = null;
  private String password = null;

  /**
   * 
   */
  public DatabaseData() {
  }

  public String getTcpHost() {
    return tcpHost;
  }

  public void setTcpHost(String tcpHost) {
    this.tcpHost = tcpHost;
  }

  public String getTcpPort() {
    return tcpPort;
  }

  public void setTcpPort(String tcpPort) {
    this.tcpPort = tcpPort;
  }

  public String getH2DBName() {
    return h2DBName;
  }

  public void setH2DBName(String h2dbName) {
    h2DBName = h2dbName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getTcpUrl() {
    return jdbcUrlPrefix + tcpHost + ":" + tcpPort + "/" + h2DBName;
  }
}
