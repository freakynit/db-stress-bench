package com.freakynit.sql.db.stress.bench.runner;

import lombok.Getter;

@Getter
public class RunTimeDataContainer {
    private DataContainer dataContainer;
    int curIndex = 0;
    int numRows = 0;

    public RunTimeDataContainer(DataContainer dataContainer) {
        this.dataContainer = dataContainer;
        this.curIndex = 0;
        this.numRows = dataContainer.getDataRows().size();
    }
}
