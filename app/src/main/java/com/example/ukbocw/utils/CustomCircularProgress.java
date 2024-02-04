package com.example.ukbocw.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

import com.example.ukbocw.R;


public class CustomCircularProgress {
    private Dialog dialog;
    private static CustomCircularProgress mInstance;

    public static synchronized CustomCircularProgress getInstance() {
        if (mInstance == null) {
            mInstance = new CustomCircularProgress();
        }
        return mInstance;
    }

    public void show(Context context) {
        try {
            if (dialog != null && dialog.isShowing()) {
                return;
            }
            dialog = new Dialog(context);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.materialprogressbar);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCancelable(false);
            dialog.show();
        } catch (Exception e) {

        }

    }

    public void dismiss() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();

            }
        } catch (IllegalArgumentException e) {

        }

    }
}
