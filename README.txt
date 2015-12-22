#TrainSoftware

Software designed to control the pcDuino as a model railroad control board with a Seeed Motor shield with the track connected on the motor 1 output and a dual coil switch on the second motor output. 12 volts is applied to the VIN of the shield and the pcDuino is off of it's own USB Micro connector with a 5v 2A power supply.

The usage is as follows (after you compile a jar or class file):

java [-jar TrainSoftware.jar] (port number)

The default port number is 80, so you will need to have root access if you don't add an argument to change the port number to a number above 1024. A good port to run it on above this is 8080 
