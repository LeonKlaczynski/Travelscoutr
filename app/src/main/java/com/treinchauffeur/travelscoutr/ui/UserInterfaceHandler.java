package com.treinchauffeur.travelscoutr.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.treinchauffeur.travelscoutr.MapsActivity;
import com.treinchauffeur.travelscoutr.R;

import java.util.ArrayList;

public class UserInterfaceHandler {

    private boolean isLoading = false;

    private final MapsActivity activity;

    private MaterialCardView cardView;
    private ExtendedFloatingActionButton flickrBtn;
    public DrawerLayout drawerLayout;

    private boolean controlsHidden = false;
    private boolean uiIsTransitioning = false;
    public boolean isInfoWindowShown = false;
    public boolean mapIsMoving = false;

    private Runnable runnable;
    private Handler handler = new Handler();

    private Toast currentToast;
    private final ArrayList<Toast> activeToasts = new ArrayList<>();

    public UserInterfaceHandler(MapsActivity activity) {
        drawerLayout = activity.drawerLayout;
        this.activity = activity;
        cardView = activity.findViewById(R.id.topCardView);
        flickrBtn = activity.findViewById(R.id.flickrFab);
    }

    public void setIsLoading(boolean setLoading) {
        if(isLoading == setLoading) return;

        ProgressBar progressBar = activity.findViewById(R.id.progress_circular);
        ImageView searchIcon = activity.findViewById(R.id.cardSearchIcon);

        activity.runOnUiThread(() -> {
            if(setLoading) {
                isLoading = true;
                searchIcon.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                isLoading = false;
                searchIcon.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    public void onMapClick() {
        if (controlsHidden) moveCardDown();
        else if (!isInfoWindowShown) moveCardUp();
    }

    public void onCameraIdle() {
        handler.removeCallbacks(runnable);
        mapIsMoving = false;
        runnable = () -> {
            if (!controlsHidden) moveCardUp();
        };
        handler.postDelayed(runnable, 3000);
    }

    public void onCameraMoveStart(int reason) {
        mapIsMoving = true;

        if (reason != GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION &&
                reason != GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
            handler.removeCallbacks(runnable);
            if (controlsHidden) moveCardDown();
            activity.setFollowMyLocation(false);
        }
    }

    public void moveCardDown() {
        if (uiIsTransitioning || drawerLayout.isOpen()) return;
        float moveDistance = 0f;
        ObjectAnimator animation = ObjectAnimator.ofFloat(cardView, "translationY", moveDistance);
        animation.setDuration(300);
        animation.setInterpolator(new OvershootInterpolator());
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                uiIsTransitioning = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                controlsHidden = false;
                uiIsTransitioning = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animation.start();
        flickrBtn.show();
    }

    public void moveCardUp() {
        if (uiIsTransitioning || drawerLayout.isOpen() || cardView.hasFocus()) return;
        float currentY = cardView.getY();
        float moveDistance = -300f;
        long animationDuration = 200;
        TranslateAnimation animate = new TranslateAnimation(0, 0, 0, moveDistance);
        animate.setInterpolator(new AccelerateDecelerateInterpolator());
        animate.setDuration(animationDuration);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                uiIsTransitioning = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardView.setY(currentY + moveDistance);
                controlsHidden = true;
                uiIsTransitioning = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        cardView.startAnimation(animate);
        flickrBtn.hide();
    }

    public void sendToast(String text) {
        activity.runOnUiThread(() -> {
            for (Toast toast : activeToasts) toast.cancel();
            currentToast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
            activeToasts.add(currentToast);
            currentToast.show();
        });
    }
}
