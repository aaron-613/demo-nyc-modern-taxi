package com.solace.demo.taxi;

public class Passenger {

    public enum Status {
        IDLE,
        WAITING,
        EN_ROUTE,
        ;
    }
    
    private final String id;
    private Status status;
    private String routeId;
    
    
    public Passenger(String id) {
        this.id = id;
    }
    
    
    
    
    
    
}
