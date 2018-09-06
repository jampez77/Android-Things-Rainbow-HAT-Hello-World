package com.n8.hellorainbowhat

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import java.io.IOException
import java.util.*


private const val BLINK_DURATION_MILLIS = 1000L

class MainActivity : Activity() {

    private val TAG = MainActivity::class.java.simpleName

    private val mRainbow = IntArray(7)
    private val LEDSTRIP_BRIGHTNESS = 1
    private var mLedstrip: Apa102? = null
    private var mDisplay: AlphanumericDisplay? = null
    private lateinit var buttonAInputDriver: ButtonInputDriver
    private lateinit var buttonBInputDriver: ButtonInputDriver
    private lateinit var ledRedGpio: Gpio
    private lateinit var ledGreenGpio: Gpio
    private val I2C1 = "I2C1"
    private val APA102 = "SPI0.0"
    private val buttonA = "BCM21"
    private val buttonB = "BCM20"
    private val buttonC = "BCM16"

    private val ledRed = "BCM6"
    private val ledGreen = "BCM19"
    private val ledBlue = "BCM26"

    private val MOSI = "BCM10"

    private val message = "HOLD DOWN B FOR TEMP"
    private val scrollSpeed :Long = 250


    private var background = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // Turn on the LED
            setLedValue(true, keyCode)
            return true
        } else if(keyCode == KeyEvent.KEYCODE_0){
            background.interrupt()
            setLedValue(true, keyCode)
            //Show room temp
            val sensor = RainbowHat.openSensor()
            sensor.temperatureOversampling = Bmx280.OVERSAMPLING_1X
            mDisplay?.display(sensor.readTemperature().toDouble())
            // Close the devices when done (onDestroy)
            sensor.close()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // Turn off the LED
            setLedValue(false, keyCode)
            finish()
            return true
        } else if(keyCode == KeyEvent.KEYCODE_0){
            setLedValue(false, keyCode)
            displayMessage(message, true, scrollSpeed)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Update the value of the LED output.
     */
    private fun setLedValue(value: Boolean, keyCode: Int) {
        Log.d(TAG, "Setting LED value to $value")
        when(keyCode){
            KeyEvent.KEYCODE_SPACE -> ledRedGpio.value = value
            KeyEvent.KEYCODE_0 -> ledGreenGpio.value = value
        }

    }

    private fun displayMessage(message: String, scroll: Boolean, scrollSpeed: Long) {

        background = object:Thread(){
            override fun run() {
                try {
                    if(scroll) {
                        while(scroll) {
                            val prefix = "    "
                            val stringBuilder = StringBuilder()
                            stringBuilder.append(prefix).append(message)
                            val prefixedMsg = stringBuilder.toString()

                            val chars = prefixedMsg.toCharArray()
                            var i = 0
                            val n = chars.size
                            while (i < n) {
                                System.out.print("\r" + prefixedMsg)

                                Thread.sleep(scrollSpeed)
                                mDisplay?.display(prefixedMsg.substring(i) + prefixedMsg.substring(0, 0))
                                i++
                            }
                            mDisplay?.clear()
                        }

                    }else {
                        mDisplay?.display(message)
                    }
                }catch (e: InterruptedException){
                    //e.printStackTrace()
                }

            }
        }
        background.start()

    }

    override fun onStart() {
        super.onStart()
        startProgram()
    }


    override fun onStop() {
        super.onStop()
        endProgram()
    }

    private fun startProgram(){
        val pioService = PeripheralManager.getInstance()

        /*mosiGpio = pioService.openGpio(MOSI)
        mosiGpio.setEdgeTriggerType(Gpio.EDGE_NONE) // reset for Android Things bug
        mosiGpio.setDirection(Gpio.DIRECTION_IN)
        mosiGpio.setActiveType(Gpio.ACTIVE_HIGH)
        mosiGpio.setEdgeTriggerType(Gpio.EDGE_FALLING)
        mosiGpio.registerGpioCallback(object : GpioCallback {
            override fun onGpioEdge(gpio: Gpio?): Boolean {
                Log.d(TAG, (gpio?.value  ?: "null").toString())
                return true
            }
            override fun onGpioError(gpio: Gpio?, error: Int) {
                Log.d(TAG, "GPIO $gpio Error event $error")
            }
        })*/
        ledRedGpio = pioService.openGpio(ledRed)
        ledRedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        ledGreenGpio = pioService.openGpio(ledGreen)
        ledGreenGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        buttonAInputDriver = ButtonInputDriver(buttonA, Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_SPACE)
        buttonAInputDriver.register()

        buttonBInputDriver = ButtonInputDriver(buttonB, Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_0)
        buttonBInputDriver.register()


        try {
            mLedstrip = Apa102(APA102, Apa102.Mode.BGR)
            mLedstrip?.brightness = LEDSTRIP_BRIGHTNESS
            for (i in 0 until mRainbow.size) {
                val hsv = floatArrayOf(i * 360f / mRainbow.size, 1.0f, 1.0f)
                mRainbow[i] = Color.HSVToColor(255, hsv)
            }
            val colors = IntArray(mRainbow.size)
            Arrays.fill(colors, Color.RED)
            mLedstrip?.write(colors)
        } catch (e: IOException) {
            mLedstrip = null // Led strip is optional.
        }

        try {
            mDisplay = AlphanumericDisplay(I2C1, Ht16k33.I2C_ADDRESS)
            mDisplay?.setEnabled(true)
            displayMessage(message, true, scrollSpeed)

        } catch (e: IOException) {
            e.printStackTrace()
        }



    }

    private fun endProgram(){
        background.interrupt()

        buttonAInputDriver.unregister()
        buttonAInputDriver.close()

        buttonBInputDriver.unregister()
        buttonBInputDriver.close()

        if (mLedstrip != null) {
            try {
                mLedstrip?.brightness = 0
                mLedstrip?.write(IntArray(7))
                mLedstrip?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error disabling ledstrip", e)
            } finally {
                mLedstrip = null
            }
        }
        // Remove pending blink Runnable from the handler.
        if (mDisplay != null) {
            try {
                mDisplay?.clear()
                mDisplay?.setEnabled(false)
                mDisplay?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error disabling display", e)
            } finally {
                mDisplay = null
            }
        }

        try{
            ledRedGpio.close()
            ledGreenGpio.close()
        }catch (e: IOException){
            Log.e(TAG, "Error disabling ledRedGpio", e)
        }
    }

}
