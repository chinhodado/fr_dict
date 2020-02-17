package chin.com.frdict;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import chin.com.frdict.activity.DictionaryActivity;

public class ChatheadOnTouchListener implements View.OnTouchListener {
    private long timeStart = 0;
    private boolean isLongClick = false;
    private boolean inBounded = false;
    private int removeImgWidth = 0;
    private int removeImgHeight = 0;
    private int xInitCord, yInitCord, xInitMargin, yInitMargin;
    private final ChatHeadService service;

    public ChatheadOnTouchListener(ChatHeadService service) {
        this.service = service;
    }

    private Handler longClickHandler = new Handler();
    private Runnable longClickRunnable = new Runnable() {
        @Override
        public void run() {
            isLongClick = true;
            service.getRemoveView().setVisibility(View.VISIBLE);
            onChatheadLongClick();
        }
    };

    @SuppressLint({"NewApi", "ClickableViewAccessibility"})
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        RelativeLayout chatheadView = service.getChatheadView();
        RelativeLayout removeView = service.getRemoveView();
        ImageView removeImg = service.getRemoveImg();
        WindowManager windowManager = service.getWindowManager();
        Point szWindow = service.getSzWindow();

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

        int xCord = (int) event.getRawX();
        int yCord = (int) event.getRawY();
        int xCordDestination, yCordDestination;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                timeStart = System.currentTimeMillis();
                longClickHandler.postDelayed(longClickRunnable, 600);

                removeImgWidth = removeImg.getLayoutParams().width;
                removeImgHeight = removeImg.getLayoutParams().height;

                xInitCord = xCord;
                yInitCord = yCord;

                xInitMargin = layoutParams.x;
                yInitMargin = layoutParams.y;

                break;
            case MotionEvent.ACTION_MOVE:
                int xDiffMove = xCord - xInitCord;
                int yDiffMove = yCord - yInitCord;

                xCordDestination = xInitMargin + xDiffMove;
                yCordDestination = yInitMargin + yDiffMove;

                if (isLongClick) {
                    int xBoundLeft = (szWindow.x - removeView.getWidth()) / 2 - 250;
                    int xBoundRight = (szWindow.x + removeView.getWidth()) / 2 + 100;

                    int yBoundTop = szWindow.y - (removeView.getHeight() + service.getStatusBarHeight()) - 200;

                    if ((xCordDestination >= xBoundLeft && xCordDestination <= xBoundRight) && yCordDestination >= yBoundTop) {
                        inBounded = true;

                        layoutParams.x = (szWindow.x - chatheadView.getWidth()) / 2;
                        layoutParams.y = szWindow.y - (removeView.getHeight() + service.getStatusBarHeight()) + 70;

                        if (removeImg.getLayoutParams().height == removeImgHeight) {
                            removeImg.getLayoutParams().height = (int) (removeImgHeight * 1.5);
                            removeImg.getLayoutParams().width = (int) (removeImgWidth * 1.5);

                            WindowManager.LayoutParams paramRemove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                            int xCordRemove = (int) ((szWindow.x - (removeImgHeight * 1.5)) / 2);
                            int yCordRemove = (int) (szWindow.y - ((removeImgWidth * 1.5) + service.getStatusBarHeight()));
                            paramRemove.x = xCordRemove;
                            paramRemove.y = yCordRemove;

                            windowManager.updateViewLayout(removeView, paramRemove);
                        }

                        windowManager.updateViewLayout(chatheadView, layoutParams);
                        break;
                    }
                    else {
                        inBounded = false;
                        removeImg.getLayoutParams().height = removeImgHeight;
                        removeImg.getLayoutParams().width = removeImgWidth;

                        WindowManager.LayoutParams paramRemove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                        int xCordRemove = (szWindow.x - removeView.getWidth()) / 2;
                        int yCordRemove = szWindow.y - (removeView.getHeight() + service.getStatusBarHeight());

                        paramRemove.x = xCordRemove;
                        paramRemove.y = yCordRemove;

                        windowManager.updateViewLayout(removeView, paramRemove);
                    }
                }

                layoutParams.x = xCordDestination;
                layoutParams.y = yCordDestination;

                windowManager.updateViewLayout(chatheadView, layoutParams);
                break;
            case MotionEvent.ACTION_UP:
                isLongClick = false;
                removeView.setVisibility(View.GONE);
                removeImg.getLayoutParams().height = removeImgHeight;
                removeImg.getLayoutParams().width = removeImgWidth;
                longClickHandler.removeCallbacks(longClickRunnable);

                if (inBounded) {
                    onChatheadClose();
                    inBounded = false;
                    break;
                }

                int xDiff = xCord - xInitCord;
                int yDiff = yCord - yInitCord;

                if (xDiff < 5 && yDiff < 5) {
                    long timeEnd = System.currentTimeMillis();
                    if ((timeEnd - timeStart) < 300) {
                        onChatheadClick();
                    }
                }

                xCordDestination = xInitMargin + xDiff;
                yCordDestination = yInitMargin + yDiff;

                int xStart = xCordDestination;

                int barHeight = service.getStatusBarHeight();
                if (yCordDestination < 0) {
                    yCordDestination = 0;
                }
                else if (yCordDestination + (chatheadView.getHeight() + barHeight) > szWindow.y) {
                    yCordDestination = szWindow.y - (chatheadView.getHeight() + barHeight);
                }
                layoutParams.y = yCordDestination;

                inBounded = false;
                service.resetPosition(xStart);

                break;
            default:
                break;
        }
        return true;
    }

    private void onChatheadClose() {
        if (DictionaryActivity.INSTANCE != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DictionaryActivity.INSTANCE.finishAndRemoveTask();
            }
            else {
                // This will leave the task in the task list
                // I'm too lazy to figure out how to do this (remove the task) properly on lower APIs
                // and I don't own any pre-lollipop device anyway...
                DictionaryActivity.INSTANCE.finish();
            }
        }

        service.stopSelf();
    }

    private void onChatheadClick() {
        Log.i(Utility.LogTag, "onChatheadClick()");
        if (DictionaryActivity.active) {
            DictionaryActivity.INSTANCE.moveTaskToBack(true);
        } else {
            Intent it = new Intent(service, DictionaryActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            service.startActivity(it);
        }
    }

    private void onChatheadLongClick() {
        RelativeLayout removeView = service.getRemoveView();
        WindowManager.LayoutParams paramRemove = (WindowManager.LayoutParams) removeView.getLayoutParams();
        Point szWindow = service.getSzWindow();
        int xCordRemove = (szWindow.x - removeView.getWidth()) / 2;
        int yCordRemove = szWindow.y - (removeView.getHeight() + service.getStatusBarHeight());

        paramRemove.x = xCordRemove;
        paramRemove.y = yCordRemove;

        service.getWindowManager().updateViewLayout(removeView, paramRemove);
    }
}