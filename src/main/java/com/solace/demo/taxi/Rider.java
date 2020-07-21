package com.solace.demo.taxi;

public class Rider {

    public enum Status {
        IDLE,
        WAITING,
        EN_ROUTE,
        ;
    }
    
    private final String id;
    private Status status;
    private String routeId;
    
    
    public Rider(String id) {
        this.id = id;
    }
    
    
    
    
    
    
}
