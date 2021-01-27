package com.solace.demo.taxi;

import java.awt.geom.Point2D;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Collections.emptyMap());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final ZoneId NYC_TZ = ZoneId.of("America/New_York");

    private static final Logger logger = LogManager.getLogger(Ride.class);

    private final String rideId;
    private final Driver driver;
    private final Passenger passenger;
    private final int routeNum;
    private final RouteLoader.Route route;
    private Status status = Status.NOT_STARTED;
    private int routePositionIndex = 0;
    
    private Ride(Passenger passenger) {
        this.rideId = UUID.randomUUID().toString();
        this.driver = Drivers.INSTANCE.getRandomIdleDriver();
        this.driver.setState(State.OCCUPIED);
        this.passenger = passenger;
        this.routeNum = (int)(Math.random()*RouteLoader.INSTANCE.getNumRoutes());
        this.route = RouteLoader.INSTANCE.getRoute(routeNum);
    }
    
    public static Ride newRide() {
        return new Ride(Passenger.randomPassenger());
    }
    
    public static Ride newRide(Passenger passenger) {
        return new Ride(passenger);
    }
    
    /**
     * Starts somewhere in the middle of the ride
     * @return
     */
    public static Ride randomRide() {
        Ride ride = new Ride(Passenger.randomPassenger());
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
    
    /**
     * This is called when the ride object is first created by the GPS Publisher. It will send a "fake" ride request message, as if a human had done it
     */
    public void makeRideCalled() {
        assert status == Status.NOT_STARTED;
        try {
            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            //taxinyc/ops/ride/called/v1/${car_class}/${passenger_id}/${pick_up_longitude}/${pick_up_latitude}
            StringBuilder topicSb = new StringBuilder("taxinyc/ops/ride/called/v1/");
            Point2D.Float point = route.coords.get(0);  // first coord
            topicSb.append(driver.getCarClass()).append('/')
                    .append(String.format("%08d",passenger.getId())).append('/')
                    .append(String.format("%09.5f",point.y)).append('/')
                    .append(String.format("%010.5f",point.x));
            msg.setText(getRideCalledPayload());
            GpsGenerator.INSTANCE.sendMessage(msg,topicSb.toString());
            msg.reset();
            // now, send the "response" or "reply"
            // topic = taxinyc/ops/ride/updated/v1/accepted/${driver_id}/${passenger_id}/${current_longitude}/${current_latitude}
            topicSb = new StringBuilder("taxinyc/ops/ride/updated/v1/accepted/");
            point = route.coords.get(Math.min(5,route.coords.size()-1));  // up to 5 "ticks" away
            topicSb.append(String.format("%08d",driver.getId())).append('/')
                    .append(String.format("%08d",passenger.getId())).append('/')   // use the full-width, 8-char version
                    .append(String.format("%09.5f",point.x)).append('/')
                    .append(String.format("%010.5f",point.y));
            String payload = getRideUpdatedPayload("accepted");
            msg.setText(payload);
            GpsGenerator.INSTANCE.sendMessage(msg,topicSb.toString());
            
        } catch (RuntimeException e) {
            // shouldn't have anything thrown from here
            logger.warn("HEY! Had a runtime exception thrown from makeRideRequest: ",e);
        } finally {
            
        }
    }

    public String getRideCalledPayload() {
        /*
    {
      "ridecalled_id": "23232323",
      "information_source": "PassengerMobileApp",
      "dropoff_latitude": 40.75473,
      "dropoff_longitude": -73.98385,
      "pickup_latitude": 40.75473,
      "pickup_longitude": -73.98385,
      "timestamp": "2020-06-03T16:51:47.292-04:00",
      "passenger_count": 1,
      "car_class": "SUV",
      "passenger": {
        "passenger_id": 2345243,
        "first_name": "Jesse",
        "last_name": "Menning",
        "rating": 2.23
      }
    }
         */
        Point2D.Float dropOffPoint = route.coords.get(route.coords.size()-1);
        Point2D.Float pickUpPoint = route.coords.get(0);
        JsonObjectBuilder job = JSON_BUILDER_FACTORY.createObjectBuilder();
        job.add("ridecalled_id",rideId)  // should be rideId?
                .add("information_source","PassengerMobileApp")
                .add("dropoff_latitude",Math.round(dropOffPoint.y*1000000)/1000000.0)
                .add("dropoff_longitude",Math.round(dropOffPoint.x*1000000)/1000000.0)
                .add("pickup_latitude",Math.round(pickUpPoint.y*1000000)/1000000.0)
                .add("pickup_longitude",Math.round(pickUpPoint.x*1000000)/1000000.0)
                .add("timestamp",ZonedDateTime.now(NYC_TZ).format(FORMATTER))
                .add("passenger_count",route.passengerCount)
                .add("car_class",getDriver().getCarClass())
                .add("passenger",JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("passenger_id",getPassenger().getId())
                        .add("first_name",getPassenger().getFirstName())
                        .add("last_name",getPassenger().getLastName())
                        .add("rating",getPassenger().getRating()));

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
                if (!passenger.isHuman()) {  // i.e. it's a robot, one of the "background noise" rides driving around
                    GpsGenerator.INSTANCE.addNewRide(newRide());  // make a new random one to replace this one that is ending
                }
                return;
            }
            // topic = taxinyc/ops/ride/updated/v1/${ride_status}/${driver_id}/${passenger_id}/${current_longitude}/${current_latitude}
            final String baseTopic = "taxinyc/ops/ride/updated/v1/";
            StringBuilder topicSb = new StringBuilder(baseTopic);
            
            Point2D.Float point = route.coords.get(routePositionIndex);
    
            topicSb.append(rideStatus).append('/')
                    .append(String.format("%08d",driver.getId())).append('/')     // use the full-width, 8-char version
                    .append(String.format("%08d",passenger.getId())).append('/')  // use the full-width, 8-char version
                    .append(String.format("%09.5f",point.y)).append('/')
                    .append(String.format("%010.5f",point.x));
            
            String topic = topicSb.toString();
            String payload = getRideUpdatedPayload(rideStatus);
            msg.setText(payload);
            GpsGenerator.INSTANCE.sendMessage(msg,topic);
        } catch (Exception e) {
            logger.warn("Caught during tick() on "+this.toString(),e);
            try {
                logger.warn(" with payload :"+getRideUpdatedPayload("test"));
            } catch (Exception e1) {
                // silent... probably problem with something in the payload
            }
        }
    }

    
    /**
     * 
     * @param rideStatus one of "accepted", "pickup", "enroute", "dropoff"
     * @return
     */
    public String getRideUpdatedPayload(final String rideStatus) {
        // {"ride_id":"00001e3e-00d4-4a13-a358-61ccc3a7e86a","point_idx":1,"latitude":40.7875,"longitude":-73.97457,"timestamp":"2020-06-04T20:35:17.83958-04:00",...
        //      "meter_reading":0.036538463,"meter_increment":0.036538463,"ride_status":"enroute","passenger_count":1}
        Point2D.Float point = route.coords.get(routePositionIndex);
        JsonObjectBuilder job = JSON_BUILDER_FACTORY.createObjectBuilder();
        job.add("ride_id",rideId)
                .add("information_source","RideDispatcher")
                .add("point_idx",routePositionIndex)
                .add("latitude",Math.round(point.y*1000000)/1000000.0)
                .add("longitude",Math.round(point.x*1000000)/1000000.0)
/* optional */  .add("heading",VehicleUtils.calcHeading(routeNum,routePositionIndex))
/* optional */  .add("speed",VehicleUtils.calcSpeed(routeNum,routePositionIndex))
                .add("timestamp",ZonedDateTime.now(NYC_TZ).format(FORMATTER))
                .add("meter_reading",Math.round(route.meterAmount.get(routePositionIndex)*100)/100.0)
                .add("meter_increment",Math.round(route.meterIncrement*10000000)/10000000.0)
                .add("ride_status",rideStatus)
                .add("passenger_count",route.passengerCount)
                .add("driver",JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("driver_id",getDriver().getId())
                        .add("first_name",getDriver().getFirstName())
                        .add("last_name",getDriver().getLastName())
                        .add("rating",getDriver().getRating())
                        .add("car_class",getDriver().getCarClass()))
                .add("passenger",JSON_BUILDER_FACTORY.createObjectBuilder()
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
