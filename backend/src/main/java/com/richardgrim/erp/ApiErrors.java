package com.richardgrim.erp;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.TreeMap;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(HttpMessageNotReadableException.class)
 ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException ex) {
  return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
   HttpStatus.BAD_REQUEST,"La solicitud contiene datos o un estado no válidos."));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Revisa los campos del formulario.");
  var fields=new TreeMap<String,String>();
  ex.getBindingResult().getFieldErrors().forEach(e->fields.put(e.getField(),e.getDefaultMessage()));
  p.setProperty("errors",fields); return ResponseEntity.badRequest().body(p);
 }
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<ProblemDetail> missing(ResponseStatusException ex) {
  return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(),ex.getReason()));
 }
}
