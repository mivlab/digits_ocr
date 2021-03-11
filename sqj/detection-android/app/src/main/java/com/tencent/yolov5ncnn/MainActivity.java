// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolov5ncnn;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import io.reactivex.functions.Consumer;

public class MainActivity extends FragmentActivity {
    private static final int SELECT_IMAGE = 1;
    private int FLAG = 0;

    private ImageView imageView;
    private Bitmap bitmap = null;
    private Bitmap roibitmap = null;
    private Bitmap yourSelectedImage = null;
    private int requestCodeCam = 0x100;
    //private Canvas canvas = null;


    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();

    // 仪器屏幕区域坐标
    private float x = 0;
    private float y = 0;
    private float w = 0;
    private float h = 0;

    // roi区域坐标
    int k = 0;   // 每张图中需要识别的区域数量
    int id = -1;    // 屏幕id
    private float[] inX = null;
    private float[] inY = null;
    private float[] inW = null;
    private float[] inH = null;



    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        boolean ret_init = yolov5ncnn.Init(getAssets());
        if (!ret_init) {
            Log.e("MainActivity", "yolov5ncnn Init failed");
        }

        imageView = (ImageView) findViewById(R.id.imageView);

        //进入本地相册，选图
        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

//        //识别cpu
//        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
//        buttonDetect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                if (yourSelectedImage == null)
//                    return;
//
//                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, false);
//
//                showObjects(objects);
//            }
//        });


//        //识别gpu操作
//        Button buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
//        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                if (yourSelectedImage == null)
//                    return;
////                int pw = yourSelectedImage.getWidth();
////                int ph = yourSelectedImage.getHeight();
////                Log.v("D","pw:"+pw+"  ph:"+ph);
//                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, true);
//                showObjects(objects);
//            }
//
//
//        });

