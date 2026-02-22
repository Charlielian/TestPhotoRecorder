package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.signal.test.services.SignalCollector;
import com.signal.test.services.LocationService;
import com.signal.test.models.SignalData;
import com.signal.test.utils.CameraUtils;
import com.signal.test.utils.StorageUtils;
import com.signal.test.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private Button btnCapture, btnBack, btnGallery;
    private TextView overlayOperator, overlayCgi, overlayFrequency, overlayBand, overlayRssi;
    
    private ImageCapture imageCapture;
    private SignalCollector signalCollector;
    private LocationService locationService;
    private CameraUtils cameraUtils;
    private StorageUtils storageUtils;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        initViews();
        initServices();
        startCamera();
        updateOverlayInfo();
    }
    
    private void initViews() {
        viewFinder = findViewById(R.id.view_finder);
        btnCapture = findViewById(R.id.btn_capture);
        btnBack = findViewById(R.id.btn_back);
        btnGallery = findViewById(R.id.btn_gallery);
        overlayOperator = findViewById(R.id.overlay_operator);
        overlayCgi = findViewById(R.id.overlay_cgi);
        overlayFrequency = findViewById(R.id.overlay_frequency);
        overlayBand = findViewById(R.id.overlay_band);
        overlayRssi = findViewById(R.id.overlay_rssi);
        
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnBack.setOnClickListener(v -> finish());
        btnGallery.setOnClickListener(v -> openGallery());
    }
    
    private void initServices() {
        signalCollector = new SignalCollector(this);
        locationService = new LocationService(this);
        cameraUtils = new CameraUtils(this);
        storageUtils = new StorageUtils(this);
        locationService.startLocationUpdates();
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // 清除之前的绑定
                cameraProvider.unbindAll();
                
                // 设置预览
                Preview preview = new Preview.Builder()
                        .setTargetResolution(new android.util.Size(1920, 1080))
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                
                // 设置图像捕获
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetResolution(new android.util.Size(4032, 3024)) // 高分辨率
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                        .build();
                
                // 选择后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                // 绑定生命周期
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "相机未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取信号数据
        SignalData signalData = signalCollector.getSignalData();
        
        // 更新位置信息
        signalData.setLocation(locationService.getLocationDescription());
        // 记录经纬度
        android.location.Location location = locationService.getCurrentLocation();
        if (location != null) {
            signalData.setLatitude(location.getLatitude());
            signalData.setLongitude(location.getLongitude());
        }
        signalData.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        // 创建输出文件
        File outputFile = new File(cameraUtils.getPhotoDirectory(), "SIGNAL_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");
        
        // 创建图像捕获输出选项
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(outputFile).build();
        
        // 创建图像捕获回调
        ImageCapture.OnImageSavedCallback callback = new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                try {
                    // 读取保存的图片
                    Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                    
                    // 叠加信号信息并重新保存
                    String photoPath = cameraUtils.savePhotoWithSignalInfo(bitmap, signalData);
                    
                    if (photoPath != null) {
                        // 删除原始图片
                        outputFile.delete();
                        
                        // 保存信号数据到数据库
                        signalData.setPhotoPath(photoPath);
                        storageUtils.insertSignalData(signalData);
                        
                        Toast.makeText(CameraActivity.this, R.string.msg_capture_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CameraActivity.this, R.string.msg_capture_failed, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CameraActivity.this, R.string.msg_capture_failed, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(ImageCaptureException exception) {
                exception.printStackTrace();
                Toast.makeText(CameraActivity.this, R.string.msg_capture_failed, Toast.LENGTH_SHORT).show();
            }
        };
        
        // 捕获照片
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), callback);
    }
    
    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    
    private void updateOverlayInfo() {
        SignalData data = signalCollector.getSignalData();
        overlayOperator.setText("运营商: " + data.getOperator());
        overlayCgi.setText("CGI: " + data.getCgi());
        overlayFrequency.setText("频点: " + data.getFrequency());
        overlayBand.setText("频段: " + data.getBand());
        
        // 根据网络类型显示不同的信号强度参数
        if (data.getNetworkType().equals("5G")) {
            overlayRssi.setText("RSRP: " + (data.getRsrp() != 0 ? data.getRsrp() + " dBm" : "N/A"));
        } else {
            overlayRssi.setText("电平: " + data.getRssi() + " dBm");
        }
    }
    
    private void openGallery() {
        // 打开相册
        File directory = cameraUtils.getPhotoDirectory();
        if (directory.exists() && directory.listFiles() != null && directory.listFiles().length > 0) {
            // 这里可以实现打开相册的逻辑
            Toast.makeText(this, "相册功能开发中", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "暂无照片", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        locationService.startLocationUpdates();
        updateOverlayInfo();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        locationService.stopLocationUpdates();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        storageUtils.close();
    }
}