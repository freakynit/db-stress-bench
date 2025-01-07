package com.freakynit.sql.db.stress.bench.db.postgre;

import lombok.Data;

@Data
public class PostgreConfig {
    private String connectionUrl;
    private String username;
    private String password;
    private Boolean autoReconnect;
}
