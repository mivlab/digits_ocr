package com.tencent.yolov5ncnn.camera;

/**
 * @author: Allen
 * @date: 2021/2/8
 * @description:
 */
public interface OnCaptureCallback {

    public void onCapture(boolean success, String filePath);
}