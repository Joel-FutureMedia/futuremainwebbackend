package com.futuremedia.config;

import com.futuremedia.models.ApiResponse;
import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.UnauthorizedException;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Invalid email or password"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("Access denied. Insufficient permissions."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Validation failed", errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("File size exceeds the maximum allowed limit"));
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            ConstraintViolationException.class,
            DataAccessException.class,
            PersistenceException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleDataAccess(Exception ex) {
        String root = rootSqlMessage(ex);
        System.err.println("Database error root cause: " + root);
        ex.printStackTrace();

        String message = root;
        if (root != null && root.toLowerCase().contains("duplicate")) {
            message = "Duplicate record. This email or value already exists.";
        } else if (root != null && root.toLowerCase().contains("foreign key")) {
            message = "Cannot complete this action because related records still exist.";
        } else if (root != null && root.contains("25P02")) {
            message = "Database transaction was aborted. Please retry the request.";
        } else if (root == null || root.isBlank()) {
            message = "A database error occurred. Please try again.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        String root = rootSqlMessage(ex);
        String message = root != null ? root : (ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        System.err.println("Unhandled error: " + message);
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(message));
    }

    private String rootSqlMessage(Throwable ex) {
        Throwable current = ex;
        SQLException sqlEx = null;
        while (current != null) {
            if (current instanceof SQLException sql) {
                sqlEx = sql;
            }
            current = current.getCause();
        }
        if (sqlEx != null) {
            // Walk to the deepest SQLException that is NOT the 25P02 follow-up
            SQLException deepestUseful = sqlEx;
            SQLException next = sqlEx;
            while (next != null) {
                if (next.getSQLState() != null && !"25P02".equals(next.getSQLState())) {
                    deepestUseful = next;
                }
                next = next.getNextException() != null ? next.getNextException() : null;
                if (next == null && deepestUseful.getCause() instanceof SQLException causeSql) {
                    next = causeSql;
                }
            }
            // Prefer non-25P02 messages from the cause chain
            Throwable t = ex;
            String fallback = sqlEx.getMessage();
            while (t != null) {
                if (t instanceof SQLException s && s.getSQLState() != null && !"25P02".equals(s.getSQLState())) {
                    return s.getMessage();
                }
                if (t.getMessage() != null
                        && !t.getMessage().contains("current transaction is aborted")
                        && t instanceof SQLException) {
                    fallback = t.getMessage();
                }
                t = t.getCause();
            }
            return deepestUseful.getMessage() != null ? deepestUseful.getMessage() : fallback;
        }
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
