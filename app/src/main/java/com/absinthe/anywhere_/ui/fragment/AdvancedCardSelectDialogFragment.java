package com.absinthe.anywhere_.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.absinthe.anywhere_.view.AnywhereDialogBuilder;
import com.absinthe.anywhere_.view.AnywhereDialogFragment;
import com.absinthe.anywhere_.viewbuilder.entity.AdvancedCardSelectDialogBuilder;

public class AdvancedCardSelectDialogFragment extends AnywhereDialogFragment {

    private Context mContext;
    private AdvancedCardSelectDialogBuilder mBuilder;
    private OnClickItemListener mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = getActivity();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AnywhereDialogBuilder builder = new AnywhereDialogBuilder(mContext);
        mBuilder = new AdvancedCardSelectDialogBuilder(mContext);
        initView();

        return builder.setView(mBuilder.getRoot()).create();
    }

    public void setListener(OnClickItemListener mListener) {
        this.mListener = mListener;
    }

    private void initView() {
        mBuilder.tvAddImage.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onClick(0);
            }
        });
        mBuilder.tvAddShell.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onClick(1);
            }
        });
    }

    public interface OnClickItemListener {
        void onClick(int item);
    }
}
