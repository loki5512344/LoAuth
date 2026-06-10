package dev.loki.loAuth.common.cfg;

public interface DbConfig {
    String getDbType();
    String getDbHost();
    int    getDbPort();
    String getDbName();
    String getDbUser();
    String getDbPassword();
    int    getAuthTimeoutSeconds();
    int    getVerificationCodeLength();

    default String getJdbcUrl() {
        if ("sqlite".equals(getDbType())) {
            return "jdbc:sqlite:" + getDbName();
        }
        return "jdbc:postgresql://" + getDbHost() + ":" + getDbPort() + "/" + getDbName();
    }
}
