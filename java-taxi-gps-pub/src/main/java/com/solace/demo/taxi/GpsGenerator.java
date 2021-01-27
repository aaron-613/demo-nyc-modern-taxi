package com.solace.demo.taxi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ClientName;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProducerEventHandler;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.ProducerEventArgs;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public enum GpsGenerator {

    INSTANCE;
    
    public final static int GPS_UPDATE_RATE_MS = 5000;
    public final static String COORDS_FILENAME = "config/coords_00.txt";
    public final static Charset UTF_8 = Charset.forName("utf-8");
    
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
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        };
        service.setThreadFactory(factory);
        this.service = service;
    }
    
    public void initializeSingletonBroadcaster(String host, String vpn, String user, String pw) { 
        INSTANCE.host = host;
        INSTANCE.vpn = vpn;
        INSTANCE.user = user;
        INSTANCE.pw = pw;
    }
    
    /**
     * Very basic bare-bones publisher utility method
     */
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
            logger.warn("Had an issue sending a message",e);
            logger.warn(message);
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

    void addNewRide(Ride ride) {
        // the ride will start in 5 to 15 seconds
        ScheduledFuture<?> future = service.scheduleAtFixedRate(ride,(long)(Math.random()*10000) + 5000,GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
        rides.put(ride,future);
        // send out the ride request!
        service.execute(new Runnable() {
            @Override
            public void run() {
                ride.makeRideCalled();  // will publish a message, so this method (ideally) shouldn't be called from an API-owned thread
            }
        });
    }
    

    void run() throws JCSMPException {
        System.out.println("About to create GpsGenerator session.");
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST,host);
        properties.setProperty(JCSMPProperties.VPN_NAME,vpn);
        properties.setProperty(JCSMPProperties.USERNAME,user);
        properties.setProperty(JCSMPProperties.PASSWORD,pw);
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // re-subscribe Direct subs after reconnect
        JCSMPChannelProperties cp = new JCSMPChannelProperties();
        cp.setReconnectRetries(-1);
        //cp.setCompressionLevel(9);  // disable if not using compressed port
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES,cp);
        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();  // will throw a checked exception here if it can't connect
        session.setProperty(JCSMPProperties.CLIENT_NAME,"TaxiPub_"+session.getProperty(JCSMPProperties.CLIENT_NAME));
        try {
            try {
                producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
                    
                    @SuppressWarnings("deprecation")
                    @Override
                    public void responseReceived(String messageID) {
                        // never called, Ex version instead
                    }
                    
                    @SuppressWarnings("deprecation")
                    @Override
                    public void handleError(String messageID, JCSMPException cause, long timestamp) {
                        // never called, Ex version instead
                    }

                    @Override
                    public void responseReceivedEx(Object key) {
                        // won't since we're only sending Direct messages
                        logger.info("prodcer response received");
                    }

                    @Override
                    public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
                        // shouldn't get a publisher error (NACK), but may have exceptions thrown for connectivity
                        logger.warn("Producer handleError() got something: "+cause.toString());
                    }

                }, new JCSMPProducerEventHandler() {
                    @Override
                    public void handleEvent(ProducerEventArgs event) {
                        logger.warn("Producer EVENT handler got something: "+event.toString());
                        
                    }
                });
                consumer = session.getMessageConsumer(new JCSMPReconnectEventHandler() {
                    
                    @Override
                    public boolean preReconnect() throws JCSMPException {
                        logger.info("RECONNECTING...");
                        connected = false;
                        return true;
                    }
                    
                    @Override
                    public void postReconnect() throws JCSMPException {
                        logger.info("RECONNECTED!");
                        connected = true;
                    }
                }, new XMLMessageListener() {
                    @Override
                    public void onReceive(BytesXMLMessage message) {
                        //System.out.println(topic);
                        //System.out.println(message.dump());
                        String topic = message.getDestination().getName();

                        // taxinyc/ops/ride/called/v1/${car_class}/${passenger_id}/${pick_up_longitude}/${pick_up_latitude}
                        if (topic.startsWith("taxinyc/ops/ride/called/v1/")) {
                            // someone is calling a ride
                            logger.info("Received Ride request: "+topic);
                            
                        } else if (topic.startsWith("taxinyc/ops/ride/called/demo")) {
                            logger.info("Received HUMAN Ride request: "+topic);
                            String payload;
                            if (message instanceof TextMessage) {
                                payload = ((TextMessage)message).getText();
                            } else {  // assume it's binary!
                                try {
                                    payload = new String(((BytesMessage)message).getData(),UTF_8);
                                } catch (RuntimeException e) {
                                    logger.warn("Could not decode payload of Human rider message: "+message.dump());
                                    return;
                                }
                            }
                            logger.info(payload);  // hopefully it's JSON
                            try {
                                JsonReader reader = Json.createReader(new StringReader(payload));
                                JsonObject msgJsonObject = reader.readObject();
                                if (!msgJsonObject.containsKey("solClientName")) {
                                    logger.warn("Could not find Client Name in JSON object");
                                    return;
                                }
                                String solClientName = msgJsonObject.getString("solClientName");
                                String name;
                                if (msgJsonObject.containsKey("name")) {
                                    name = msgJsonObject.getString("name");
                                } else {
                                    name = Names.randomFirstName() + " " + Names.randomLastName();
                                }
                                Passenger p = Passenger.newPassenger(name);
//                                ClientName clientName = JCSMPFactory.onlyInstance().createClientName(origMsg.getSenderId());
                                ClientName clientName = JCSMPFactory.onlyInstance().createClientName(solClientName);

                                Topic routeSub;  // the subscription that receivers all the route information (accepted, pickup, enroute, dropoff)
                                Topic reqSub;    // the sub that just receives the generated ride request message
                                if (solClientName.startsWith("#mqtt/")) {
                                    // need to use the SMF equivalent for "#"
                                    // https://docs.solace.com/Open-APIs-Protocols/MQTT/MQTT-Topics.htm#Using
                                    routeSub = JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/*/*/"+p.getId()+"/"+(char)3);
                                    // taxinyc/ops/ride/called/v1/${car_class}/${passenger_id}/${pick_up_longitude}/${pick_up_latitude}
                                    reqSub = JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/called/v1/*/"+p.getId()+"/"+(char)3);
                                } else {
                                    // ">" wildcard only works for SMF clients
                                    routeSub = JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/*/*/"+p.getId()+"/>");
                                    reqSub = JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/called/v1/*/"+p.getId()+"/>");
                                }
                                try {
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/a/b/"+p.getId()),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/*/*/"+p.getId()),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/a/b/"+p.getId()+"/abc"),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/a/b/"+p.getId()+"#"),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/+/+/"+p.getId()+"#"),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/*/*/"+p.getId()+"#"),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/*/*/"+p.getId()+(char)3),JCSMPSession.WAIT_FOR_CONFIRM);
                                    //session.addSubscription(clientName,JCSMPFactory.onlyInstance().createTopic("taxinyc/ops/ride/updated/v1/*/*/"+p.getId()+"/"+(char)3),JCSMPSession.WAIT_FOR_CONFIRM);
				    // this adds subscriptoins to the requesting client connection
                                    session.addSubscription(clientName,routeSub,JCSMPSession.WAIT_FOR_CONFIRM);
                                    session.addSubscription(clientName,reqSub,JCSMPSession.WAIT_FOR_CONFIRM);
                                } catch (JCSMPException e) {
                                    logger.warn("COuld not add a sub for "+clientName,e);
                                    return;  // don't add a new ride for a client that doesn't exist!
                                }
                                Ride requestedRide = Ride.newRide(p);
                                addNewRide(requestedRide);
                            } catch (JsonException e) {
                                logger.warn("COuldn't parse JSON");
                            }
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
        
        String name = "aaron   ";
        System.out.println(name.split(" ",3).length);
        
        
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
        INSTANCE.createInitialRides(100);
        INSTANCE.run();
    }
}
