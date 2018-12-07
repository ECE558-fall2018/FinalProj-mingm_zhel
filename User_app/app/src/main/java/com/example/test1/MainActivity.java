package com.example.test1;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private String Distance_In;
    private double Distance;
    private RecyclerView mRecyclerView;
    private ImageEntryAdapter mAdapter;

    private TextView mDistance;
    private Button Delete_Button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDistance = (TextView) findViewById(R.id.Show_Distance);

        FirebaseDatabase database_r = FirebaseDatabase.getInstance();
        final DatabaseReference mRef = database_r.getReference("Distance");
        //listener to listen the distance and use it's value to decide show alert or not
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Distance_In = String.valueOf(dataSnapshot.getValue());
                Distance = Double.valueOf(Distance_In);
                mDistance.setText(String.valueOf(Distance) + "cm");
                // if the distance is in specific range, show the alert.
                if (Distance <= 15.00 && Distance >= 10.00) {
                    // call the module to give the enable to android pi
                    takephoto1();
                } else {
                    //otherwise set the enable to false.
                    FirebaseDatabase database_E = FirebaseDatabase.getInstance();
                    DatabaseReference enableRef_1 = database_E.getReference("Enable");
                    enableRef_1.setValue("false");
                }
            }


        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    });

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("logs");
        mRecyclerView = (RecyclerView) findViewById(R.id.doorbellView);
        // Show most recent items at the top
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        mRecyclerView.setLayoutManager(layoutManager);

        // Initialize RecyclerView adapter
        mAdapter = new ImageEntryAdapter(this, ref);
        mRecyclerView.setAdapter(mAdapter);
        Delete_Button = (Button) findViewById(R.id.button_1);
        //setup the button to delete image both in firebase and recycle view
        Delete_Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                delete_photo();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Initialize Firebase listeners in adapter
        mAdapter.startListening();
        // Make sure new events are visible
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        // Tear down Firebase listeners in adapter
        mAdapter.stopListening();
    }

    /**
     *
     *method used to setup the alert dialog, if user press yes, send enable signal to firebase database ture.
     */
    public void takephoto1() {
        new AlertDialog.Builder(this)
                .setTitle("Detect an object is approaching.")
                .setMessage("Do you want take a photo?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseDatabase database_D = FirebaseDatabase.getInstance();
                        DatabaseReference enableRef_2 = database_D.getReference("Enable");
                        enableRef_2.setValue("true");
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


    //delete photos both in the recycle view and firebase
    public void delete_photo()
    {
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();
        DatabaseReference log = mDatabase.getReference("logs");//.push();

        StorageReference imageRef = mStorage.getReference().child("images").child(log.getKey());
        imageRef.delete();
        log.removeValue();

    }
}
