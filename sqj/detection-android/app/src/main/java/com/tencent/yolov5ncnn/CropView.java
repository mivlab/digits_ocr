package com.tencent.yolov5ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class CropView extends View {
    //    int xProp = 4;
//    int yProp = 3;
    LocationParam locationParam;
    int xSize = 0;
    int ySize = 0;
    Rect cutRect = new Rect();
    private int viewWidth = 0;
    private int viewHeight = 0;

    public CropView(Context context) {
        super(context);
    }

    public CropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setLocationParam(LocationParam param) {
        locationParam = param;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        // 在拍照界面绘制提示文字
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50);
        String str = "请将红色框对准屏幕内边缘，绿色框对准待识别目标";
        canvas.drawText(str,canvas.getWidth()*0.5f-550,50,textPaint);
        String strS = "当前屏幕编号："+locationParam.id;
        canvas.drawText(strS,canvas.getWidth()*0.5f-150,110,textPaint);

        // 透明框区域
        Paint cropPaint = new Paint();
        cropPaint.setColor(Color.BLACK);
        cropPaint.setAlpha(127);

        viewWidth = canvas.getWidth();
        viewHeight = canvas.getHeight();

        Log.v("TAG","viweWidth:"+viewWidth+"  viewHeight:"+viewHeight);


//        canvas.drawColor(Color.parseColor("#a0000000"));
        makeCutRect(viewWidth, viewHeight);

        Rect cropRect = new Rect(0, 0, viewWidth, cutRect.top);
//        canvas.clipRect(cropRect);
        canvas.drawRect(cropRect, cropPaint);

        cropRect.top = cutRect.top;
        cropRect.bottom = cutRect.bottom;
        cropRect.right = cutRect.left;
        canvas.drawRect(cropRect, cropPaint);

        cropRect.left = cutRect.right;
        cropRect.right = viewWidth;
        canvas.drawRect(cropRect, cropPaint);

        cropRect.left = 0;
        cropRect.right = viewWidth;
        cropRect.top = cutRect.bottom;
        cropRect.bottom = viewHeight;
        canvas.drawRect(cropRect, cropPaint);

        // 屏幕区域 红框
        Paint outPaint = new Paint();
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setColor(Color.RED);
        outPaint.setStrokeWidth(3.0f);
        canvas.drawRect(cutRect, outPaint);

//        Rect numRect = makeNumRect(cutRect);
        // 绘画roi区域，并且显示
        //Rect numRect = makeNumRect(viewWidth,viewHeight);
        //canvas.drawRect(numRect, outPaint);
        Paint roioutPaint = new Paint();
        roioutPaint.setStyle(Paint.Style.STROKE);
        roioutPaint.setColor(Color.GREEN);
        roioutPaint.setStrokeWidth(3.0f);
        for(int i=0;i<locationParam.k;++i)
        {
            Rect numRect = makeNumRect(viewWidth,viewHeight,i);
            canvas.drawRect(numRect,roioutPaint);
        }

        //        super.onDraw(canvas);
    }

    // 绘制数字区域
    private Rect makeNumRect(int nWidth, int nHeight) {
        Rect rtNum = new Rect();
        rtNum.left = (int) (nWidth * locationParam.innerXPos);
        rtNum.top = (int) (nHeight * locationParam.innerYPos);
        rtNum.right = (int) (nWidth * (locationParam.innerXPos+locationParam.innerWidth));
        rtNum.bottom = (int) (nHeight * (locationParam.innerYPos+locationParam.innerHeight));
        return rtNum;
    }

    // 绘制数字区域
    private Rect makeNumRect(int nWidth, int nHeight,int k) {
        Rect rtNum = new Rect();
        rtNum.left = (int) (nWidth * locationParam.LXPos[k]);
        rtNum.top = (int) (nHeight * locationParam.LYPos[k]);
        rtNum.right = (int) (nWidth * (locationParam.LXPos[k]+locationParam.LWidth[k]));
        rtNum.bottom = (int) (nHeight * (locationParam.LYPos[k]+locationParam.LHeight[k]));
        return rtNum;
    }

    private Rect makeNumRect(Rect cutRect) {
        Rect rtNum = new Rect();
        int nWidth = cutRect.right - cutRect.left;
        int nHeight = cutRect.bottom - cutRect.top;
        float fLeft = 0.3f;
        float fRight = 0.9f;
        float fTop = 0.8f;
        float fBottom = 0.9f;

        rtNum.left = (int) (cutRect.left + nWidth * fLeft);
        rtNum.right = (int) (cutRect.left + nWidth * fRight);
        rtNum.top = (int) (cutRect.top + nHeight * fTop);
        rtNum.bottom = (int) (cutRect.top + nHeight * fBottom);

        return rtNum;
    }

    int DEFAULT_OFFSET = 300;

    private void makeCutRect(int nWidth, int nHeight) {
//        int yOffset = DEFAULT_OFFSET;
//        int xOffset = DEFAULT_OFFSET;
//
//        int yValue = nHeight-2*yOffset;
//        int xValue = yValue*xProp/yProp;
//        if(xValue>nWidth){
//            xValue = nWidth-2*xOffset;
//            yValue = xValue*yProp/xProp;
//            yOffset = (nHeight-yValue)/2;
//        }else{
//            xOffset = (nWidth-xValue)/2;
//        }
//
//        cutRect.left = xOffset;
//        cutRect.right = nWidth - xOffset;
//        cutRect.top = yOffset;
//        cutRect.bottom = nHeight-yOffset;

        // 绘制屏幕区域
        cutRect.left = (int) (nWidth * locationParam.xPos);
        cutRect.top = (int) (nHeight * locationParam.yPos);
        cutRect.right = (int) (nWidth * (locationParam.xPos+locationParam.width));
        cutRect.bottom = (int) (nHeight * (locationParam.yPos+locationParam.height));
        Log.v("d","ctleft:"+cutRect.left+" cttop:"+cutRect.top+" ctright:"+cutRect.right+" ctbottom:"+cutRect.bottom);

    }

    // 获取屏幕区域
    public Rect getCropRect(int width, int height) {
        Rect rtRet = new Rect();
        float scale = width * 1.0f / viewWidth;
        Log.v("D","scale:"+scale+" viewwidth:"+viewWidth+" width:"+width);
        rtRet.left = (int) (cutRect.left * scale);
        rtRet.top = (int) (cutRect.top * scale);
        rtRet.right = (int) (cutRect.right * scale);
        rtRet.bottom = (int) (cutRect.bottom * scale);
        Log.v("d","rtleft:"+rtRet.left+" rttop:"+rtRet.top+" rtright:"+rtRet.right+" rtbottom:"+rtRet.bottom);
        return rtRet;
    }

    // 获取数字区域
    public Rect getNumRect(int width, int height){
        Rect nrtRet = new Rect();
        Rect rtRet = makeNumRect(viewWidth,viewHeight);
        float scale = width * 1.0f / viewWidth;
        nrtRet.left = (int)(rtRet.left * scale);
        nrtRet.top = (int)(rtRet.top * scale);
        nrtRet.right = (int)(rtRet.right * scale);
        nrtRet.bottom = (int)(rtRet.bottom * scale);
        return nrtRet;
    }

    public Rect getNumRect(int width, int height, int k){
        Rect nrtRet = new Rect();
        Rect rtRet = makeNumRect(viewWidth,viewHeight,k);
        float scale = width * 1.0f / viewWidth;
        nrtRet.left = (int)(rtRet.left * scale);
        nrtRet.top = (int)(rtRet.top * scale);
        nrtRet.right = (int)(rtRet.right * scale);
        nrtRet.bottom = (int)(rtRet.bottom * scale);
        return nrtRet;
    }

    public int getNum(){
        return locationParam.k;
    }
}
