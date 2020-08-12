package com.solace.demo.taxi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProducerEventHandler;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.ProducerEventArgs;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public enum GpsGenerator {

    INSTANCE;
    
    public final static int GPS_UPDATE_RATE_MS = 10000;  // every 10 seconds
    public final static String COORDS_FILENAME = "config/coords_00.txt";
    
    private String host;
    private String vpn;
    private String user;
    private String pw;

    JCSMPSession session = null;
    XMLMessageProducer producer = null;
    XMLMessageConsumer consumer = null;
    volatile boolean connected = false;
    
    private final ScheduledExecutorService service;
    private final Map<Ride,ScheduledFuture<?>> rides = new HashMap<>();
    
    private static final Logger logger = LogManager.getLogger(GpsGenerator.class);
    
    private GpsGenerator() {
        //service = Executors.newScheduledThreadPool(20);
        ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(20);
        service.setRemoveOnCancelPolicy(true);
        service.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.service = service;
    }
    
    public void initializeSingletonBroadcaster(String host, String vpn, String user, String pw) { 
        INSTANCE.host = host;
        INSTANCE.vpn = vpn;
        INSTANCE.user = user;
        INSTANCE.pw = pw;
    }
    
    void sendMessage(BytesXMLMessage message, String topic) {
        try {
            //Topic destination = JCSMPFactory.onlyInstance().createTopic(topic);
            if (connected) {
                logger.debug("sending to "+topic+" -- "+message.dump());
                producer.send(message,JCSMPFactory.onlyInstance().createTopic(topic));
            }
            else {
//                    System.out.println("not connected, can't send");
            }
        } catch (JCSMPException e) {
            System.out.println("Had an issue sending a message");
        }
    }
    
    
    
    /////////////////////////////////// USER INTERACTION PARTS

    void loadRoutes() throws FileNotFoundException, IOException {
        RouteLoader.INSTANCE.load(COORDS_FILENAME);
    }
    
    void createDrivers(int number) {
        // todo later
    }

    void createPassengers(int number) {
        // todo later
    }
    
    void createInitialRides(int number) {
        for (int i=0;i<number;i++) {
            Ride ride = Ride.randomRide();
            ScheduledFuture<?> future = service.scheduleAtFixedRate(ride,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
            rides.put(ride,future);
            logger.info("Created "+ride);
        }
    }
    
    void removeRide(Ride ride) {
        rides.get(ride).cancel(false);  // cancel the future to stop it from reoccuring
        rides.remove(ride);  // remove it from the list
    }

    void addNewRide() {
        Ride ride = Ride.newRide();
        ScheduledFuture<?> future = service.scheduleAtFixedRate(ride,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
        rides.put(ride,future);
    }
    
    /*
    void addRandomBus(int number) {
        for (int i=0;i<number;i++) {
            Bus bus = busTracker.addRandomBus();
            service.scheduleAtFixedRate(bus,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
//            System.out.println("added a bus");
        }
    }
    
    void addBues() {
        busTracker.initBuses();
        for (Bus bus : busTracker.buses) {
            service.scheduleAtFixedRate(bus,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
        }
        System.out.println(busTracker.buses.size()+" buses added.");
    }
    
    void addRandomTaxi(int number) {
        for (int i=0;i<number;i++) {
            Taxi taxi = taxiTracker.addRandomTaxi();
            service.scheduleAtFixedRate(taxi,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
//            System.out.println("added a taxi");
        }
    }
*/    

    void run() throws JCSMPException {
        System.out.println("About to create GpsGenerator session.");
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST,host);
        properties.setProperty(JCSMPProperties.VPN_NAME,vpn);
        properties.setProperty(JCSMPProperties.USERNAME,user);
        properties.setProperty(JCSMPProperties.PASSWORD,pw);
        JCSMPChannelProperties cp = new JCSMPChannelProperties();
        cp.setReconnectRetries(-1);
        //cp.setCompressionLevel(9);  // disable if not using compressed port
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES,cp);
        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        session.setProperty(JCSMPProperties.CLIENT_NAME,"TaxiPub_"+session.getProperty(JCSMPProperties.CLIENT_NAME));
        try {
            try {
                producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
                    
                    @Override
                    public void responseReceived(String messageID) {
                        // never called, Ex version instead
                    }
                    
                    @Override
                    public void handleError(String messageID, JCSMPException cause, long timestamp) {
                        // never called, Ex version instead
                    }

                    @Override
                    public void responseReceivedEx(Object key) {
                        // won't since we're only sending Direct messages
                        System.out.println("prodcer response received");
                    }

                    @Override
                    public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
                        // shouldn't get a publisher error (NACK), but may have exceptions thrown for connectivity
                        System.err.println("Producer handleError() got something: "+cause.toString());
                    }

                }, new JCSMPProducerEventHandler() {
                    @Override
                    public void handleEvent(ProducerEventArgs event) {
                        System.err.println("Producer EVENT handler got something: "+event.toString());
                        
                    }
                });
                consumer = session.getMessageConsumer(new JCSMPReconnectEventHandler() {
                    
                    @Override
                    public boolean preReconnect() throws JCSMPException {
                        System.out.println("RECONNECTING...");
                        connected = false;
                        return true;
                    }
                    
                    @Override
                    public void postReconnect() throws JCSMPException {
                        System.out.println("RECONNECTED!");
                        connected = true;
                    }
                }, new XMLMessageListener() {
                    @Override
                    public void onReceive(BytesXMLMessage message) {
                        System.out.println("HERE");
                        System.out.println(message.dump());
                        /* this is where you will add a switch/case statement to call various methods to
                         *  a) add n more random cars
                         *  b) add a user car
                         *  c) fault a user car
                         *  etc.
                         *      geo/bus/vehNum/lat/lon/routeNum/status
                         *      geo/taxi/vehNum/lat/lon/status
                         *      geo/train/vehNum/lat/lon/routeNum
                                comms/bus/1234
                                comms/route/012
                                comms/broadcast
                                comms/dispatch
                                ctrl/bus/vehNum/stop
                                ctrl/bus/vehNum/start
                                ctrl/bus/vehNum/flat
                                ctrl/bus/vehNum/crash
                                ctrl/bus/vehNum/fix
                         */
                        String topic = message.getDestination().getName();
                        
                        System.out.println(topic);
                        
                        if (topic.startsWith("taxinyc/ops/ride/called/v1/")) {
                            // someone is calling a ride
                        } else if (topic.startsWith(".../")) {
                            
                        }
                        ///taxinyc/ops/ride/updated/v1/${ride_status}/${driver_id}/${rider_id}/${current_longitude}/${current_latitude}

                    }
                    
                    @Override
                    public void onException(JCSMPException exception) {
                        System.out.println("EXCEPTION");
                        // won't happen as all Direct messages
                    }
                });
                System.out.println("Sending \"GPS\" data now...");
                connected = true;
                session.addSubscription(JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/called/>"),true);
                consumer.start();
            } catch (JCSMPException e) {
                // should only throw an exception during start up (from this thread)... otherwise the exception
                // will be thrown and caught by other processes
                System.err.println("GpsGenerator: I have had an exception thrown to be at the outside");
                System.err.println(Thread.currentThread().getName());
                e.printStackTrace();
            }
            while (true) {
                Thread.sleep(100000);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted... terminating");
            // ready to fall out
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }


    public static void main(String... args) throws JCSMPException, IOException {
        if (args.length < 3) {
            System.out.println("Not enough args");
            System.out.println("Usage: GpsGenerator <IP or hostname> <vpn> <user> [password]");
            System.exit(-1);
        }
        String host = args[0];
        String vpn = args[1];
        String user = args[2];
        String pw = args.length > 3 ? args[3] : "";

        INSTANCE.initializeSingletonBroadcaster(host, vpn, user, pw);
        INSTANCE.loadRoutes();
        INSTANCE.createInitialRides(50);
        INSTANCE.run();
    }
}
