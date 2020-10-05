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

- `GpsGenerator` is main file, loads in route data, connects to Solace
- Currently hardcoded for 100 simutaneous routes at updates every 5 seconds
   - ~ 20 msg/s
- Currently using approximately 1400 fixed routes from Google PubSub taxi route dump
- Currently randomizing Driver and Passenger details
   - Will be updating Drivers to use Microsoft Azure DB soon

## Misc

We're going to use singe-precision (`float` not `double`) to store internally the coordinates for the routes:
https://sites.google.com/site/trescopter/Home/concepts/required-precision-for-gps-calculations

Althgouh using `double` values for things like Ratings and Fare Amount, as JSON library does strange rounding/floating point stuff when using only `float`.  E.g. a rating of 3.27 becomes 3.26984719472374912 in the JSON message if using floats.
