package com.freakynit.sql.db.stress.bench;


import com.freakynit.sql.db.stress.bench.model.StatsApiResponseModel;

public interface StatsProviderInterface {
    StatsApiResponseModel getSnapshot();
}
