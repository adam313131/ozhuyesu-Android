package com.ozhuyesu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScrollAwareWebView extends WebView {

    private OnScrollListener onScrollListener;
    private float lastY;
    private static final int SCROLL_THRESHOLD_DP = 4; // A small threshold in DP
    private final int scrollThreshold;

    public ScrollAwareWebView(@NonNull Context context) {
        this(context, null);
    }

    public ScrollAwareWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollAwareWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Convert the DP threshold to pixels
        scrollThreshold = (int) (SCROLL_THRESHOLD_DP * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (onScrollListener != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Store the initial Y position when the user touches the screen
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    // When the user moves their finger
                    float currentY = event.getY();
                    float dy = currentY - lastY; // Calculate the change in Y

                    // Check if the movement is significant enough to be considered a scroll
                    if (Math.abs(dy) > scrollThreshold) {
                        if (dy > 0) {
                            // Finger moving down -> content scrolling UP
                            onScrollListener.onScrollUp();
                        } else {
                            // Finger moving up -> content scrolling DOWN
                            onScrollListener.onScrollDown();
                        }
                        // Update the last Y position for the next movement calculation
                        lastY = currentY;
                    }
                    break;
            }
        }
        // Pass the event to the parent class to handle standard WebView behavior
        return super.onTouchEvent(event);
    }

    // Method to set the scroll listener
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    // The callback interface
    public interface OnScrollListener {
        void onScrollUp();   // Scrolling up (to show the buttons)
        void onScrollDown(); // Scrolling down (to hide the buttons)
    }
}
