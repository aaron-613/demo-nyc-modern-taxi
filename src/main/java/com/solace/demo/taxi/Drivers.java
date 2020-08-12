package com.solace.demo.taxi;

import java.util.ArrayList;
import java.util.List;

public enum Drivers {

    INSTANCE(500);
    
    
    private final List<Driver> drivers = new ArrayList<>();
    
    Drivers(int numDrivers) {
        for (int i=0;i<numDrivers;i++) {
            drivers.add(Driver.newInstance());
        }
    }
    
    public List<Driver> getDrivers() {
        return drivers;
    }
    
    public Driver getRandomDriver() {
        Driver driver = drivers.get((int)(Math.random()*drivers.size()));
        int count = 1;
        while (driver.getState() != Driver.State.IDLE && count++ < 10) {
            driver = drivers.get((int)(Math.random()*drivers.size()));
        }
        if (count == 10) {
            throw new RuntimeException("COULD NOT FIND A FREE DRIVER!");
        }
        return driver;
    }
    
    
}
