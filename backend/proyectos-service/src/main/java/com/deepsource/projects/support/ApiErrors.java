package com.deepsource.projects.support;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status()).body(Map.of("message", e.getMessage())); }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("message", "Revisa los datos enviados")); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> database(DataAccessException e) { return ResponseEntity.status(503).body(Map.of("message", "No pudimos guardar o consultar los datos. Intenta nuevamente")); }
}
