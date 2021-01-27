# js-mqtt-client

This little client application is used to simulate a person requesting a taxi. It initiates an MQTT connection to the broker, and then send a single message on a "well-known topic" (WKT) that the backend GPS / ride hailing service is listening to.  The GPS publisher will add specific topics to the client, specifically called subscribing On Behalf Of (OBO). Then the GPS publisher will start a new route/ride for the customer.

Since this is just a simple app, it only displays the payload. Maybe in the future it will show graphical view.


