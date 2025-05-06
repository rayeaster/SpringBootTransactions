package com.trxmgr.hometask.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.trxmgr.hometask.entities.InvalidTransationException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTransationException.class)
    public ResponseEntity<Map<String, String>> handleCustomException(InvalidTransationException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.toString());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
} 