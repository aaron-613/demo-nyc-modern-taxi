# demo-nyc-modern-taxi

Current (temporary) location: https://sg.solace.com/taxi/

This demo showcases many aspects of the Solace PubSub+ Platform, including but not limited to:

 - Solace PubSub+ event broker communication
 - Multi-Cloud deployment, currently running inside Azure and AWS
 - Multi-protocol support, with Java SMF applications and MQTT WebSocket applications
 - Integration with Kafka via Connectors
 - Integration with SalesForce via Boomi

This demo has several parts.  They are itemized below.


## java-taxi-gps-pub

The "backend" publisher that is generating all of the taxi GPS positions.  This data is based on the data of taxi rides release by NYC in 2019 (provide links).  The application uses Solace JCSMP API for communication over Solace.

The publisher also listens to "ride called" requests, and issues new Rides. This capability is provided by the Subscription Manager feature of Solace PubSub+.



## js-mqtt-map

A simple JavaScript map application using Google Maps to visualize where all of the various taxis are. Uses Eclipse Paho MQTT libraries for communication over Solace.

![Screenshot](https://github.com/aaron-613/demo-nyc-modern-taxi/blob/master/map.jpg "Screenshott")

## js-mqtt-client

A very basic "bare bones" MQTT JavaScript appliation that a user can "summon" or request rides from the backend GPS publisher.




