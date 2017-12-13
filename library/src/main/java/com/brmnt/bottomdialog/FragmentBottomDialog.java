package com.brmnt.bottomdialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;

/**
 * @author by Bramengton create: 24.08.17.
 */
abstract class FragmentBottomDialog<T extends BuilderBottomDialog> extends BottomSheetDialogFragment {
    abstract void setDialogStyledAttributes(Context context, @StyleRes int defStyle);
    abstract int setDialogStyle();

    private T mBuilder;
    public FragmentBottomDialog add(T bilder){
        mBuilder = bilder;
        return this;
    }

    T getBuilder(){
        return mBuilder;
    }

    private final static String PARCELABLE = "BuilderBottomDialogParcel";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCELABLE, mBuilder);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(savedInstanceState != null && savedInstanceState.containsKey(PARCELABLE)) {
            mBuilder = savedInstanceState.getParcelable(PARCELABLE);
        }

        Context context = getActivity().getApplicationContext();
        mBuilder.setContext(context);
        int theme=getThemeResId(context, setDialogStyle());
        setStyle(DialogFragment.STYLE_NO_TITLE, getThemeResId(context, theme));
        setDialogStyledAttributes(context, theme);
        return super.onCreateDialog(savedInstanceState);
    }

    private int getThemeResId(Context context, int themeId) {
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            TypedValue outValue = new TypedValue();
            if (context.getTheme().resolveAttribute(R.attr.bottomDialogStyle, outValue, true)) {
                themeId = outValue.resourceId;
            }
            else{
                themeId = R.style.BottomDialog_Dialog;
            }
        }
        return themeId;
    }

    /*dialog kept disappearing on orientation change.  might be a bug in v4 support.
	 * http://stackoverflow.com/questions/12433397/android-dialogfragment-disappears-after-orientation-change
	 * */
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
//            getDialog().setOnDismissListener(null);  --causes a crash on some devices.
        super.onDestroyView();
    }
}
