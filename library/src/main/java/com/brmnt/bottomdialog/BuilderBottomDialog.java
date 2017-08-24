package com.brmnt.bottomdialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.app.FragmentManager;

/**
 * @author by Bramengton create: 24.08.17.
 */
abstract class BuilderBottomDialog<T extends FragmentBottomDialog> extends Fields {

    private T mDialog;
    public abstract T setDialog();
    private FragmentManager mFragmentManager;

    BuilderBottomDialog(Context context, FragmentManager manager) {
        super(context);
        mFragmentManager = manager;
    }

    public FragmentManager getFragmentManager(){
        return mFragmentManager;
    }

    public T getDialog(){
        return mDialog;
    }

    @SuppressLint("Override")
    public T build() {
        mDialog = setDialog();
        mDialog.add(this);
        return mDialog;
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public T show() {
        mDialog = build();
        mDialog.show(mFragmentManager, null);
        return mDialog;
    }

    public void dismiss() {
        if(mDialog!=null) mDialog.dismiss();
    }
}
