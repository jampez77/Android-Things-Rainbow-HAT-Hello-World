##Hello World Rainbow HAT

This is a really quick project I put together to help me get to know the Android Things platform.

Setting up Android Things was pretty painless and the [official documentation](https://developer.android.com/things/hardware/raspberrypi) is more than sufficient to get started with.

##Features
- Scrolling text on the Alphanumeric segment display (I2C1).
- Key down on button A (BCM21) will light up the red LED.
- Key up on button A will close the app and stop all activity.
- Key down on button B (BCM20) will display the current temperature.
- Key up on button B will re-initialise the scrolling text.
- All lights on LED strip (APA102) will be red at full brightness.
