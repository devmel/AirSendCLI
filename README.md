# AirSendCLI
This application controls an AirSend device :
- temperature and illuminance sensors
- send/receive 433Mhz raw data or packets

Compatibility :
- AirSend

# Example
Turn on A1 remote switch and display sensors values : 
- java -jar AirSendCLI.jar -s -wp PT2262:1980:1

Turn off A1 remote switch : 
- java -jar AirSendCLI.jar -wp PT2262:1980:0

Nice device : 
- java -jar AirSendCLI.jar -wp FLOR:19520:1
19520 is the address, it can be found in mobile app (shared command only)
1 is the button it can be between 1 and 16

# Library
- Devmel from https://devmel.com/
- Webcam Capture from http://webcam-capture.sarxos.pl/
- Zxing from http://github.com/zxing/zxing/
- Log4j from http://logging.apache.org/log4j/

# Source
Java source licensed under the Apache License, Version 2.0
