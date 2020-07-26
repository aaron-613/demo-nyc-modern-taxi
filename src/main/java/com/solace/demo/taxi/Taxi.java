package com.solace.demo.taxi;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import com.solace.aa.gpsdemo.RouteLoader.Direction;
import com.solace.aaron.rnrf.RangeUtils;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;


public class Taxi implements Runnable {

    static final int LAT_PADDING = 2;
    static final int LON_PADDING = 3;
    static final int LAT_FACTOR = 6;
    static final int LON_FACTOR = 6;
    static final int ROUTE_PADDING = 3;  // 000-999
    static final int VEH_NUM_PADDING = 4; // 1000-9999
    
    static RouteLoader taxiLoauder = new RouteLoader();
    static {
        String filename = "coords2.txt";
        try {
            taxiLoader.load(filename);
        } catch (FileNotFoundException e) {
            System.out.println("Taxi class could not find requested filename "+filename+" on the classpath");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("Taxi class had I/O issue loading filename "+filename);
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public enum Status {
        OK,
    }
    
    final int routeNum;
    int positionIndex;  // which 'tick' along the route is this?
    final int vehicleNum;
    Status status = Status.OK;

    // each Bus should be listening on their topics: comm/route/012, comm/bus/1234, comm/broadcast
    
    protected Taxi(int taxiNum) {
        this.routeNum = (int)Math.floor(Math.random() * taxiLoader.getNumRoutes());  // start this car somewhere along the new raw route (pick an index between 0-length)
        this.positionIndex = (int)Math.floor(Math.random() * taxiLoader.getRoute(this.routeNum).coords.size()-1);  // start this car somewhere along the new raw route (pick an index between 0-length)
        if (this.positionIndex == taxiLoader.getRoute(this.routeNum).coords.size()) {
            throw new AssertionError("somehow made a position that's the same size!");
        }
        //this.vehicleNum = loader.addVehicle(this);  // not good code practice to call another method with 'this' while still in the constructor!
        this.vehicleNum = taxiNum;
    }
    
    int calcSpeed() {
        return VehicleUtils.calcSpeedTaxi(this.routeNum, this.positionIndex);  // Point2D.Double.distance(lastPos.x, lastPos.y, getPosition().x, getPosition().y) * 80000;  // rougly km/h at 5 second updates
    }

    // 192.168.56.101 default default

    public void tick() {
        if (status == Status.OK) {
            if (direction == Direction.FORWARD) {
                positionIndex++;
                if (positionIndex >= taxiLoader.getRouteCoords(routeNum).size()-1) {  // so size==10, then pos=9 and bus turns around
                    positionIndex = 0;  // reset back to beginning... always go forward
//                        direction = Direction.BACKWARD;
                }
            } else {
                positionIndex--;
                if (positionIndex <= 0) {
                    direction = Direction.FORWARD;
                }
            }
        }
        GpsGenerator.onlyInstance().sendMessage(buildMessage(),genTopic());
        //BroadcastQueue.INSTANCE.queue.add.onlyInstance().sendMessage(buildMessage("speed="+speed+"status="+status),JCSMPFactory.onlyInstance().createTopic(genTopicString()));
    }

    @Override
    public void run() {
        try {
            tick();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.printf("route %d, pos %d, veh num %d, dir %s%n",routeNum,positionIndex,vehicleNum,direction);
            System.exit(-1);
        } catch (Error e) {
            e.printStackTrace();
            System.err.printf("route %d, pos %d, veh num %d, dir %s%n",routeNum,positionIndex,vehicleNum,direction);
            System.exit(-1);
        }
    }

    public Point2D.Double getPosition() {
        return taxiLoader.getRouteCoords(routeNum).get(positionIndex);
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    @Override
    public String toString() {
        return String.format("Bus %d %s on route %d, pos %d",this.vehicleNum,this.direction,this.routeNum,this.positionIndex);
    }

    void receiveMessage(BytesXMLMessage msg) {
        // is this a control message?  Maybe we should stop?  Or start?
        System.out.println(toString()+" Received a message!");
    }
    
    BytesXMLMessage buildMessage() {
        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        msg.setText(genPayload());
        return msg;
    }

    Destination genTopic() {
        // geo/bus/1234/001.238212/103.128345/023/OK
        StringBuilder sb = new StringBuilder("geo/taxi/");
//        sb.append(RangeUtils.helperMakeSubCoordString(quadrant.xNegativeModifier,innerX,xFactor,xPadding,xNeedNegs)).append("*/");
        sb.append(RangeUtils.helperMakeRangeCoordString(vehicleNum,0,4,false)).append("/");
        sb.append(RangeUtils.helperMakeRangeCoordString(getPosition().x,LAT_FACTOR,LAT_PADDING,true)).append("/");
        sb.append(RangeUtils.helperMakeRangeCoordString(getPosition().y,LON_FACTOR,LON_PADDING,true)).append("/");
        //sb.append(RangeUtils.helperMakeRangeCoordString(this.routeNum,0,3,false)).append("/");
        sb.append(getPracticalStatus());
        return JCSMPFactory.onlyInstance().createTopic(sb.toString());
    }
    
    String getPracticalStatus() {
        return status.name();
    }
    
    String genPayload() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        if (getPracticalStatus().equals("FAULT")) {
            job.add("status",getPracticalStatus()+": "+status);
        } else {
            job.add("status",getPracticalStatus());
        }
        job.add("speed",calcSpeed() > 60 ? 60 : calcSpeed());
        job.add("heading",VehicleUtils.calcHeading(routeNum,positionIndex));
        return job.build().toString();
    }
    

    
    
    
}
