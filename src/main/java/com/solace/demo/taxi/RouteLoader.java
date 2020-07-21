package com.solace.demo.taxi;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RouteLoader {

    public class Route {
        
        final int passengerCount;
        final float meterIncrement;
        final List<Float> meterAmount;
        final List<Point2D.Float> coords;  // lat=x, lon=y
        
        private Route(int passengerCount, float meterIncrement, List<Float> meterAmount, List<Point2D.Float> coords) {
            this.passengerCount = passengerCount;
            this.meterIncrement = meterIncrement;
            this.meterAmount = meterAmount;
            this.coords = coords;
        }
    }

    List<Route> routes = new ArrayList<Route>();

    public int getNumRoutes() {
        return routes.size();
    }
    
    public Route getRoute(int routeNum) {
        return routes.get(routeNum);
    }
    
    public List<Point2D.Float> getRouteCoords(int routeNum) {
        return routes.get(routeNum).coords;
    }
    
    public Point2D.Float getCoord(int routeNum, int routeIndex) {
        return routes.get(routeNum).coords.get(routeIndex);
    }
    
    
    ///////////////////////////////////
    
    // int passenger count | float meter_increment | [float meter_reading, float lat, float lon];
    Route parseRawRouteText(String routeTextLine) {
        String[] fields = routeTextLine.split("\\|");
        int passengerCount = Integer.parseInt(fields[0]);
        float meterIncrement = Float.parseFloat(fields[1]);
        String[] textCoords = fields[2].split(";");
        List<Float> meterAmount = new ArrayList<>();
        List<Point2D.Float> coords = new ArrayList<>();
        for (String triple : textCoords) {
            String[] amountlatlon = triple.split(",");
            meterAmount.add(Float.parseFloat(amountlatlon[0]));
            float lat = Float.parseFloat(amountlatlon[1]);
            float lon = Float.parseFloat(amountlatlon[2]);
            coords.add(new Point2D.Float(lat,lon));  // x=lat, y=lon
        }
        Route route = new Route(passengerCount,meterIncrement,meterAmount,coords);
        return route;
    }

    void load(String filename) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(getClass().getClassLoader().getResource(filename).getFile()));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                routes.add(parseRawRouteText(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }
    
    
    public static void main(String... args) throws FileNotFoundException, IOException {
        RouteLoader rl = new RouteLoader();
        rl.load("coords_00.txt");
        System.out.println(rl.getNumRoutes());
        
        
        
        float test = 123.456f;
        System.out.println(test);
        test = 173.4567f;
        System.out.println(test);
        test = 173.45670f;
        System.out.println(test);
        test = 173.45671f;
        System.out.println(test);
        test = 173.45672f;
        System.out.println(test);
        test = 173.45673f;
        System.out.println(test);
        test = 173.45674f;
        System.out.println(test);
        test = 173.45675f;
        System.out.println(test);
        test = 173.45676f;
        System.out.println(test);
        test = 173.45677f;
        System.out.println(test);
        test = 173.45678f;
        System.out.println(test);
        test = 173.45679f;
        System.out.println(test);
        
    }

}
