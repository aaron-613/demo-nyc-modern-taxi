package com.solace.demo.taxi;

import java.awt.geom.Point2D;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solace.demo.taxi.Driver.State;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;

public class Ride implements Runnable {

    enum Status {
        NOT_STARTED,
        EN_ROUTE,
        FINISHED;
    }
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Logger logger = LogManager.getLogger(Ride.class);

    private final String rideId;
    private final Driver driver;
    private final Passenger passenger;
    private final int routeNum;
    private final RouteLoader.Route route;
    private Status status = Status.NOT_STARTED;
    private int routePositionIndex = 0;
    
    private Ride() {
        rideId = UUID.randomUUID().toString();
        driver = Drivers.INSTANCE.getRandomIdleDriver();
        driver.setState(State.OCCUPIED);
        passenger = Passenger.newPassenger();
        routeNum = (int)(Math.random()*RouteLoader.INSTANCE.getNumRoutes());
        route = RouteLoader.INSTANCE.getRoute(routeNum);
    }
    
    public static Ride newRide() {
        return new Ride();
    }
    
    /**
     * Starts somewhere in the middle of the ride
     * @return
     */
    public static Ride randomRide() {
        Ride ride = new Ride();
        if (ride.route.coords.size() < 5) return ride;  // is it a really short ride? less than 5 ticks? start at the beginning
        // else... start somewhere in the middle, and
        ride.routePositionIndex = 2 + (int)(Math.round(Math.random()*(ride.route.coords.size()-4)));
        ride.status = Status.EN_ROUTE;
        return ride;
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
    
    
    @Override
    public void run() {
        tick();
    }
    
    public void tick() {
        try {
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
                logger.debug("Starting ride: "+this.toString());
                break;
            case EN_ROUTE:
                routePositionIndex++;
                if (routePositionIndex == route.coords.size()-1) {  // the end!
                    rideStatus = "dropoff";
                    status = Status.FINISHED;
                } else {
                    rideStatus = "enroute";
                }
                break;
            case FINISHED:  // we are done now
            default:
                logger.debug("DELETING ride: "+this.toString());
                driver.setState(State.IDLE);
                GpsGenerator.INSTANCE.removeRide(this);
                GpsGenerator.INSTANCE.addNewRide();  // make a new one to replace this one
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
            
            String topic = topicSb.toString();
            String payload = getPayload(rideStatus);
            msg.setText(payload);
            GpsGenerator.INSTANCE.sendMessage(msg,topic);
        } catch (Exception e) {
            logger.warn("Caught during tick() on "+this.toString()+", with payload :"+getPayload("test"),e);
        }
    }
    
    
    
    public String getPayload(String rideStatus) {
        // {"ride_id":"00001e3e-00d4-4a13-a358-61ccc3a7e86a","point_idx":1,"latitude":40.7875,"longitude":-73.97457,"timestamp":"2020-06-04T20:35:17.83958-04:00",...
        //      "meter_reading":0.036538463,"meter_increment":0.036538463,"ride_status":"enroute","passenger_count":1}
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.emptyMap());
        Point2D.Float point = route.coords.get(routePositionIndex);
        JsonObjectBuilder job = factory.createObjectBuilder();
        job.add("ride_id",rideId)
                .add("point_idx",routePositionIndex)
                .add("latitude",Math.round(point.y*1000000)/1000000.0)
                .add("longitude",Math.round(point.x*1000000)/1000000.0)
/* optional */  .add("heading",VehicleUtils.calcHeading(routeNum,routePositionIndex))
/* optional */  .add("speed",VehicleUtils.calcSpeed(routeNum,routePositionIndex))
                .add("timestamp",LocalDateTime.now().format(FORMATTER))
                .add("meter_reading",Math.round(route.meterAmount.get(routePositionIndex)*100)/100.0)
                .add("meter_increment",Math.round(route.meterIncrement*10000000)/10000000.0)
                .add("ride_status",rideStatus)
                .add("passenger_count",route.passengerCount)
                .add("driver",factory.createObjectBuilder()
                        .add("driver_id",getDriver().getId())
                        .add("first_name",getDriver().getFirstName())
                        .add("last_name",getDriver().getLastName())
                        .add("rating",getDriver().getRating())
                        .add("car_class",getDriver().getCarClass()))
                .add("passenger",factory.createObjectBuilder()
                        .add("passenger_id",getPassenger().getId())
                        .add("first_name",getPassenger().getFirstName())
                        .add("last_name",getPassenger().getLastName())
                        .add("rating",getPassenger().getRating()));
                        
                
                        
                        
//        return job.build().toString();
        // how about pretty print instead??
        StringWriter writer = new StringWriter();
        Map<String, Object> properties = new HashMap<>();
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
        JsonWriter jsonWriter = writerFactory.createWriter(writer);
        jsonWriter.writeObject(job.build());
        jsonWriter.close();
        return writer.toString();
    }
    
    

    @Override
    public String toString() {
        return String.format("RideId %s: %s driving %s on route %d at %s",rideId,driver,passenger,routeNum,getCoord());
    }

    
}