        //识别gpu操作
        Button buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage != null && FLAG==0) {
                    YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, true);
                    showObjects(objects);
                    yourSelectedImage = null;
                }
                else if(FLAG == 1){
                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(rgba);
                    //canvas = new Canvas(bitmap);
                    for(int i=0;i<k;++i){
                        String strPathFront = Environment.getExternalStorageDirectory().toString()+"/"+i+".jpg";
                        roibitmap = BitmapFactory.decodeFile(strPathFront);

                        //Bitmap newBitmap = pureColorPicture(roibitmap);
                        //String path = saveBitmap(newBitmap);

                        //Log.v("D","yoloroiPath:"+strPathFront);
                        //Log.v("D","roibitmap:"+roibitmap.getHeight()+" height:"+roibitmap.getHeight());


                        Bitmap SelectedImage = roibitmap.copy(Bitmap.Config.ARGB_8888, true);
                        //SelectedImage = Bitmap.createScaledBitmap(SelectedImage,640,640,true);
                        //Bitmap SelectedImage = newBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(SelectedImage, true);
                        showObjects(objects,canvas,rgba,i);
                        delete(Environment.getExternalStorageDirectory().toString(),i+".jpg");
                        //Log.v("d","i:"+i);
                        FLAG = 0;

//                        Log.v("D","yoloroiPath:"+strPathFront);
//                        try {
//                            //Uri selectedImage = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), roibitmap, null, null));
//                            Uri selectedImage = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), roibitmap, "123",null));
//                            //Log.v("D","uri:"+strPathFront);
//                            roibitmap = decodeUri(selectedImage);
//                            Bitmap SelectedImage = roibitmap.copy(Bitmap.Config.ARGB_8888, true);
//                            YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(SelectedImage, true);
//                            showObjects(objects,canvas,rgba,i);
//                            //imageView.setImageBitmap(bitmap);
//                        }catch (FileNotFoundException e){
//                            Log.e("MainActivity", "FileNotFoundException");
//                            return;}
//                        FLAG=0;
                    }
                    //deleteDirectory(Environment.getExternalStorageDirectory().toString());
                }
                else {
                    Log.v("D","请重新拍照");
                    return;
                }
            }
        });


        //拍照
        findViewById(R.id.buttonCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //申请拍照权限
                RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
                rxPermissions.request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {  //申请成功，跳转拍照页面
                            Intent captureIntent = new Intent(MainActivity.this, MainActivityNew.class);
                            LocationParam param = new LocationParam();
                            //getRect(instrument);

                            if(x==0 || y==0 || w == 0 || h==0)
                            {
                                Log.e("e","请先选择仪器");
                                Toast.makeText(getApplicationContext(),"请先选择仪器",Toast.LENGTH_LONG).show();
                                return;
                            }
                            // 屏幕区域信息
                            param.xPos=x;   //0.1f;
                            param.yPos=y;   //0.1f;
                            param.width=w;  //0.8f;
                            param.height=h; //0.8f;

//                            param.innerXPos=0.1f;
//                            param.innerYPos=0.1f;
//                            param.innerWidth=0.3f;
//                            param.innerHeight=0.1f;

                            param.k = k;
                            param.id = id;
                            param.LXPos = inX;
                            param.LYPos = inY;
                            param.LWidth = inW;
                            param.LHeight = inH;
                            captureIntent.putExtra("data",param);
                            startActivityForResult(captureIntent, requestCodeCam);
                            //x=y=w=h=0;
                        }
                    }
                });


            }
        });
    }

    // 选择仪器
    public void singleChoice(View v)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择仪器");
        String[] items = {"屏幕1","屏幕2","屏幕3","屏幕4","屏幕5","屏幕6","屏幕7","屏幕8","屏幕9","屏幕10","屏幕11","屏幕12","屏幕13","屏幕14","屏幕15"};
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                getRect(which);
                id = which+1;
                //Log.v("D","instrument:"+items[which]);
                //Log.v("D","id:"+which);
                Toast.makeText(getApplicationContext(),"你选择了"+items[which],Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    // 选择仪器后，确定框大小
    public void getRect(int i)
    {
        switch (i)
        {
            case 0:
//                k=1;x = 0.21092f;y = 0.21495f;w = 0.54839f;h = 0.54563f;
//                inX = new float[]{0.41439f};
//                inY = new float[]{0.56217f};
//                inW = new float[]{0.20099f};
//                inH = new float[]{0.18188f};

                k=1;x=0.15f;y=0.157f;w=0.7f;h=0.685f;
                inX = new float[]{0.403f};//0.372f
                inY = new float[]{0.625f};
                inW = new float[]{0.267f};//0.328f
                inH = new float[]{0.174f};//0.24f
                Log.v("d","inx:"+inX[0]);
                break;
            case 1:
                k=3;x=0.15f;y=0.219f;w=0.7f;h=0.562f;
                inX = new float[]{0.243f,0.243f,0.243f};
                inY = new float[]{0.380f,0.490f,0.598f};
                inW = new float[]{0.169f,0.169f,0.169f};
                inH = new float[]{0.085f,0.085f,0.085f};
                break;
            case 2:
                k=1;x=0.15f;y=0.226f;w=0.7f;h=0.548f;
                inX = new float[]{0.175f};
                inY = new float[]{0.300f};
                inW = new float[]{0.445f};
                inH = new float[]{0.240f};
                break;
            case 3:
                k=1;x=0.15f;y=0.342f;w=0.7f;h=0.317f;
                inX = new float[]{0.204f};
                inY = new float[]{0.375f};
                inW = new float[]{0.579f};
                inH = new float[]{0.256f};
                break;
            case 4:
                k=1;x=0.15f;y=0.319f;w=0.7f;h=0.363f;
                inX = new float[]{0.362f};
                inY = new float[]{0.529f};
                inW = new float[]{0.297f};
                inH = new float[]{0.122f};
                break;
            case 5:
                k=1;x=0.15f;y=0.244f;w=0.7f;h=0.511f;
                inX = new float[]{0.320f};
                inY = new float[]{0.350f};
                inW = new float[]{0.132f};
                inH = new float[]{0.086f};
                break;
            case 6:
                k=1;x=0.15f;y=0.283f;w=0.7f;h=0.434f;
                inX = new float[]{0.213f};
                inY = new float[]{0.384f};
                inW = new float[]{0.578f};
                inH = new float[]{0.245f};
                break;
            case 7://chu cuo
                k=3;x=0.15f;y=0.240f;w=0.7f;h=0.521f;
                inX = new float[]{0.285f,0.285f,0.527f};
                inY = new float[]{0.430f,0.488f,0.426f};
                inW = new float[]{0.118f,0.118f,0.179f};
                inH = new float[]{0.056f,0.055f,0.128f};
                break;
            case 8:
                k=1;x=0.15f;y=0.253f;w=0.7f;h=0.493f;
                inX = new float[]{0.201f};
                inY = new float[]{0.496f};
                inW = new float[]{0.222f};
                inH = new float[]{0.116f};
                break;
            case 9:
                k=1;x=0.15f;y=0.147f;w=0.7f;h=0.706f;
                inX = new float[]{0.168f};
                inY = new float[]{0.365f};
                inW = new float[]{0.258f};
                inH = new float[]{0.111f};
                break;
            case 10:
                k=1;x=0.15f;y=0.144f;w=0.7f;h=0.711f;
                inX = new float[]{0.341f};
                inY = new float[]{0.483f};
                inW = new float[]{0.293f};
                inH = new float[]{0.199f};
                break;
            case 11:
                k=6;x=0.15f;y=0.15f;w=0.7f;h=0.7f;
                inX = new float[]{0.430f,0.642f,0.430f,0.642f,0.430f,0.642f};
                inY = new float[]{0.530f,0.530f,0.585f,0.585f,0.638f,0.638f};
                inW = new float[]{0.134f,0.102f,0.134f,0.102f,0.134f,0.102f};
                inH = new float[]{0.050f,0.050f,0.050f,0.050f,0.044f,0.044f};
                break;
            case 12:
                k=6;x=0.15f;y=0.244f;w=0.7f;h=0.512f;
                inX = new float[]{0.346f,0.633f,0.346f,0.633f,0.346f,0.633f};
                inY = new float[]{0.283f,0.283f,0.402f,0.402f,0.505f,0.505f};
                inW = new float[]{0.251f,0.198f,0.251f,0.198f,0.251f,0.198f};
                inH = new float[]{0.108f,0.111f,0.095f,0.095f,0.095f,0.095f};
                break;
            case 13:
                k=6;x=0.2f;y=0.244f;w=0.7f;h=0.512f;
                inX = new float[]{0.396f,0.683f,0.396f,0.683f,0.396f,0.683f};
                inY = new float[]{0.283f,0.283f,0.402f,0.402f,0.505f,0.505f};
                inW = new float[]{0.251f,0.198f,0.251f,0.198f,0.251f,0.198f};
                inH = new float[]{0.108f,0.111f,0.095f,0.095f,0.095f,0.095f};
                break;
            case 14:
                k=1;x=0.1f;y=0.1f;w=0.8f;h=0.8f;
                inX = new float[]{0.372f};
                inY = new float[]{0.604f};
                inW = new float[]{0.328f};
                inH = new float[]{0.24f};
                break;
            case 15:
                k=1;x=0.1f;y=0.1f;w=0.8f;h=0.8f;
                inX = new float[]{0.372f};
                inY = new float[]{0.604f};
                inW = new float[]{0.328f};
                inH = new float[]{0.24f};
                break;
            default:
                k=0;x=0.0f;y=0.0f;w=0.0f;h=0.0f;
                inX = null;
                inY = null;
                inW = null;
                inH = null;
                Log.v("D","请选择仪器");
                Toast.makeText(getApplicationContext(),"不存在该仪器",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // 删除文件
    public boolean delete(String SDPATH,String fileName) {

        //SDPATH目录路径，fileName文件名

        File file = new File(SDPATH + "/" + fileName);
        if (file == null || !file.exists() || file.isDirectory()){
            return false;
        }
        file.delete();

        return true;
    }

    // 删除文件夹
    public boolean deleteDirectory(String filePath) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }


    // 创建一张底图，输入roi，对roi进行边缘填充
    public static Bitmap pureColorPicture(Bitmap bitmap)
    {
        // 创建一张新图
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width+100,height+100,Bitmap.Config.ARGB_8888);
        newBitmap.eraseColor(Color.parseColor("#FF0000"));

        // 把创建的位图作为画板
        Canvas mCanvas = new Canvas(newBitmap);
        mCanvas.drawBitmap(bitmap,50,50,null);
        return newBitmap;
    }

    // 保存图片
    private String saveBitmap(Bitmap bm) {
        String strPathFront = Environment.getExternalStorageDirectory().toString()+"/newtemp.jpg";
        try {
            FileOutputStream saveImgOut = new FileOutputStream(strPathFront);
            // compress - 压缩的意思
            bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
            //存储完成后需要清除相关的进程
            saveImgOut.flush();
            saveImgOut.close();
            Log.d("Save New Bitmap", "The picture is save to your phone!");
        } catch (IOException ex) {
            ex.printStackTrace();
            strPathFront = "";
        }finally {
            return strPathFront;
        }
    }



    //封装bitmap，显示识别结果
    private void showObjects(YoloV5Ncnn.Obj[] objects) {
        if (objects == null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        // draw objects on bitmap
        Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        final int[] colors = new int[]{
                Color.rgb(54, 67, 244),
                Color.rgb(99, 30, 233),
                Color.rgb(176, 39, 156),
                Color.rgb(183, 58, 103),
                Color.rgb(181, 81, 63),
                Color.rgb(243, 150, 33),
                Color.rgb(244, 169, 3),
                Color.rgb(212, 188, 0),
                Color.rgb(136, 150, 0),
                Color.rgb(80, 175, 76),
                Color.rgb(74, 195, 139),
                Color.rgb(57, 220, 205),
                Color.rgb(59, 235, 255),
                Color.rgb(7, 193, 255),
                Color.rgb(0, 152, 255),
                Color.rgb(34, 87, 255),
                Color.rgb(72, 85, 121),
                Color.rgb(158, 158, 158),
                Color.rgb(139, 125, 96)
        };

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(26);
        textpaint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < objects.length; i++) {
            paint.setColor(colors[i % 19]);

            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // draw filled text inside image
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";

                float text_width = textpaint.measureText(text);
                float text_height = -textpaint.ascent() + textpaint.descent();

                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        imageView.setImageBitmap(rgba);
    }

    //封装bitmap，显示识别结果
    private void showObjects(YoloV5Ncnn.Obj[] objects,Canvas canvas, Bitmap rgba,int index) {
        if (objects == null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        // draw objects on bitmap
        //Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        //int rgbaWidth = rgba.getWidth();
        //int rgbaHeight = rgba.getHeight();
        //imageView.setImageBitmap(rgba);
        //Log.v("TAG","rbga:"+rgba.getWidth()+"  rgbaHeight:"+rgba.getHeight());

        final int[] colors = new int[]{
                Color.rgb(54, 67, 244),
                Color.rgb(99, 30, 233),
                Color.rgb(176, 39, 156),
                Color.rgb(183, 58, 103),
                Color.rgb(181, 81, 63),
                Color.rgb(243, 150, 33),
                Color.rgb(244, 169, 3),
                Color.rgb(212, 188, 0),
                Color.rgb(136, 150, 0),
                Color.rgb(80, 175, 76),
                Color.rgb(74, 195, 139),
                Color.rgb(57, 220, 205),
                Color.rgb(59, 235, 255),
                Color.rgb(7, 193, 255),
                Color.rgb(0, 152, 255),
                Color.rgb(34, 87, 255),
                Color.rgb(72, 85, 121),
                Color.rgb(158, 158, 158),
                Color.rgb(139, 125, 96)
        };

        //Canvas canvas = new Canvas(rgba);
        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        //Log.v("TAG","viweWidth:"+viewWidth+"  viewHeight:"+viewHeight);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(26);
        textpaint.setTextAlign(Paint.Align.LEFT);


        Paint mtextpaint = new Paint();
        mtextpaint.setColor(Color.RED);
        mtextpaint.setTextSize(100);
        mtextpaint.setTextAlign(Paint.Align.LEFT);

//        float scale = bitmap.getWidth()*1.0f/ imageView.getWidth();
//        int exWidth = (int)((inX[index]-x)*bitmap.getWidth()*scale);
//        int exHeigh = (int)((inY[index]-y)*bitmap.getHeight()*scale);
//        int exWidth = 0;
//        int exHeigh = 0;

        for (int i = 0; i < objects.length; i++) {
//            paint.setColor(colors[i % 19]);
//
//            canvas.drawRect(objects[i].x+exWidth, objects[i].y+exHeigh, objects[i].x + objects[i].w+exWidth, objects[i].y + objects[i].h+exHeigh, paint);
//
//            // draw filled text inside image
//            {
////                    String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";
//                String text = objects[i].label;
//
//                float text_width = textpaint.measureText(text);
//                float text_height = -textpaint.ascent() + textpaint.descent();
//
//                float x = objects[i].x+exWidth;
//                float y = objects[i].y - text_height+exHeigh;
//                if (y < 0)
//                    y = 0;
//                if (x + text_width+exWidth > rgba.getWidth())
//                    x = rgba.getWidth() - text_width+exWidth;
//
//                canvas.drawRect(x+exWidth, y+exHeigh, x + text_width+exWidth, y + text_height+exHeigh, textbgpaint);
//
//                canvas.drawText(text, x+exWidth, y - textpaint.ascent()+exHeigh, textpaint);
//            }

            // 显示结果
            String text = objects[i].label;
            int exWidth1 = (int)((inX[index]-0.05)*bitmap.getWidth());
            int exHeigh1 = (int)((inY[index])*bitmap.getHeight());
//            int exWidth1 = 0;
//            int exHeigh1 = 0;
            float text_width = mtextpaint.measureText(text);
            float text_height = -mtextpaint.ascent() + mtextpaint.descent();
            float x = objects[i].x+exWidth1;
            float y = objects[i].y - text_height+exHeigh1;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width+exWidth1;
            canvas.drawText(text, x, y - mtextpaint.ascent(), mtextpaint);
        }

        imageView.setImageBitmap(rgba);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //拍照回调
        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            try {
                if (requestCode == SELECT_IMAGE) {
                    // 用来识别bitmap
                    bitmap = decodeUri(selectedImage);
                    yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    imageView.setImageBitmap(bitmap);
                    FLAG=0;
                }
            } catch (FileNotFoundException e) {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
        //接收result返回的byte
        if (resultCode == RESULT_OK && requestCode == requestCodeCam) {
            String filePath = data.getStringExtra("imgPath");
            Log.v("MainActivity","filePath:"+filePath);
            bitmap = BitmapFactory.decodeFile(filePath);
            //yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageView.setImageBitmap(bitmap);
            FLAG = 1;

//            //bitmap 转 uri
//            try {
//                Uri selectedImage = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
//                bitmap = decodeUri(selectedImage);
//                yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//                imageView.setImageBitmap(bitmap);
//            } catch (FileNotFoundException e){
//                Log.e("MainActivity", "FileNotFoundException");
//                return;
//            }


            //Bundle roidata = data.getBundleExtra("roiPath");

            // roi
//            for(int i=0;i<k;++i){
//                //String roiPath = data.getStringExtra("roiPath"+i);
//                String roiPath = roidata.getString("roiPath"+i);
//                Log.v("MainActivity","roiPath:"+roiPath);
//                roibitmap = BitmapFactory.decodeFile(roiPath);
//                yourSelectedImage = roibitmap.copy(Bitmap.Config.ARGB_8888, true);
//                imageView.setImageBitmap(roibitmap);
////                try {
////                    Uri selectedImage = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
////                    bitmap = decodeUri(selectedImage);
////                    yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
////                    //imageView.setImageBitmap(bitmap);
////                } catch (FileNotFoundException e){
////                    Log.e("MainActivity", "FileNotFoundException");
////                    return;
////                }
//
//            }

            // bitmap是屏幕图片，需要根据仪器确定roi区域，yourSelectedImage = roi区域
            // 若有多个roi区域，采用遍历的方法，逐个令yourSelectedImage = roi区域
            //yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            //imageView.setImageBitmap(bitmap);  //显示预览图
//            byte[] imgData = data.getByteArrayExtra("imgByte");
//            bitmap = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);  //转bitmap
//            yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//            imageView.setImageBitmap(bitmap);  //显示预览图
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 640;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

        // Rotate according to EXIF
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(selectedImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (IOException e) {
            Log.e("MainActivity", "ExifInterface IOException");
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}
