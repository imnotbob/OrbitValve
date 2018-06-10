# OrbitValve
Iris Orbit Zigbee faucet valve
for SmartThings

Orbit Model HT8-ZB

Installation if running prior to Smarthings hub firwmare 22.8,  you should set the time on the orbit device prior to including it in Smartthings.   Firmware 22.8 and later should set the time for you.

This device handler should be installed via the IDE in "My Device Handlers", "save", and "publish for me".

This device automatically shuts off after 10 minutes.  If using webcore, you send another on command prior to 10 minutes it will reset this timer for another 10 mintues

This device supports switch (on/off) and valve (open / closed) device capabilities

In the ST app, you can turn it on/off (on defaults to 10mins)

You can set longer time in the ST device.

There is an API runfor(minutes) that you can use in core to run for longer periods of time without having to code for the 10 minute cycle in the device
