package edu.pdx.android.ece558finalproject.mingm.distance_sensor;

import android.app.Activity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.nio.ByteBuffer;

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

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private Camera mCamera;

    private ButtonInputDriver mButtonInputDriver;

    private Handler mCameraHandler;
    private HandlerThread mCameraThread;

    /**
     * A {@link Handler} for running Cloud tasks in the background.
     */
    private Handler mCloudHandler;

    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private HandlerThread mCloudThread;




    //Variable for Distance Sensor
    private static final String ECHO_PIN = "BCM17";
    private static final String TRIGGER_PIN = "BCM4";
    private Gpio mEcho;
    private Gpio mTrigger;
    private String mDistance;
    private int mEnable;


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

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference initialRef = database.getReference("Enable");
        initialRef.setValue("false");

        PeripheralManager service = PeripheralManager.getInstance();
        Log.d(TAG, "Available GPIOs: " + service.getGpioList());

        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission");
            return;
        }

        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());


        // Initialize the doorbell button driver
        initPIO();

        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = Camera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);



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

        FirebaseDatabase database_r = FirebaseDatabase.getInstance();
        final DatabaseReference mRef = database_r.getReference("Enable");
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String ADC5IN = String.valueOf(dataSnapshot.getValue());
                boolean m = Boolean.valueOf(ADC5IN);
                System.out.println("Test for ADC5IN" + ADC5IN);
                if(m == true) {
                    mCamera.takePicture();
                } else {
                    System.out.println("Test for Range");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


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
            DatabaseReference enableRef = database.getReference("Enable");
            if(Distance <= 15.00 && Distance >= 10.00){
                enableRef.setValue("true");
            } else {
                enableRef.setValue("false");
            }
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
                    Thread.sleep(2000);
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

    private void initPIO() {
        try {
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonInputDriver.register();
        } catch (IOException e) {
            mButtonInputDriver = null;
            Log.w(TAG, "Could not open GPIO pins", e);
        }
    }

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
        mCamera.shutDown();

        mCameraThread.quitSafely();
        try {
            mButtonInputDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
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

    /*@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doorbell rang!
            Log.d(TAG, "button pressed");
            mCamera.takePicture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
*/


    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    /**
     * Upload image data to Firebase as a doorbell event.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            final DatabaseReference log = mDatabase.getReference("logs").push();
            final StorageReference imageRef = mStorage.getReference().child(log.getKey());
            // upload image to storage
            UploadTask uploadTask = imageRef.putBytes(imageBytes);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        Log.i(TAG, "Image upload successful");
                        log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                        log.child("image").setValue(downloadUri.toString());
                    } else {
                        // clean up this entry
                        Log.w(TAG, "Unable to upload image to Firebase");
                        log.removeValue();
                    }
                }
            });

        }


    }


}
