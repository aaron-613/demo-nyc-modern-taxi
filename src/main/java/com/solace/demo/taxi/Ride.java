package com.solace.demo.taxi;

import java.awt.geom.Point2D;
import java.util.UUID;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;

public class Ride {

    enum Status {
        NOT_STARTED,
        EN_ROUTE,
        FINISHED;
    }
    
    private final String rideId;
    private final Driver driver;
    private final Passenger passenger;
    private final int routeNum;
    private final RouteLoader.Route route;
    private Status status = Status.NOT_STARTED;
    private int routePositionIndex = 0;
    
    private Ride() {
        rideId = UUID.randomUUID().toString();
        driver = Drivers.INSTANCE.getRandomDriver();
        passenger = Passenger.newPassenger();
        routeNum = (int)(Math.random()*RouteLoader.INSTANCE.getNumRoutes());
        route = RouteLoader.INSTANCE.getRoute(routeNum);
    }
    
    public static Ride newRide() {
        return new Ride();
    }

    public Driver getDriver() {
        return driver;
    }

    public Passenger getPassenger() {
        return passenger;
    }
    
    public int getPassengerCount() {
        return route.passengerCount;
    }

    public int getRouteNum() {
        return routeNum;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public int getRoutePositionIndex() {
        return routePositionIndex;
    }
    
    public Point2D.Float getCoord() {
        return RouteLoader.INSTANCE.getCoord(routeNum,routePositionIndex);
    }
    
    
    
    public void tick() {
        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        /*
         * {"ride_id":"00001e3e-00d4-4a13-a358-61ccc3a7e86a","point_idx":0,"latitude":40.78754,"longitude":-73.97467,"timestamp":"2020-06-04T20:35:15.19397-04:00","meter_reading":0,"meter_increment":0.036538463,"ride_status":"pickup","passenger_count":1}
         * {"ride_id":"00001e3e-00d4-4a13-a358-61ccc3a7e86a","point_idx":1,"latitude":40.7875,"longitude":-73.97457,"timestamp":"2020-06-04T20:35:17.83958-04:00","meter_reading":0.036538463,"meter_increment":0.036538463,"ride_status":"enroute","passenger_count":1}
         * {"ride_id":"00001e3e-00d4-4a13-a358-61ccc3a7e86a","point_idx":364,"latitude":40.76278000000001,"longitude":-73.9735,"timestamp":"2020-06-04T20:51:18.19397-04:00","meter_reading":13.3,"meter_increment":0.036538463,"ride_status":"dropoff","passenger_count":1}        final String baseTopic = "taxinyc/ops/ride/updated/v1/";
         */
        String rideStatus;
        switch (status) {
        case NOT_STARTED:  // guess we're starting now!
            status = Status.EN_ROUTE;
            rideStatus = "pickup";
            break;
        case EN_ROUTE:
            routePositionIndex++;
            if (route.coords.size() == routePositionIndex) {  // the end!
                rideStatus = "dropoff";
            } else {
                rideStatus = "enroute";
            }
            break;
        case FINISHED:  // why are we even here?
        default:
            return;
        }
        // topic = taxinyc/ops/ride/updated/v1/${ride_status}/${driver_id}/${passenger_id}/${current_longitude}/${current_latitude}
        final String baseTopic = "taxinyc/ops/ride/updated/v1/";
        StringBuilder topicSb = new StringBuilder(baseTopic);
        
        Point2D.Float point = route.coords.get(routePositionIndex);

        topicSb.append(rideStatus).append('/')
                .append(driver.getId()).append('/')
                .append(passenger.getId()).append('/')
                .append(String.format("%010.5f",point.x)).append('/')
                .append(String.format("%09.5f",point.y));
        
        
    }
    
    
    
    public String getPayload() {
        // {"ride_id":"00001e3e-00d4-4a13-a358-61ccc3a7e86a","point_idx":1,"latitude":40.7875,"longitude":-73.97457,"timestamp":"2020-06-04T20:35:17.83958-04:00","meter_reading":0.036538463,"meter_increment":0.036538463,"ride_status":"enroute","passenger_count":1}
        
        
        
        
    }
    
    

    @Override
    public String toString() {
        return String.format("%s: %s driving %s on route %d at %s",rideId,driver,passenger,routeNum,getCoord());
    }
    
    
    
    
}
