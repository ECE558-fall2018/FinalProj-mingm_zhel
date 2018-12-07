package com.example.test1;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * RecyclerView adapter to populate image entries from Firebase.
 */
public class ImageEntryAdapter extends FirebaseRecyclerAdapter<ImageEntry, ImageEntryAdapter.ImageEntryViewHolder> {

    /**
     * ViewHolder for each image entry
     */
    public static class ImageEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView time;
        public final TextView metadata;

        boolean isImageFitToScreen;

        public ImageEntryViewHolder(View itemView) {
            super(itemView);

            this.image = (ImageView) itemView.findViewById(R.id.imageView1);
            this.time = (TextView) itemView.findViewById(R.id.textView1);
            this.metadata = (TextView) itemView.findViewById(R.id.textView2);


            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isImageFitToScreen) {
                        isImageFitToScreen = false;
                        image.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        image.setAdjustViewBounds(true);
                    } else {
                        isImageFitToScreen = true;
                        image.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                        image.setScaleType(ImageView.ScaleType.FIT_XY);
                    }
                }
            });
        }
    }

    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;

    public ImageEntryAdapter(Context context, DatabaseReference ref) {
        super(new FirebaseRecyclerOptions.Builder<ImageEntry>()
                .setQuery(ref, ImageEntry.class)
                .build());

        mApplicationContext = context.getApplicationContext();
        mFirebaseStorage = FirebaseStorage.getInstance();


    }

    @Override
    public ImageEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.doorbell_entry, parent, false);

        return new ImageEntryViewHolder(entryView);
    }

    @Override
    protected void onBindViewHolder(ImageEntryViewHolder holder, int position, ImageEntry model) {
        // Display the timestamp
        CharSequence prettyTime = DateUtils.getRelativeDateTimeString(mApplicationContext,
                model.getTimestamp(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.time.setText(prettyTime);

        // Display the image
        if (model.getImage() != null) {
            StorageReference imageRef = mFirebaseStorage.getReferenceFromUrl(model.getImage());

            GlideApp.with(mApplicationContext)
                    .load(imageRef)
                    .placeholder(R.drawable.ic_image)
                    .into(holder.image);
        }

    }
}
