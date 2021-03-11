package com.tencent.yolov5ncnn;

import android.hardware.Camera;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author: Allen
 * @date: 2021/2/8
 * @description:
 */
public class MyCamPara {
    private final CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private static MyCamPara myCamPara = null;

    private MyCamPara() {

    }

    public static MyCamPara getInstance() {
        if (myCamPara == null) {
            myCamPara = new MyCamPara();
            return myCamPara;
        } else {
            return myCamPara;
        }
    }

    public Camera.Size getPreviewSize(List<Camera.Size> list, int th) {
        Collections.sort(list, sizeComparator);
        Camera.Size size = null;
        for (int i = 0; i < list.size(); i++) {
            size = list.get(i);
            if ((size.width > th) && equalRate(size, 1.3f)) {
                break;
            }
        }
        return size;
    }

    public Camera.Size getPictureSize(List<Camera.Size> list, int th) {
        Collections.sort(list, sizeComparator);
        Camera.Size size = null;
        for (int i = 0; i < list.size(); i++) {
            size = list.get(i);
            if ((size.width > th) && equalRate(size, 1.3f)) {
                break;
            }
        }
        return size;

    }

    public boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        if (Math.abs(r - rate) <= 0.2) {
            return true;
        } else {
            return false;
        }
    }

    public class CameraSizeComparator implements Comparator<Camera.Size> {
        //按升序排列
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // TODO Auto-generated method stub
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }
}
