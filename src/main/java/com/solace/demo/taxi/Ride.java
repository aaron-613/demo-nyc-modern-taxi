package com.solace.demo.taxi;

public class Ride {

    
    
    private final Driver driver;
    private final String passenger;
    private final int routeIndex;
    
    private Ride() {
        driver = Drivers.INSTANCE.getRandomDriver();
        passenger = "John Doe";
        routeIndex = (int)(Math.random()*RouteLoader.INSTANCE.getNumRoutes());
    }
    
    public static Ride newRide() {
        return new Ride();
    }

    public Driver getDriver() {
        return driver;
    }

    public String getPassenger() {
        return passenger;
    }

    public int getRouteIndex() {
        return routeIndex;
    }
    
    
    
}
