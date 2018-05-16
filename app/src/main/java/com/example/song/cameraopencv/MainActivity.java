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
import android.widget.EditText;
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
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    private String TAG = "CVSAMPLE";
    private Button takePic,takeGallery,defectDetection,test;
    private File fileUri = new File(Environment.getExternalStorageDirectory().getPath() + "/photo.jpg");
    private File fileCropUri = new File(Environment.getExternalStorageDirectory().getPath() + "/crop.jpg");
    private File fileDetecUri = new File(Environment.getExternalStorageDirectory().getPath() + "/detec.jpg");
    private Uri imageUri;
    private Uri detecImageUri;
    private Uri cropImageUri;
    private static final int CODE_GALLERY_REQUEST = 0xa0;
    private static final int CODE_CAMERA_REQUEST = 0xa1;
    private static final int CODE_RESULT_REQUEST = 0xa2;
    private static final int CODE_DETEC_REQUEST=0xa3;
    private static final int CODE_RESULT2_REQUEST=0xa4;
    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 0x03;
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 0x04;
    private ImageView template_image;
    private ImageView detec_image;
    private ImageView compare_image;
    private int match_method;
    private EditText editText;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePic = findViewById(R.id.takePic);
        takeGallery = findViewById(R.id.takeGallery);
        test = findViewById(R.id.test);
        defectDetection = findViewById(R.id.defectDetection);
        template_image = findViewById(R.id.template_image);
        detec_image = findViewById(R.id.detec_image);
        compare_image = findViewById(R.id.compare_image);
        editText = findViewById(R.id.threshold);
        takePic.setOnClickListener(click);
        takeGallery.setOnClickListener(click);
        test.setOnClickListener(click);
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
                case R.id.test:
                    test();
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

    /**
     * 调用系统相机拍照
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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

    private int OUTPUT_X=600;
    private int OUTPUT_Y=600;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                //拍照完成回调
                case CODE_CAMERA_REQUEST:
                    cropImageUri = Uri.fromFile(fileCropUri);
                    SysCameraActivity.cropImageUri(this, imageUri, cropImageUri, 9998, 9999, OUTPUT_X, OUTPUT_Y, CODE_RESULT_REQUEST);
//                    cropImageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.song.cameraopencv.fileprovider", fileCropUri);
                    break;
                //检测按钮拍照完成回调
                case CODE_DETEC_REQUEST:
                    detecImageUri = Uri.fromFile(fileDetecUri);
                    SysCameraActivity.cropImageUri(this, imageUri, detecImageUri, 9998, 9999, OUTPUT_X, OUTPUT_Y, CODE_RESULT2_REQUEST);
//                    detecImageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.song.cameraopencv.fileprovider", fileDetecUri);
                    break;

                //访问相册完成回调
                case CODE_GALLERY_REQUEST:
                    if (hasSdcard()) {
                        cropImageUri = Uri.fromFile(fileCropUri);
                        Uri newUri = Uri.parse(SysCameraActivity.getPath(this, data.getData()));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            newUri = FileProvider.getUriForFile(this, "com.example.song.cameraopencv.fileprovider", new File(newUri.getPath()));
                        }
                        SysCameraActivity.cropImageUri(this, newUri, cropImageUri, 1, 1, OUTPUT_X, OUTPUT_Y, CODE_RESULT_REQUEST);
                    } else {
                        ToastUtils.showShort(this, "设备没有SD卡！");
                    }
                    break;
//                case CODE_RESULT_REQUEST:
//                    Bitmap bitmap = SysCameraActivity.getBitmapFromUri(cropImageUri, this);
//                    if (bitmap != null) {
//                        showImages(bitmap);
//                    }
//                    break;
                default:
            }
        }
    }

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
        imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.song.cameraopencv.fileprovider", fileUri);
        SysCameraActivity.takePicture(this, imageUri, CODE_DETEC_REQUEST);

    }

    /**
     * 测试图像灰度，二值化，缺陷面积检测
     */
    private void test() {
        Mat src = new Mat();
        Mat src2 = new Mat();
        Mat graySrc = new Mat();
        Mat binarySrc = new Mat();
        Mat dst = new Mat();
        Mat dst2 = new Mat();
        Mat grayDst = new Mat();
        Mat binaryDst = new Mat();
        Mat diffMat = new Mat();
        String threshold=editText.getText().toString();
        int th = Integer.parseInt(threshold);
        String org = Environment.getExternalStorageDirectory().getPath() + "/crop.jpg";
        String detec = Environment.getExternalStorageDirectory().getPath() + "/detec.jpg";
        Bitmap orgBitmap = BitmapFactory.decodeFile(org);
        Bitmap detecBitmap = BitmapFactory.decodeFile(detec);
        Bitmap currentBitmap1 = Bitmap.createBitmap(orgBitmap);
//        Bitmap currentBitmap2 = Bitmap.createBitmap(detecBitmap);
//        Bitmap currentBitmap3 = Bitmap.createBitmap(orgBitmap);
        Utils.bitmapToMat(orgBitmap,src);
        Utils.bitmapToMat(detecBitmap,dst);
        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGRA2BGR);//这个转换很重要，不然下面双边滤波会报格式错误~
        Imgproc.bilateralFilter(src,src2,30,30*2,30/2);
        Imgproc.cvtColor(src2,graySrc,Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(graySrc, binarySrc, th, 255, Imgproc.THRESH_BINARY);
        Imgproc.cvtColor(dst,dst,Imgproc.COLOR_BGRA2BGR);
        Imgproc.bilateralFilter(dst,dst2,30,30*2,30/2);
        Imgproc.cvtColor(dst2,grayDst,Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(grayDst, binaryDst, th, 255, Imgproc.THRESH_BINARY);
        //将二值化后的图片相减，用的不是Imgproc，而是Core。
        Core.absdiff(binaryDst,binarySrc,diffMat);
        Utils.matToBitmap(diffMat,currentBitmap1);
//        Utils.matToBitmap(binarySrc,currentBitmap2);
//        Utils.matToBitmap(binaryDst,currentBitmap3);
//        detec_image.setImageBitmap(currentBitmap3);
//        template_image.setImageBitmap(currentBitmap2);

        compare_image.setImageBitmap(currentBitmap1);
//        template_image.setImageBitmap(orgBitmap);

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
