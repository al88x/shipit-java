package com.softwire.training.shipit.exception;

public class NotEnoughSpaceInTruckException extends RuntimeException {
    public NotEnoughSpaceInTruckException(String s) {
        super(s);
    }
}
