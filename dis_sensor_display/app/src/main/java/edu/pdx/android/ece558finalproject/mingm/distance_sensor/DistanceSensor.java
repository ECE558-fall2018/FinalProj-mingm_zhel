package edu.pdx.android.ece558finalproject.mingm.distance_sensor;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.print.PrinterId;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class DistanceSensor extends Activity {

    private static final String ECHO_PIN = "BCM20";
    private static final String TRIGGER_PIN = "BCM21";
    private Gpio mEcho;
    private Gpio mTrigger;
    private double mDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_sensor);

        PeripheralManager service = PeripheralManager.getInstance();
        Log.d(TAG, "Available GPIOs: " + service.getGpioList());

        try{
            //Create GPIO connection for Echo
            mEcho = service.openGpio(ECHO_PIN);
            //Set mEcho as input
            mEcho.setDirection(Gpio.DIRECTION_IN);
            //Enable edge trigger events
            mEcho.setEdgeTriggerType(Gpio.EDGE_BOTH);
            //Set Active type as high, high will be considered as true
            mEcho.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e){
            Log.e(TAG, "Error on Peripheral Input API", e);
            e.printStackTrace();
        }

        try{
            //Create GPIO connection for Trigger
            mTrigger = service.openGpio(TRIGGER_PIN);
            //Configure Trigger as output with default LOW(False) value
            mTrigger.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG,"Error on Peripheral Output API", e);
            e.printStackTrace();
        }

        new Thread(){
            @Override
            public void run(){
                try{
                    while(true){
                        readDistance();
                        Thread.sleep(300);
                    }
                } catch (IOException e){
                    e.printStackTrace();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                }
            }.start();
        }

    protected void readDistance() throws IOException, InterruptedException{
        //Make sure trigger is false at first
        mTrigger.setValue(false);
        Thread.sleep(0, 2000);
        //Hold the trigger High for at least 10 us
        mTrigger.setValue(true);
        Thread.sleep(0, 10000); //10 us
        int mReceiving;
        //Reset the trigger pin
        while(!mEcho.getValue()){
             mReceiving = 0; //make this while loop keep working
        }
        long mTimeStart = System.nanoTime();
        Log.i(TAG, "Echo Arrived.");

        //Wait for the end of the pulse on the ECHO pin
        while(mEcho.getValue()){
            mReceiving = 1;
        }
        long mTimeEnd = System.nanoTime();
        Log.i(TAG, "Echo Ended.");

        long pulseWidth = mTimeEnd - mTimeStart;
        //Assume the speed of sound in air is about 340m/s
        mDistance = ((pulseWidth / 1000000000.0) * 340.0 / 2.0) * 100.0;
        Log.i(TAG, "Distance: " + mDistance + "cm");
    }

}
