# java-taxi-gps-pub

## To build / run

1. Clone the whole project
1. `cd java-taxi-gps-pub`
2. `./gradlew clean assemble` on Linux/Mac terminal, or `.\gradlew.bat clean assemble` on Windows Command Prompt
1. `cd build`
1. `cd distribution`
1. Unzip or UnTar `demo-nyc-modern-taxi-gps-pub` archive
1. `cd demo-nyc-modern-taxi-gps-pub`
1. `bin/demo-nyc-modern-taxi-gps-pub <localhost> <vpnName> <username> [password]`

## Notes

- `GpsGenerator` is main file, loads in route data from `src/main/resources/config/`, connects to Solace
- Currently hardcoded for 100 simutaneous routes at updates every 5 seconds
   - ~ 20 msg/s
- Currently using approximately 1400 fixed routes from Google PubSub taxi route dump
   - Have several hundred-thousand routes, so can expand in the future
- Currently generating random Driver at startup and reused throughout, and random Passenger information for every single ride
   - Will be updating Drivers to use Microsoft Azure DB soon

## Capabilities

- Publishes streaming GPS data on Solace (and MQTT) topics that look like: 


