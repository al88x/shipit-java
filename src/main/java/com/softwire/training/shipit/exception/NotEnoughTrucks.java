package com.softwire.training.shipit.exception;

public class NotEnoughTrucks extends RuntimeException{
    public NotEnoughTrucks(String s) {
        super(s);
    }
}