import './style.css';
import {Map, View} from 'ol';
import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';

// added by aaron, so it's lat/lon and not metres
import {useGeographic} from 'ol/proj';
useGeographic();

import Feature from 'ol/Feature';
import VectorSource from 'ol/source/Vector';
import {Icon, Style} from 'ol/style';
import {Vector as VectorLayer} from 'ol/layer';
import Point from 'ol/geom/Point';
import Collection from 'ol/Collection';  // 





const iconStyle = new Style({
  image: new Icon({
    anchor: [0.5, 0.5],
    anchorXUnits: 'fraction',
    anchorYUnits: 'fraction', // 'pixels',
    src: './icon.png',
    opacity: 1,
  }),
});

var coll = new Collection();  // needs to be used as an array

//const vectorSource = new VectorSource({
  //features: [iconFeature],
//  features: coll,
//});
const vectorSource = new VectorSource();
//vectorSource.addFeature(iconFeature);


const vectorLayer = new VectorLayer({
  source: vectorSource,
});




const map = new Map({
  target: 'map',
  layers: [
    new TileLayer({
      source: new OSM()
    }),
    vectorLayer
  ],
  view: new View({
    center: [-73.9, 40.7],
    zoom: 12,
    rotation: -0.522,
  })
});













    var vehicles = {};  // keyed by driverID



    // MQTT code //////////////////////////////////////////////////////
  
    function writeConsole(msg) {
      console.log(msg);
    }

    var isConnected = false;
    writeConsole("Connecting...");
    //var uniqueID = "abcdefg";
    var uid = ""+(Math.random() + 1).toString(36).substring(2,8);  // generate a "random" [0-9a-z] 6 char ID
    var mqttClient = getClientConnection(uid,onMessageArrived,onConnectionLost,onConnect);

    // called when the client connects
    function onConnect() {
      writeConsole("Connected!");
      isConnected = true;
      //mqttClient.subscribe("gps/#");
      mqttClient.subscribe("taxinyc/#");
    }

    // called when the client loses its connection
    function onConnectionLost(responseObject) {
      isConnected = false;
      if (responseObject.errorCode !== 0) {
        writeConsole("Connection Lost! " + responseObject.errorMessage);
      }
    }

    // called when a message arrives
    function onMessageArrived(msg) {
      try {
        if (msg.destinationName.indexOf("taxinyc/ops/ride/updated/v1") == 0) {
          parseRideUpdatedMessage(msg);
	//} else if (msg.destinationName.indexOf("taxinyc/ops/ride/called/v1") == 0) {
        //  parseRideCalledMessage(msg);
          //console.log(msg.destinationName);
        }
      } catch (e) {
        console.log("Had an issue with received message: topic="+msg.destinationName+", payload="+msg.payload);
        console.log(e);
      }
    }


    ////////////////////// Callback functions //////////////////////////////////////////////////////////////////////////////

    function buildBusInfoWindowContent(vehNum) {
      var contentString = '<div id="content">';
      var taxi = vehicles[vehNum];
      var payload = taxi.payload;
      //console.log(payload);
      //return;
      contentString = '<h2 id="firstHeading" class="firstHeading">Rider: '+payload.passenger.first_name+' '+payload.passenger.last_name+'</h2>' +
          '<div id="bodyContent"><p>' +
          '<b>Driver:</b> ' + payload.driver.first_name+' '+payload.driver.last_name + 
		              '<br/><b>Meter:</b> $' + payload.meter_reading.toFixed(2) +
          '<br/><b>Current Position:</b> ' + payload.latitude.toFixed(4) + 'N,' + payload.longitude.toFixed(4) + 'W' +
          //'<b>Status:</b> ' + bus["payload"].status +
          '<br/><b>Speed:</b> ' + payload.speed + ' MPH' +
          '<br/><b>Heading:</b> ' + payload.heading + '&deg;'
          '</p>';
      // if (img != null) {
      //   contentString += '<p align="center"><img src="' + img + '"></p>';
      // }
      contentString += '</div></div>';
      return contentString;
    }

    // received when the taxi is moving
    function parseRideUpdatedMessage(msg) {
//      console.log("Received message on: "+msg.destinationName+"  ("+msg.payloadString.length+" bytes)");
//return;
      var levels = msg.destinationName.split('/');
      var payload = JSON.parse(msg.payloadString);
      if (levels[5] != 'enroute') {
        //console.log(levels[5]);
        //console.log(payload);
      }
      //console.log(payload);
      //console.log(Math.floor(payload.heading / 22.5));
      var vehNum = payload.driver.driver_id;
      var taxi;  // for later
      // now, have we seen this guy before?
      if (!(vehNum in vehicles)) {  // keyed by driverID
//console.log("FIRst time seeing "+vehNum);
        vehicles[vehNum] = {}; // create new hash
        taxi = vehicles[vehNum];
        //var pos = new Point([ +payload.longitude, +payload.latitude ]);  //new google.maps.LatLng(lat, lon);
        taxi["marker"] = new Feature({
          //geometry: pos,  // set later
          //name: "hi"+vehNum,
        });
        //taxi.marker.setId(vehNum);
        taxi.marker.setStyle(iconStyle);  // might want to update dynamically later with proper heading
        vectorSource.addFeature(taxi.marker);  // add this feature to my list
//console.log(taxi.marker);

        // if they click the icon, pop up a window
/*        google.maps.event.addListener(taxi.marker, 'click', function() {
          taxi.marker.setZIndex(globalZindex++);  // pop to top
          //buildBusInfoWindowContent(vehNum);
          if (taxi.infoWindow != null) {
            taxi.infoWindow.setZIndex(globalZindex++);
            return;
          }
          // var infoWindowOptions = {
          //   disableAutoPan: true,
          // }
          taxi.infoWindow = new google.maps.InfoWindow({ disableAutoPan: true});
          taxi.infoWindow.setZIndex(globalZindex++);
          taxi.infoWindow.setContent(buildBusInfoWindowContent(vehNum));
          taxi.infoWindow.taxi = taxi;
          taxi.infoWindow.open(map, taxi.marker);
          google.maps.event.addListener(taxi["infoWindow"], 'closeclick', function() {
            taxi.infoWindow = null;
          });
          infoWindows.push(taxi.infoWindow);
          if (infoWindows.length > 5) {
            var oldInfo = infoWindows.shift(1);
            oldInfo.taxi.infoWindow = null;
            oldInfo.close();
          }
        });   */
      }
      // now we've inserted the new guy for sure
      taxi = vehicles[vehNum];
      //lat = lat + taxi["latOff"];
      //lon += taxi["lonOff"];
      var lat1 = payload.latitude;
      var lon1 = payload.longitude;

      taxi["pos"] = new Point([ lon1, lat1 ]);  //new google.maps.LatLng(lat, lon);
      taxi.marker.setGeometry(taxi.pos);

      taxi["payload"] = payload;
      taxi["ts"] = Date.now();
    }  // end parseRideUpdatedMessage

    // make the taxis fade out
    setInterval(function() {
      for (var key in vehicles) {
        if (!vehicles.hasOwnProperty(key)) continue;
        var taxi = vehicles[key];
        var delta = Date.now() - taxi.ts;
        if (delta > 11000) {
          // they have faded away!  Delete 'em!
          //console.log("DELETING taxi "+key);
          vectorSource.removeFeature(taxi.marker);
          delete vehicles[key];
//        } else if (delta > 9000) {
//          taxi.marker.setOpacity(0.33);
//        } else if (delta > 7000) {
//          taxi.marker.setOpacity(0.66);
        }
      }
    }, 1000);



