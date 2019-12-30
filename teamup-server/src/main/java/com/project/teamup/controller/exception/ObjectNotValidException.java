package com.project.teamup.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This Exception extends the RuntimeException class to use for Spring Validator Annotations.
 * @author Sebastian
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ObjectNotValidException extends RuntimeException {

    public ObjectNotValidException(){
        super();
    }

    public ObjectNotValidException(String message){
        super(message);
    }

    public ObjectNotValidException(String message, Throwable throwable){
        super(message, throwable);
    }
}
