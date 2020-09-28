package com.solace.demo.taxi;

public class Passenger {

    public enum Status {
        IDLE,
        WAITING,
        EN_ROUTE,
        ;
    }
    
    private final long id;
    private final String firstName;
    private final String lastName;
    private final boolean human;
//    private Status status;
//    private String routeId;
    
    
    private Passenger(String firstName, String lastName, boolean human) {
        this.id = (int)(Math.random()*100_000_000);  // always assigned a random ID
        this.firstName = firstName;
        this.lastName = lastName;
        this.human = human;
    }
    
    
    public static Passenger randomPassenger() {
        return new Passenger(Names.randomFirstName(),Names.randomLastName(),false);
    }
    
    public static Passenger newPassenger(String name) {
        String[] split = name.split(" ");
        if (split.length > 1) {
            return new Passenger(split[0],split[1],true);
        }
        return new Passenger(split[0],Names.randomLastName(),true);
    }
    
    public long getId() {
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
    
    /**
     * Is this Passenger object representing someone who actually requested a ride?  Or one of the robot riders?
     */
    public boolean isHuman() {
        return human;
    }
    
    public float getRating() {
        return 4.3f + (float)(Math.random()*0.7);
    }

    @Override
    public String toString() {
        return getName();
    }
    
    
}
