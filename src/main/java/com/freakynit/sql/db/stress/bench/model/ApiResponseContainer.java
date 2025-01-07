package com.freakynit.sql.db.stress.bench.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseContainer<T> {
    private boolean success;
    private String errorMessage;
    private T data;
}
