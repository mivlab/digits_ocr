package com.tencent.yolov5ncnn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.functions.Consumer;

public class MainActivityNew extends FragmentActivity {

    private SurfaceView surfaceview;
    private Camera camera;
    private Button take;
    CropView cropView;
    private SurfaceHolder holder;
    private View frameView;

    private int picid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 没有标题  必须在设置布局之前找到调用
        setContentView(R.layout.activity_main);
        frameView = findViewById(R.id.frame);
        //拍照按钮
        take = (Button) findViewById(R.id.take);
        //拍照显示模块
        surfaceview = (SurfaceView) findViewById(R.id.surfaceview);
        holder = surfaceview.getHolder();
//        holder.setFixedSize(176, 155);// 设置分辨率
        holder.setKeepScreenOn(true);  //竖屏
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new MySurfaceCallback());

        // SurfaceView只有当activity显示到了前台，该控件才会被创建     因此需要监听surfaceview的创建

        //拍照按钮
        take.setOnClickListener(view -> {
            takepicture();  //拍照
        });

        //第二个拍照
//        findViewById(R.id.second).setOnClickListener(view -> startActivity(new Intent(MainActivityNew.this, RectCameraActivity.class)));

        cropView = findViewById(R.id.vw_crop);
        Intent lastIntent = getIntent();
        if(lastIntent!=null) {
            LocationParam param = lastIntent.getParcelableExtra("data");
            cropView.setLocationParam(param);
        }
    }


    //点击事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //对焦
        if (camera != null) camera.autoFocus((b, camera) -> camera.cancelAutoFocus());
        return super.onTouchEvent(event);
    }

    /**
     * 监听surfaceview的创建
     *
     * @author Administrator
     * Surfaceview只有当activity显示到前台，该空间才会被创建
     */
    private final class MySurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
            try {
                //摄像头画面显示在Surface上
                camera.setPreviewDisplay(surfaceview.getHolder());
                updateCameraParameters();
                camera.startPreview();
            } catch (IOException e) {
                if (camera != null) camera.release();
                camera = null;
            }
        }


        @SuppressLint("CheckResult")
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub

            try {
                // 当surfaceview创建就去打开相机
                camera = Camera.open();
                camera.setPreviewDisplay(surfaceview.getHolder());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                if(camera!=null){
                    camera.release();
                    camera = null;
                }
                e.printStackTrace();
            }
            if(camera==null){
                finish();
                return;
            }
            updateCameraParameters();
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            if (camera != null) {  //释放相机
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        }

    }

    private void updateCameraParameters() {
        if (camera != null) {
            Camera.Parameters p = camera.getParameters();
            List<String> listScene = p.getSupportedSceneModes();
//            long time = new Date().getTime();
//            p.setGpsTimestamp(time);
            Camera.Size pictureSize = findBestPictureSize(p);
            p.setPictureSize(pictureSize.width, pictureSize.height);
            // Set the preview frame aspect ratio according to the picture size.
            Camera.Size size = p.getPictureSize();
//            PreviewFrameLayout frameLayout = (PreviewFrameLayout) findViewById(R.id.frame_layout);
//            frameLayout.setAspectRatio((double) size.width / size.height);
            Camera.Size previewSize = findBestPreviewSize(p);
            p.setPreviewSize(previewSize.width,previewSize.height);
            camera.setParameters(p);
            int supportPreviewWidth = previewSize.width;
            int supportPreviewHeight = previewSize.height;
            int srcWidth = frameView.getWidth();
            int srcHeight = frameView.getHeight();
//            int srcWidth = getScreenWH().widthPixels;
//            int srcHeight = getScreenWH().heightPixels;
            int width = Math.min(srcWidth, srcHeight);
            int height = width * supportPreviewWidth / supportPreviewHeight ;
            FrameLayout.LayoutParams surfaceLayoutParam = (FrameLayout.LayoutParams) surfaceview.getLayoutParams();
            surfaceLayoutParam.width = height;
            surfaceLayoutParam.height = width;
            surfaceview.setLayoutParams(surfaceLayoutParam);//
            FrameLayout.LayoutParams cropLayoutParam = (FrameLayout.LayoutParams) cropView.getLayoutParams();
            cropLayoutParam.width = height;
            cropLayoutParam.height = width;
            cropView.setLayoutParams(cropLayoutParam);
        }
    }
    private Camera.Size findBestPictureSize(Camera.Parameters parameters) {
        int  diff = Integer.MIN_VALUE;
        String pictureSizeValueString = parameters.get("picture-size-values");
        // saw this on Xperia
        if (pictureSizeValueString == null) {
            pictureSizeValueString = parameters.get("picture-size-value");
        }
        if(pictureSizeValueString == null) {
            return  camera.new Size(getScreenWH().widthPixels,getScreenWH().heightPixels);
        }
        Log.d("tag", "pictureSizeValueString : " + pictureSizeValueString);
        int bestX = 0;
        int bestY = 0;
        for(String pictureSizeString : (pictureSizeValueString.split(",")))
        {
            pictureSizeString = pictureSizeString.trim();
            int dimPosition = pictureSizeString.indexOf('x');
            if(dimPosition == -1){
                continue;
            }
            int newX = 0;
            int newY = 0;
            try{
                newX = Integer.parseInt(pictureSizeString.substring(0, dimPosition));
                newY = Integer.parseInt(pictureSizeString.substring(dimPosition+1));
            }catch(NumberFormatException e){
                continue;
            }
            Point screenResolution = new Point (getScreenWH().widthPixels,getScreenWH().heightPixels);
            int newDiff = Math.abs(newX - screenResolution.x)+Math.abs(newY- screenResolution.y);
            if(newDiff == diff)
            {
                bestX = newX;
                bestY = newY;
                break;
            } else if(newDiff > diff){
                if((3 * newX) == (4 * newY)) {
                    bestX = newX;
                    bestY = newY;
                    diff = newDiff;
                }
            }
        }
        if (bestX > 0 && bestY > 0) {
            return camera.new Size(bestX, bestY);
        }
        return null;
    }
    private Camera.Size findBestPreviewSize(Camera.Parameters parameters) {
        String previewSizeValueString = null;
        int diff = Integer.MAX_VALUE;
        previewSizeValueString = parameters.get("preview-size-values");
        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }
        if(previewSizeValueString == null) {  // 有些手机例如m9获取不到支持的预览大小   就直接返回屏幕大小
            return  camera.new Size(getScreenWH().widthPixels,getScreenWH().heightPixels);
        }
        Log.d("tag", "previewSizeValueString : " + previewSizeValueString);
        int bestX = 0;
        int bestY = 0;
        for(String prewsizeString : previewSizeValueString.split(","))
        {
            prewsizeString = prewsizeString.trim();
            int dimPosition = prewsizeString.indexOf('x');
            if(dimPosition == -1){
                continue;
            }
            int newX = 0;
            int newY = 0;
            try{
                newX = Integer.parseInt(prewsizeString.substring(0, dimPosition));
                newY = Integer.parseInt(prewsizeString.substring(dimPosition+1));
            }catch(NumberFormatException e){
                continue;
            }
            Point screenResolution = new Point (getScreenWH().widthPixels,getScreenWH().heightPixels);
            int newDiff = Math.abs(newX - screenResolution.x)+Math.abs(newY- screenResolution.y);
            if(newDiff == diff)
            {
                bestX = newX;
                bestY = newY;
                break;
            } else if(newDiff < diff){
                if((3 * newX) == (4 * newY)) {
                    bestX = newX;
                    bestY = newY;
                    diff = newDiff;
                }
            }
        }
        if (bestX > 0 && bestY > 0) {
            return camera.new Size(bestX, bestY);
        }
        return null;
    }
    protected DisplayMetrics getScreenWH() {
        DisplayMetrics dMetrics = new DisplayMetrics();
        dMetrics = this.getResources().getDisplayMetrics();
        return dMetrics;
    }

    //拍照的函数
    public void takepicture() {
        /*
         * shutter:快门被按下
         * raw:相机所捕获的原始数据
         * jpeg:相机处理的数据
         */
        if (camera != null) camera.takePicture(null, null, new MyPictureCallback());
    }

    //byte转Bitmap
    public Bitmap Bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    //bitmap转byte
    public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    //照片回调函数，其实是处理照片的
    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            try {
                //byte转bitmap
                // 原图
                Bitmap bitmap = Bytes2Bimap(data);
                Matrix m = new Matrix();
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                m.setRotate(90);
                //将照片右旋90度
//                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);

                Log.d("TAG", "width " + width);
                Log.d("TAG", "height " + height);
                //Toast.makeText(getApplicationContext(),"width:"+width+" height:"+height,Toast.LENGTH_SHORT).show();

                //截取透明框内照片
//                bitmap = scaleBitmap(bitmap,0.1f);
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                //Log.v("v","111width:"+width+" 111height:"+height);
                //Toast.makeText(getApplicationContext(),"111width:"+width+" 111height:"+height,Toast.LENGTH_SHORT).show();

//                int nXPos = 50;
//                int nYPos = (int) (height*0.3f);
//                int nNewHeight = (int)(height*0.4f);
//                int nNewWidth = (int) (nNewHeight*1.5f);
                Rect cropRect = cropView.getCropRect(width,height); //new Rect();
                int k = cropView.getNum();
                Intent intent = new Intent();
                Bundle roidata = new Bundle();
                for(int i=0;i<k;++i){
                    Rect roiRect = cropView.getNumRect(width,height,i);
                    //Toast.makeText(getApplicationContext(),"i:"+i+"roiwidth:"+(roiRect.right-roiRect.left),Toast.LENGTH_SHORT).show();
                    Bitmap roiBitMap = Bitmap.createBitmap(bitmap, roiRect.left,roiRect.top,
                            roiRect.right-roiRect.left,roiRect.bottom-roiRect.top);
                    // resize
                    roiBitMap = Bitmap.createScaledBitmap(roiBitMap,(int)roiBitMap.getWidth()/2,(int)roiBitMap.getHeight()/2,true);

                    //Bitmap newRoiMap = pureColorPicture(roiBitMap);
                    roiBitMap = pureColorPicture(roiBitMap);

                    Log.v("d","roi width:"+roiBitMap.getWidth()+" roi height:"+roiBitMap.getHeight());
                    //Log.v("d","new roi width:"+newRoiMap.getWidth()+" new roi height:"+newRoiMap.getHeight());

                    String roiPath = saveRoimap(roiBitMap,i);

                    //保存训练数据

                    //String savaPath = saveRoimapData(roiBitMap,i);
                    //picid+=1;

                    //String newRoiPath = saveRoimap(newRoiMap,i+10);

                    roidata.putString("roiPath"+i,roiPath);
                    intent.putExtra("roiPath",roidata);

                    Log.v("roi","roi path:"+roiPath);

//                    Intent intent = new Intent();
//                    intent.putExtra("roiPath"+i, roiPath);
////                intent.putExtra("imgByte", data);
//                    setResult(RESULT_OK, intent);
//                    finish();
                }
                Log.v("d","k:"+k);

                //String filePath = saveBitmap(bitmap);

                bitmap = Bitmap.createBitmap(bitmap, cropRect.left,cropRect.top,
                        cropRect.right-cropRect.left,cropRect.bottom-cropRect.top);
//                bitmap = Bitmap.createBitmap(bitmap, 50, 250, 650, 500);

                width = bitmap.getWidth();
                height = bitmap.getHeight();
                Log.v("v","width:"+width+" height:"+height);
                //Toast.makeText(getApplicationContext(),"width:"+width+" height:"+height,Toast.LENGTH_SHORT).show();

                String filePath = saveBitmap(bitmap);
                //把数据返回给首界面
//                data = Bitmap2Bytes(bitmap);
//                Intent intent = new Intent();
                intent.putExtra("imgPath", filePath);
//                intent.putExtra("imgByte", data);
                setResult(RESULT_OK, intent);
                finish();
//                File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(data);
                // 在拍照的时候相机是被占用的,拍照之后需要重新预览
//                camera.startPreview();
                //   Toast.makeText(MainActivityNew.this, "拍照截取成功，本地相册查看", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private String saveBitmap(Bitmap bm) {
        String strPathFront = Environment.getExternalStorageDirectory().toString()+"/temp.jpg";
        try {
            FileOutputStream saveImgOut = new FileOutputStream(strPathFront);
            // compress - 压缩的意思
            bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
            //存储完成后需要清除相关的进程
            saveImgOut.flush();
            saveImgOut.close();
            Log.d("Save Bitmap", "The picture is save to your phone!");
        } catch (IOException ex) {
            ex.printStackTrace();
            strPathFront = "";
        }finally {
            return strPathFront;
        }
    }

    private String saveRoimap(Bitmap bm, int k){
        String strPathFront = Environment.getExternalStorageDirectory().toString()+"/"+k+".jpg";
        try {
            FileOutputStream saveImgOut = new FileOutputStream(strPathFront);
            // compress - 压缩的意思
            bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
            //存储完成后需要清除相关的进程
            saveImgOut.flush();
            saveImgOut.close();
            Log.d("Save Roi", "The roi picture is save to your phone!");
        } catch (IOException ex) {
            ex.printStackTrace();
            strPathFront = "";
        }finally {
            return strPathFront;
        }
    }

    // 保存至image
    private String saveRoimapData(Bitmap bm,int i){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "TrainData";
        File destDir = new File(path);
        if(!destDir.exists()) {
            destDir.mkdirs();
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date data = new Date();
        String str = null;
        str = format.format(data);
        String strPathFront = path+"/"+str+i+".jpg";
        try {
            FileOutputStream saveImgOut = new FileOutputStream(strPathFront);
            // compress - 压缩的意思
            bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
            //存储完成后需要清除相关的进程
            saveImgOut.flush();
            saveImgOut.close();
            Log.d("Save Roi", "The roi picture is save to your phone!");
        } catch (IOException ex) {
            ex.printStackTrace();
            strPathFront = "";
        }finally {
            return strPathFront;
        }
    }

    // 创建一张底图，输入roi，对roi进行边缘填充
    public static Bitmap pureColorPicture(Bitmap bitmap)
    {
        // 创建一张新图
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap((int)width*3/2,(int)height*2,Bitmap.Config.ARGB_8888);
        newBitmap.eraseColor(Color.parseColor("#C0C0C0"));// #FF0000 红色

        // 把创建的位图作为画板
        Canvas mCanvas = new Canvas(newBitmap);
        mCanvas.drawBitmap(bitmap,(int)width/4,(int)height/4,null);
        return newBitmap;
    }

    /**
     * 根据给定的宽和高进行拉伸
     *
     * @param origin    原图
     * @param newWidth  新图的宽
     * @param newHeight 新图的高
     * @return new Bitmap
     */
    private Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);// 使用后乘
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBM;
    }

    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    private Bitmap scaleBitmap(Bitmap origin, float ratio) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 裁剪
     *
     * @param bitmap 原图
     * @return 裁剪后的图像
     */
    private Bitmap cropBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int cropWidth = w >= h ? h : w;// 裁切后所取的正方形区域边长
        cropWidth /= 2;
        int cropHeight = (int) (cropWidth / 1.2);
        return Bitmap.createBitmap(bitmap, w / 3, 0, cropWidth, cropHeight, null, false);
    }

    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 偏移效果
     * @param origin 原图
     * @return 偏移后的bitmap
     */
    private Bitmap skewBitmap(Bitmap origin) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.postSkew(-0.6f, -0.3f);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

}


