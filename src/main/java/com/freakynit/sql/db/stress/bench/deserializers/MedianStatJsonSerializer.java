package com.freakynit.sql.db.stress.bench.deserializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.freakynit.sql.db.stress.bench.utils.RunningMedianCalculator;

import java.io.IOException;

public class MedianStatJsonSerializer extends JsonSerializer<RunningMedianCalculator> {
    @Override
    public void serialize(RunningMedianCalculator value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(value.getMedian());
    }
}

