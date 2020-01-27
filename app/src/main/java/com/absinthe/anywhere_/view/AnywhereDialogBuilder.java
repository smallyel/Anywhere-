package com.absinthe.anywhere_.view;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.absinthe.anywhere_.R;
import com.absinthe.anywhere_.utils.manager.DialogStack;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AnywhereDialogBuilder extends MaterialAlertDialogBuilder {

    private boolean isDismissParent = false;    //Dismiss this Dialog and its parent Dialog

    public AnywhereDialogBuilder(Context context) {
        super(context, R.style.AppTheme_Dialog);
    }

    public AnywhereDialogBuilder(Context context, int overrideThemeResId) {
        super(context, overrideThemeResId);
    }

    @Override
    public AlertDialog show() {
        setOnDismissListener(dialog -> {
            DialogStack.pop();

            if (isDismissParent) {
                DialogStack.pop();
            }
        });
        AlertDialog dialog = super.show();
        DialogStack.push(dialog);
        return dialog;
    }

    public void setDismissParent(boolean flag) {
        isDismissParent = flag;
    }
}
