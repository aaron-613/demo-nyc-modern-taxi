package com.solace.demo.taxi;

public class Passenger {

    public enum Status {
        IDLE,
        WAITING,
        EN_ROUTE,
        ;
    }
    
    private final String id;
    private final String firstName = "Sam";
    private final String lastName = "Doe";
//    private Status status;
//    private String routeId;
    
    
    private Passenger() {
        id = ""+(int)(Math.random()*100_000_000);
        
    }
    
    
    public static Passenger newPassenger() {
        return new Passenger();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return firstName + " " + lastName;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
    
    public float getRating() {
        return 4.87f;
    }

    @Override
    public String toString() {
        return getName();
    }
    
    
}
