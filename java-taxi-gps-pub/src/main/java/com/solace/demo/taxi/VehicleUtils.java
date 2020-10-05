package com.solace.demo.taxi;

import java.awt.geom.Point2D;

public class VehicleUtils {

    
//    public static int calcHeading(int route, int pos) {
//        return calcHeading(route,pos);
//    }
    
    public static int calcHeading(int route, int pos) {
        if (pos > 0) {  // use the previous point to calculate heading
            return heading(RouteLoader.INSTANCE.getCoord(route,pos-1),RouteLoader.INSTANCE.getCoord(route,pos));
        } else {  // starting point, use the next one rather than the last... assumes length of coords > 1
            return heading(RouteLoader.INSTANCE.getCoord(route,pos),RouteLoader.INSTANCE.getCoord(route,pos+1));
        }
    }

    /**
     * Calculate the heading between 2 points.  Assumes that north is 0 degrees, and the coordinates are lat/lon (i.e. x=lon, y=lat)<p>
     * https://math.stackexchange.com/questions/1596513/find-the-bearing-angle-between-two-points-in-a-2d-space
     * @param previous Point2D.Float lon,lat (my previous point)
     * @param current Point2D.Float lon,lat (my current point)
     * @return a degree integer heading 0-359, 0=north, from p1 to p2
     */
    public static int heading(Point2D.Float previous, Point2D.Float current) {
        float xd = current.x - previous.x;
        float yd = current.y - previous.y;
        double theta = Math.atan2(xd,yd);
        int head = (int)Math.round((theta/Math.PI)*180);
        if (head < 0) head += 360;
        //System.out.printf("dLat=%f, dLon=%f, head=%d%n",p2.x-p1.x,p2.y-p1.y,head);
        return head;
    }
    
    public static int calcSpeed(int route, int pos) {
        if (pos > 0) {  // use the previous point to calculate heading
            return speed(RouteLoader.INSTANCE.getCoord(route,pos-1),RouteLoader.INSTANCE.getCoord(route,pos));
        } else {  // starting point, use the next one rather than the last
            return speed(RouteLoader.INSTANCE.getCoord(route,pos),RouteLoader.INSTANCE.getCoord(route,pos+1));
        }
    }


    public static int speed(Point2D.Float previous, Point2D.Float current) {
        // since 1 degree lat ~= 111km, then 111 * 1/3600 == how many km you drive per degree second
        return (int)Math.round(Point2D.Float.distance(previous.y,current.x,current.y,current.x) * 200000);  // rougly km/h at 2 second updates
    }
    

}
