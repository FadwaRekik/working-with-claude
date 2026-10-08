package com.marlowefinch.ops;

import java.util.List;

/** Thrown when request parameters fail validation; mapped to HTTP 400 by {@link ApiExceptionHandler}. */
public class InvalidQueryException extends RuntimeException {

    private final List<String> errors;

    public InvalidQueryException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
