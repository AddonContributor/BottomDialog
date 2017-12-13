package com.brmnt.bottomdialog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.*;

/**
 * @author by Bramengton
 * @date 24.08.17.
 */
class Fields implements Parcelable{

    Fields(){}

    private Context mContext;
    private int mTheme = 0;
    private Drawable mIcon;
    private int mIconRes;
    private CharSequence mTitle;
    private int mTitleRes;
    private Object[] mTitleArgs;

    private boolean mGrid = false;
    private @IntegerRes int mLimit = -1;

    protected void setContext(Context context){
        mContext = context;
    }

    public Fields Theme(@StyleRes int style) {
        mTheme= style;
        return this;
    }

    public int getTheme(){
        return mTheme;
    }

    public Fields setTitle(CharSequence title) {
        this.mTitle = title;
        return this;
    }

    public Fields setTitle(@StringRes int title) {
        this.mTitleRes = title;
        return this;
    }

    public Fields setTitle(@StringRes int resId, Object... formatArgs) {
        mTitleRes = resId;
        mTitleArgs = formatArgs;
        return this;
    }

    public CharSequence getTitle() {
        if((mTitle==null || mTitle.length()<=0) && (mTitleRes>0))
            mTitle = mContext.getString(mTitleRes, mTitleArgs);
        return mTitle;
    }

    public Fields setGrid() {
        this.mGrid = true;
        return this;
    }

    public boolean isGrid(){
        return mGrid;
    }

    public Fields setLimit(@IntegerRes int limitRes) {
        mLimit = limitRes;
        return this;
    }

    public int getLimit(){
        return mLimit>0 ? mContext.getResources().getInteger(mLimit) : mLimit;
    }

    public Fields setIcon(Drawable icon) {
        this.mIcon = icon;
        return this;
    }

    public Fields setIcon(@DrawableRes int iconRes) {
        this.mIconRes = iconRes;
        return this;
    }

    public Drawable getIcon() {
        if((mIcon==null) && (mIconRes>0))
            mIcon = mContext.getResources().getDrawable(mIconRes);
        return mIcon;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator<Fields> CREATOR = new Parcelable.Creator<Fields>() {
        @Override
        public Fields createFromParcel(Parcel in) {
            return new Fields(in);
        }

        @Override
        public Fields[] newArray(int size) {
            return new Fields[size];
        }
    };

    @SuppressWarnings("unchecked")
    private Fields(@NonNull Parcel source) {
        mTheme = source.readInt();
        mTitle = source.readString().subSequence(0, source.readString().length());
        mGrid = source.readByte() != 0;
        mLimit = source.readInt();

        Bitmap bitmap = source.readParcelable(getClass().getClassLoader());
        // Convert Bitmap to Drawable:
        mIcon = new BitmapDrawable(Resources.getSystem(), bitmap);
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        arg0.writeString(mTitle!=null ? mTitle.toString() : "");
        arg0.writeInt(mTheme);
        arg0.writeByte((byte)(mGrid ? 1:0));
        arg0.writeInt(mLimit);
        Bitmap bitmap = ((BitmapDrawable) mIcon).getBitmap();
        arg0.writeParcelable(bitmap, arg1);
    }
}
