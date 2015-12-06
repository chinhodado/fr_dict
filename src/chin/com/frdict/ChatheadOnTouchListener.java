package chin.com.frdict;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class ChatheadOnTouchListener implements View.OnTouchListener {
    long time_start = 0, time_end = 0;
    boolean isLongclick = false, inBounded = false;
    int remove_img_width = 0, remove_img_height = 0;
    private int x_init_cord, y_init_cord, x_init_margin, y_init_margin;
    ChatHeadService service;

    public ChatheadOnTouchListener(ChatHeadService service) {
        this.service = service;
    }

    Handler handler_longClick = new Handler();
    Runnable runnable_longClick = new Runnable() {
        @Override
        public void run() {
            isLongclick = true;
            service.removeView.setVisibility(View.VISIBLE);
            chathead_longclick();
        }
    };

    @SuppressLint("NewApi")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) service.chatheadView.getLayoutParams();

        int x_cord = (int) event.getRawX();
        int y_cord = (int) event.getRawY();
        int x_cord_Destination, y_cord_Destination;

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            time_start = System.currentTimeMillis();
            handler_longClick.postDelayed(runnable_longClick, 600);

            remove_img_width = service.removeImg.getLayoutParams().width;
            remove_img_height = service.removeImg.getLayoutParams().height;

            x_init_cord = x_cord;
            y_init_cord = y_cord;

            x_init_margin = layoutParams.x;
            y_init_margin = layoutParams.y;

            break;
        case MotionEvent.ACTION_MOVE:
            int x_diff_move = x_cord - x_init_cord;
            int y_diff_move = y_cord - y_init_cord;

            x_cord_Destination = x_init_margin + x_diff_move;
            y_cord_Destination = y_init_margin + y_diff_move;

            if (isLongclick) {
                int x_bound_left = (service.szWindow.x - service.removeView.getWidth()) / 2 - 250;
                int x_bound_right = (service.szWindow.x + service.removeView.getWidth()) / 2 + 100;

                int y_bound_top = service.szWindow.y - (service.removeView.getHeight() + service.getStatusBarHeight()) - 200;

                if ((x_cord_Destination >= x_bound_left && x_cord_Destination <= x_bound_right) && y_cord_Destination >= y_bound_top) {
                    inBounded = true;

                    layoutParams.x = (service.szWindow.x - service.chatheadView.getWidth()) / 2;
                    layoutParams.y = service.szWindow.y - (service.removeView.getHeight() + service.getStatusBarHeight()) + 70;

                    if (service.removeImg.getLayoutParams().height == remove_img_height) {
                        service.removeImg.getLayoutParams().height = (int) (remove_img_height * 1.5);
                        service.removeImg.getLayoutParams().width = (int) (remove_img_width * 1.5);

                        WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) service.removeView.getLayoutParams();
                        int x_cord_remove = (int) ((service.szWindow.x - (remove_img_height * 1.5)) / 2);
                        int y_cord_remove = (int) (service.szWindow.y - ((remove_img_width * 1.5) + service.getStatusBarHeight()));
                        param_remove.x = x_cord_remove;
                        param_remove.y = y_cord_remove;

                        service.windowManager.updateViewLayout(service.removeView, param_remove);
                    }

                    service.windowManager.updateViewLayout(service.chatheadView, layoutParams);
                    break;
                }
                else {
                    inBounded = false;
                    service.removeImg.getLayoutParams().height = remove_img_height;
                    service.removeImg.getLayoutParams().width = remove_img_width;

                    WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) service.removeView.getLayoutParams();
                    int x_cord_remove = (service.szWindow.x - service.removeView.getWidth()) / 2;
                    int y_cord_remove = service.szWindow.y - (service.removeView.getHeight() + service.getStatusBarHeight());

                    param_remove.x = x_cord_remove;
                    param_remove.y = y_cord_remove;

                    service.windowManager.updateViewLayout(service.removeView, param_remove);
                }
            }

            layoutParams.x = x_cord_Destination;
            layoutParams.y = y_cord_Destination;

            service.windowManager.updateViewLayout(service.chatheadView, layoutParams);
            break;
        case MotionEvent.ACTION_UP:
            isLongclick = false;
            service.removeView.setVisibility(View.GONE);
            service.removeImg.getLayoutParams().height = remove_img_height;
            service.removeImg.getLayoutParams().width = remove_img_width;
            handler_longClick.removeCallbacks(runnable_longClick);

            if (inBounded) {
                if (ChatHeadService.mainView != null) { // should always be true
                    ChatHeadService.windowManager.removeView(ChatHeadService.mainView);
                }
                service.stopSelf();
                MainActivity.serviceRegistered = false;
                inBounded = false;
                break;
            }

            int x_diff = x_cord - x_init_cord;
            int y_diff = y_cord - y_init_cord;

            if (x_diff < 5 && y_diff < 5) {
                time_end = System.currentTimeMillis();
                if ((time_end - time_start) < 300) {
                    chathead_click();
                }
            }

            x_cord_Destination = x_init_margin + x_diff;
            y_cord_Destination = y_init_margin + y_diff;

            int x_start;
            x_start = x_cord_Destination;

            int BarHeight = service.getStatusBarHeight();
            if (y_cord_Destination < 0) {
                y_cord_Destination = 0;
            } else if (y_cord_Destination + (service.chatheadView.getHeight() + BarHeight) > service.szWindow.y) {
                y_cord_Destination = service.szWindow.y - (service.chatheadView.getHeight() + BarHeight);
            }
            layoutParams.y = y_cord_Destination;

            inBounded = false;
            service.resetPosition(x_start);

            break;
        default:
            break;
        }
        return true;
    }

    private void chathead_click() {
        Log.i(Utility.LogTag, "chathead_click()");
        if (ChatHeadService.mainViewVisible) {
            ChatHeadService.mainView.setVisibility(View.INVISIBLE);
            ChatHeadService.mainViewVisible = false;
        }
        else {
            ChatHeadService.mainView.setVisibility(View.VISIBLE);
            ChatHeadService.mainViewVisible = true;
        }
    }

    private void chathead_longclick() {
        WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) service.removeView.getLayoutParams();
        int x_cord_remove = (service.szWindow.x - service.removeView.getWidth()) / 2;
        int y_cord_remove = service.szWindow.y - (service.removeView.getHeight() + service.getStatusBarHeight());

        param_remove.x = x_cord_remove;
        param_remove.y = y_cord_remove;

        service.windowManager.updateViewLayout(service.removeView, param_remove);
    }
}
