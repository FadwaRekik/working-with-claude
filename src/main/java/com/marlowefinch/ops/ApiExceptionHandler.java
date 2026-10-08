package com.marlowefinch.ops;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns invalid query parameters into {@code 400 {"errors": [...]}} (TODO-232). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, List<String>> invalidQuery(InvalidQueryException e) {
        return Map.of("errors", e.errors());
    }
}
