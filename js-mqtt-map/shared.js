/*
 This file is used to store the connection credentials, and to initiate an MQTT connection
*/

  var deets = {
    host: 'taxi.messaging.solace.cloud',
    port: 8443,
    ssl: true,
    username: 'public-taxi',
    password: 'iliketaxis'
  }

/*
  // eclipse test server
  var deets = {
    host: 'mqtt.eclipse.org',
    port: 443,
    ssl: true,
    username: '',
    password: '',
    //path: '/test',
  }
*/

  // this is for MQTT, it should return a connected or connecting valid Paho client
  function getClientConnection(uniqueID,onMessageArrived,onConnectionLost,onConnect) {
    var client = new Paho.MQTT.Client(deets['host'], Number(deets['port']), uniqueID); // AWS SGP Nano
    // set the callback handlers
    client.onConnectionLost = onConnectionLost;
    client.onMessageArrived = onMessageArrived;
    // define connection options
    var connectOptions = {};
    if (deets['ssl'] == true) {
      connectOptions["useSSL"] = true;
    } else {
      connectOptions["useSSL"] = false;
    }
    //connectOptions["reconnect"] = true;
    connectOptions["userName"] = deets['username'];
    connectOptions["password"] = deets['password'];  // AWS SGP Nano
    connectOptions["onSuccess"] = onConnect;
    // try to connect!
    client.connect(connectOptions);
    return client;
  }
