package com.example.test1;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

public class Loading extends Activity implements Animator.AnimatorListener{

    private ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading);
        // set up what image it will use
        img = findViewById(R.id.img);
        ViewPropertyAnimator animate = img.animate();
        // set up the parameter that how long the image showing and how large it will scale.
        animate.scaleX(1.5f);
        animate.scaleY(1.5f);
        animate.setDuration(3500L);
        animate.setInterpolator(new AccelerateDecelerateInterpolator());
        animate.setListener(this);
        animate.start();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
