package com.tencent.yolov5ncnn;

import android.os.Parcel;
import android.os.Parcelable;

public class LocationParam implements Parcelable {
    public float xPos;
    public float yPos;
    public float width;
    public float height;
    public float innerXPos;
    public float innerYPos;
    public float innerWidth;
    public float innerHeight;

    // roi 数组
    public float[] LXPos;
    public float[] LYPos;
    public float[] LWidth;
    public float[] LHeight;

    // roi 数量
    public int k;

    // 屏幕id
    public int id;


    public LocationParam() {
    }

    protected LocationParam(Parcel in) {
        xPos = in.readFloat();
        yPos = in.readFloat();
        width = in.readFloat();
        height = in.readFloat();
        innerXPos = in.readFloat();
        innerYPos = in.readFloat();
        innerWidth = in.readFloat();
        innerHeight = in.readFloat();
        LXPos = in.createFloatArray();
        LYPos = in.createFloatArray();
        LWidth = in.createFloatArray();
        LHeight = in.createFloatArray();

        // roi
        k = in.readInt();
        id = in.readInt();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(xPos);
        dest.writeFloat(yPos);
        dest.writeFloat(width);
        dest.writeFloat(height);
        dest.writeFloat(innerXPos);
        dest.writeFloat(innerYPos);
        dest.writeFloat(innerWidth);
        dest.writeFloat(innerHeight);

        dest.writeFloatArray(LXPos);
        dest.writeFloatArray(LYPos);
        dest.writeFloatArray(LWidth);
        dest.writeFloatArray(LHeight);
        dest.writeInt(k);
        dest.writeInt(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocationParam> CREATOR = new Creator<LocationParam>() {
        @Override
        public LocationParam createFromParcel(Parcel in) {
            return new LocationParam(in);
        }

        @Override
        public LocationParam[] newArray(int size) {
            return new LocationParam[size];
        }
    };
}
