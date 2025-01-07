package com.freakynit.sql.db.stress.bench.deserializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.freakynit.sql.db.stress.bench.utils.RunningPercentilesCalculator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PercentileStatsJsonSerializer extends JsonSerializer<RunningPercentilesCalculator> {
    @Override
    public void serialize(RunningPercentilesCalculator value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Map<String, Object> customMap = new LinkedHashMap<>();
        customMap.put("25", value.estimatePercentile(25));  // keep the keys as strings only since we serialize to json
        customMap.put("50", value.estimatePercentile(50));
        customMap.put("75", value.estimatePercentile(75));
        customMap.put("90", value.estimatePercentile(90));
        customMap.put("95", value.estimatePercentile(95));
        customMap.put("99", value.estimatePercentile(99));

        gen.writeObject(customMap);
    }
}

