package com.freakynit.sql.db.stress.bench.runner;

import com.freakynit.sql.db.stress.bench.configs.QueryPayload;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataContainer {
    private QueryPayload queryPayload;
    private List<List<String>> dataRows = null;

    public DataContainer(QueryPayload queryPayload, List<List<String>> dataRows) {
        this.queryPayload = queryPayload;
        this.dataRows = dataRows;
    }
}
