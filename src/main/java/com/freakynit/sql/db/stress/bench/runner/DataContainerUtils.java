package com.freakynit.sql.db.stress.bench.runner;

import com.freakynit.sql.db.stress.bench.configs.QueryPayload;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataContainerUtils {
    public static final Logger logger = LoggerFactory.getLogger(DataContainerUtils.class);

    private static DataContainer initDataContainer(QueryPayload queryPayload) throws IOException {
        List<List<String>> dataRows = null;
        CSVParser csvParser = null;

        if(queryPayload.getFilePath() != null) {
            try {
                dataRows = new ArrayList<>();   // keep it arraylist for faster lookups during benchmark phase

                csvParser = CSVParser.parse(
                        new File(queryPayload.getFilePath()),
                        StandardCharsets.UTF_8,
                        CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreEmptyLines(true)
                                .setTrim(true)
                                .build()
                );

                for (CSVRecord record : csvParser) {
                    dataRows.add(record.toList());
                }

                return new DataContainer(queryPayload, dataRows);
            } finally {
                if(csvParser != null) {
                    try {
                        csvParser.close();
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }

    public static List<DataContainer> initDataContainers(List<QueryPayload> queryPayloads) throws IOException {
        List<DataContainer> dataContainers = new LinkedList<>();

        for(QueryPayload queryPayload : queryPayloads) {
            dataContainers.add(initDataContainer(queryPayload));
        }

        return dataContainers.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
