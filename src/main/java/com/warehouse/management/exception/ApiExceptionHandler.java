package com.warehouse.management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<Map<String,Object>> body(HttpStatus status,String message){
        Map<String,Object> out=new LinkedHashMap<>();out.put("status",status.value());out.put("error",status.getReasonPhrase());out.put("message",message);return ResponseEntity.status(status).body(out);
    }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,Object>> bad(IllegalArgumentException e){return body(HttpStatus.BAD_REQUEST,e.getMessage());}
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<Map<String,Object>> state(IllegalStateException e){return body(HttpStatus.CONFLICT,e.getMessage());}
    @ExceptionHandler(java.util.NoSuchElementException.class) ResponseEntity<Map<String,Object>> missing(java.util.NoSuchElementException e){return body(HttpStatus.NOT_FOUND,e.getMessage());}
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> generic(Exception e){return body(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage()==null?"Server error":e.getMessage());}
}
