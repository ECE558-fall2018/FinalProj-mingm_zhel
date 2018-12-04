package edu.pdx.android.ece558finalproject.mingm.distance_sensor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


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

    private static final String TAG = DistanceSensor.class.getSimpleName();

    //Variable for Distance Sensor
    private static final String ECHO_PIN = "BCM17";
    private static final String TRIGGER_PIN = "BCM4";
    private Gpio mEcho;
    private Gpio mTrigger;
    private String mDistance;

    //Variable for LCD display
    private static final String GPIO_LCD_RS = "BCM26";
    private static final String GPIO_LCD_EN = "BCM19";
    private static final String GPIO_LCD_D4 = "BCM21";
    private static final String GPIO_LCD_D5 = "BCM20";
    private static final String GPIO_LCD_D6 = "BCM16";
    private static final String GPIO_LCD_D7 = "BCM12";
    private Lcd1602 lcd;

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

        Thread thread = new Thread(calc_distance);
        thread.start();

        }

    protected void readDistance() throws IOException, InterruptedException{
        //Make sure trigger is false at first
        mTrigger.setValue(false);
        Thread.sleep(0, 2000);
        //Hold the trigger High for at least 10 us
        mTrigger.setValue(true);
        Thread.sleep(0, 10000); //10 us
        mTrigger.setValue(false);
        int mReceiving;
        //Reset the trigger pin
        while(mEcho.getValue() == false){
             mReceiving = 0; //make this while loop keep working
        }
        long mTimeStart = System.nanoTime();
        Log.i(TAG, "Echo Arrived.");

        //Wait for the end of the pulse on the ECHO pin
        while(mEcho.getValue() == true){
            mReceiving = 1;
        }
        long mTimeEnd = System.nanoTime();
        Log.i(TAG, "Echo Ended.");

        long pulseWidth = mTimeEnd - mTimeStart;
        //Assume the speed of sound in air is about 340m/s
        Double Distance = ((pulseWidth / 1000000000.0) * 340.0 / 2.0) * 100.0;

        if(Distance <= 50.00){
            DecimalFormat df = new DecimalFormat("#.00");
            String formatedDistance = df.format(Distance);
            mDistance = formatedDistance;

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference distanceRef = database.getReference("Distance");
            distanceRef.setValue(mDistance);
        }
        Log.i(TAG, "Distance: " + mDistance + "cm");
        //System.out.println("Calculated Distance: " + mDistance);
    }

    private Runnable calc_distance = new Runnable() {
        @Override
        public void run() {
            try{
                lcd = new Lcd1602(GPIO_LCD_RS, GPIO_LCD_EN, GPIO_LCD_D4, GPIO_LCD_D5, GPIO_LCD_D6, GPIO_LCD_D7);
                lcd.begin(16, 2);

                while(true){
                    readDistance();
                    Thread.sleep(1500);
                    lcd.clear();
                    lcd.print("Distance:" + mDistance + "cm");
                    lcd.setCursor(0, 1);
                    lcd.print(getCurrentTime());
                }
            } catch (IOException e){
                e.printStackTrace();
            } catch (InterruptedException i){
                i.printStackTrace();
            }
        }
    };

    protected void onDestroy() {
        super.onDestroy();

        if (lcd != null) {
            try {
                lcd.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing", e);
            } finally {
                lcd = null;
            }
        }
    }

    public String getCurrentTime(){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm");
        date.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        String localTime = date.format(currentLocalTime);
        return localTime;
    }

}
