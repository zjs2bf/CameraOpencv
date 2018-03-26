package com.example.song.cameraopencv;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private String TAG = "CVSAMPLE";
    private Button takePic,takeGallery,defectDetection;
    private File fileUri = new File(Environment.getExternalStorageDirectory().getPath() + "/photo.jpg");
    private File fileCropUri = new File(Environment.getExternalStorageDirectory().getPath() + "/crop_photo.jpg");
    private Uri imageUri;
    private Uri srcImageUri;
    private static final int CODE_GALLERY_REQUEST = 0xa0;
    private static final int CODE_CAMERA_REQUEST = 0xa1;
    private static final int CODE_RESULT_REQUEST = 0xa2;
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 0x03;
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 0x04;
    private int match_method;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePic = findViewById(R.id.takePic);
        takeGallery = findViewById(R.id.takeGallery);
        defectDetection = findViewById(R.id.defectDetection);
        takePic.setOnClickListener(click);
        takeGallery.setOnClickListener(click);
        defectDetection.setOnClickListener(click);
        initLoadOpenCVLibs();//加载opencv库

    }

    private View.OnClickListener click=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent=null;
            switch (v.getId()){
                case R.id.takePic:
                    autoObtainCameraPermission();
                    break;
                case R.id.takeGallery:
                    autoObtainStoragePermission();
                    break;
                case R.id.defectDetection:
                    DefectDetection();
                    break;
                default:
            }
        }
    };

    /**
     * 自动获取相机权限
     * android6.0 以上的版本获取相机权限的方式
     */

    public void autoObtainCameraPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ToastUtils.showShort(this, "您已经拒绝过一次");
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_PERMISSIONS_REQUEST_CODE);
        } else {//有权限直接调用系统相机拍照
            if (hasSdcard()) {
                //通过FileProvider创建一个content类型的Uri
                imageUri = Uri.fromFile(fileUri);
                /*Android为我们提供了一个常量类build，其中最主要的是build中的两个内部类VERSION和VERSION_CODES,
                * VERSION表示当前系统版本信息，其中包括SDK的版本信息，用于成员SDK_INT表示；
                * VERSION_CODES其成员就是一些从最早版本到当前运行的系统的一些版本号常量，可直接用数字代替。
                * 比如下面的N is for Nougat,即N=24，下面这行代码的意思就是“大于等于24即为Android7.0以上执行的内容”*/
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //参数1 上下文，参数2 Provider主机地址，和配置文件manifest中的保持一致，参数3 共享的文件
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.song.cameraopencv.fileprovider", fileUri);
                }
                SysCameraActivity.takePicture(this, imageUri, CODE_CAMERA_REQUEST);
            } else {
                ToastUtils.showShort(this, "设备没有SD卡！");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            //调用系统相机申请拍照权限回调
            case CAMERA_PERMISSIONS_REQUEST_CODE: {
                /*如果权限请求被取消，则the result arrays are empty,即grantResults=0
                而此处的grantResults.length表示的是申请的权限的个数，如果同时申请两个权限，则值为2*/
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (hasSdcard()) {
                        imageUri = Uri.fromFile(fileUri);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            //通过FileProvider创建一个content类型的Uri
                            imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.song.cameraopencv.fileprovider", fileUri);
                        SysCameraActivity.takePicture(this, imageUri, CODE_CAMERA_REQUEST);
                    } else {
                        ToastUtils.showShort(this, "设备没有SD卡！");
                    }
                } else {

                    ToastUtils.showShort(this, "请允许打开相机！！");
                }
                break;


            }
            //调用系统相册申请Sdcard权限回调
            case STORAGE_PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SysCameraActivity.openPic(this, CODE_GALLERY_REQUEST);
                } else {

                    ToastUtils.showShort(this, "请允许打操作SDCard！！");
                }
                break;
            default:
        }
    }


//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == RESULT_OK) {
//            switch (requestCode) {
//                //拍照完成回调
//                case CODE_CAMERA_REQUEST:
//                    srcImageUri = Uri.fromFile(fileCropUri);
//                    SysCameraActivity.cropImageUri(this, imageUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, CODE_RESULT_REQUEST);
//                    break;
//                //访问相册完成回调
//                case CODE_GALLERY_REQUEST:
//                    if (hasSdcard()) {
//                        srcImageUri = Uri.fromFile(fileCropUri);
//                        Uri newUri = Uri.parse(SysCameraActivity.getPath(this, data.getData()));
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                            newUri = FileProvider.getUriForFile(this, "com.zz.fileprovider", new File(newUri.getPath()));
//                        }
//                        SysCameraActivity.cropImageUri(this, newUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, CODE_RESULT_REQUEST);
//                    } else {
//                        ToastUtils.showShort(this, "设备没有SD卡！");
//                    }
//                    break;
//                case CODE_RESULT_REQUEST:
//                    Bitmap bitmap = SysCameraActivity.getBitmapFromUri(cropImageUri, this);
//                    if (bitmap != null) {
//                        showImages(bitmap);
//                    }
//                    break;
//                default:
//            }
//        }
//    }

    /**
     * 自动获取sdk权限
     */

    private void autoObtainStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSIONS_REQUEST_CODE);
        } else {
            SysCameraActivity.openPic(this, CODE_GALLERY_REQUEST);
        }

    }

    public void DefectDetection() {
        //String src=imageUri.toString();
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inPreferredConfig= Bitmap.Config.ARGB_8888;
        //Bitmap bitmap = BitmapFactory.decodeFile(src, options);
        Bitmap bitmap2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.template, options);
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.src, options);
        Mat img = new Mat();
        Mat temp1 = new Mat();
        Utils.bitmapToMat(bitmap,img);
        Utils.bitmapToMat(bitmap2,temp1);


        //create the result matrix
        int result_cols = img.cols() - temp1.cols() + 1;
        int result_rows = img.rows() - temp1.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        //Do the Matching and Normalize
        Imgproc.matchTemplate(img, temp1, result,match_method);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        //Localizing the best match with minMaxLoc
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        Point matchLoc;
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
        }

        //show me what you got
        Imgproc.rectangle(img, matchLoc, new Point(matchLoc.x + temp1.cols(), matchLoc.y + temp1.rows()), new Scalar(0, 255, 0));
        Bitmap currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        Utils.matToBitmap(img,currentBitmap);
        ImageView imageView=this.findViewById(R.id.image_view);
        imageView.setImageBitmap(currentBitmap);

        //Save the visualized detection

//        System.out.println("Writing" + outFile);
//        Imgcodecs.imwrite(outFile, img);
    }

    /**
     * 检测opencv库是否导入成功
     */
    private void initLoadOpenCVLibs() {
        boolean success = OpenCVLoader.initDebug();
        if (success) {
            Log.i(TAG, " 类导入成功");
        } else {
            Log.i(TAG, "类导入失败");
        }
    }



    /**
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

}
