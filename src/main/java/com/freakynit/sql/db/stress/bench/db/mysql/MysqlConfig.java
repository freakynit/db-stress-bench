package com.freakynit.sql.db.stress.bench.db.mysql;

import lombok.Data;

@Data
public class MysqlConfig {
    private String connectionUrl;
    private String username;
    private String password;
    private Boolean autoReconnect;
}
