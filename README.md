# demo-nyc-modern-taxi-pub

## To build / run

1. Clone the project
2. `./gradlew clean assemble` on Linux/Mac terminal, or `.\gradlew.bat clean assemble` on Windows Command Prompt
1. `cd build`
1. `cd distributions`
1. Unzip or UnTar (`tar -xvf`) demo-nyc-modern-taxi-pub archive
1. `cd demo-nyc-modern-taxi-pub`
1. `bin/demo-nyc-modern-taxi-pub <localhost> <vpnName> <username> [password]`

## Notes

- `GpsGenerator` is main file, loads in route data, connects to Solace
- Currently hardcoded for 100 simutaneous routes at updates every 2 seconds
   - ~ 50 msg/s
- Currently using approximately 1400 fixed routes from Google PubSub taxi route dump
- Currently randomizing Driver details (Driver's can be driving multiple rides), and Passengers aren't done yet

## Misc

We're going to use singe-precision (`float` not `double`) to store internally the coordinates for the routes:
https://sites.google.com/site/trescopter/Home/concepts/required-precision-for-gps-calculations

