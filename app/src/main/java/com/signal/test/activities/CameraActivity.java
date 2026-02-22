package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.signal.test.services.SignalCollector;
import com.signal.test.services.LocationService;
import com.signal.test.models.SignalData;
import com.signal.test.utils.CameraUtils;
import com.signal.test.utils.StorageUtils;
import com.signal.test.utils.CameraConfig;
import com.signal.test.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    
    private PreviewView viewFinder;
    private Button btnCapture, btnBack, btnGallery, btnSettings;
    private TextView overlayOperator, overlayCgi, overlayFrequency, overlayBand, overlayRssi;
    
    private ImageCapture imageCapture;
    private SignalCollector signalCollector;
    private LocationService locationService;
    private CameraUtils cameraUtils;
    private StorageUtils storageUtils;
    private CameraConfig cameraConfig;
    
    private ExecutorService cameraExecutor;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        cameraExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        cameraConfig = new CameraConfig();
        
        initViews();
        initServices();
        checkPermissions();
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
    
    private void openSettings() {
        // 这里可以打开设置界面，或者显示一个对话框来调整相机配置
        // 暂时使用Toast提示，后续可以实现完整的设置界面
        Toast.makeText(this, "设置功能开发中", Toast.LENGTH_SHORT).show();
    }
    
    private void initServices() {
        signalCollector = new SignalCollector(this);
        locationService = new LocationService(this);
        cameraUtils = new CameraUtils(this);
        storageUtils = new StorageUtils(this);
    }
    
    private void checkPermissions() {
        String[] permissions = new String[]{};
        
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions = addPermission(permissions, Manifest.permission.CAMERA);
        }
        
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions = addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions = addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (permissions.length > 0) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
            locationService.startLocationUpdates();
        }
    }
    
    private String[] addPermission(String[] permissions, String permission) {
        String[] newArray = new String[permissions.length + 1];
        System.arraycopy(permissions, 0, newArray, 0, permissions.length);
        newArray[permissions.length] = permission;
        return newArray;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            startCamera();
            locationService.startLocationUpdates();
        } else {
            Toast.makeText(this, "需要必要权限才能使用相机功能", Toast.LENGTH_LONG).show();
            finish();
        }
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
                        .setTargetResolution(cameraConfig.getPreviewResolution())
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                
                // 设置图像捕获
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(cameraConfig.getCaptureMode())
                        .setTargetResolution(cameraConfig.getCaptureResolution())
                        .setFlashMode(cameraConfig.getFlashMode())
                        .build();
                
                // 选择后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                // 绑定生命周期
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "相机未初始化", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 刷新信号数据
        signalCollector.refreshSignalData();
        SignalData signalData = signalCollector.getSignalData();
        
        // 获取最新位置信息
        Location location = locationService.getCurrentLocation();
        if (location != null) {
            signalData.setLatitude(location.getLatitude());
            signalData.setLongitude(location.getLongitude());
            signalData.setLocation(locationService.getLocationDescription());
        }
        signalData.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
        
        // 使用takePicture直接获取Image对象
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), 
            new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(ImageProxy imageProxy) {
                    processCapturedImage(imageProxy, signalData);
                }
                
                @Override
                public void onError(ImageCaptureException exception) {
                    exception.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, 
                        R.string.msg_capture_failed, Toast.LENGTH_SHORT).show());
                }
            });
    }
    
    private void processCapturedImage(ImageProxy imageProxy, SignalData signalData) {
        cameraExecutor.execute(() -> {
            try {
                // 将ImageProxy转换为Bitmap
                Bitmap originalBitmap = imageProxyToBitmap(imageProxy);
                
                if (originalBitmap != null) {
                    // 添加水印（包括经纬度）
                    Bitmap watermarkedBitmap = addWatermark(originalBitmap, signalData);
                    
                    // 保存带水印的照片
                    String photoPath = saveBitmapToFile(watermarkedBitmap, signalData);
                    
                    if (photoPath != null) {
                        // 保存信号数据到数据库
                        signalData.setPhotoPath(photoPath);
                        storageUtils.insertSignalData(signalData);
                        
                        mainHandler.post(() -> {
                            Toast.makeText(CameraActivity.this, 
                                R.string.msg_capture_success, Toast.LENGTH_SHORT).show();
                            updateOverlayInfo();
                        });
                    } else {
                        mainHandler.post(() -> Toast.makeText(CameraActivity.this, 
                            R.string.msg_capture_failed, Toast.LENGTH_SHORT).show());
                    }
                    
                    // 回收位图
                    if (!originalBitmap.isRecycled()) {
                        originalBitmap.recycle();
                    }
                    if (!watermarkedBitmap.isRecycled() && watermarkedBitmap != originalBitmap) {
                        watermarkedBitmap.recycle();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(CameraActivity.this, 
                    "照片处理失败", Toast.LENGTH_SHORT).show());
            } finally {
                // 重要：关闭ImageProxy释放资源
                imageProxy.close();
            }
        });
    }
    
    /**
     * 添加水印（包括经纬度、信号信息、时间戳）
     */
    private Bitmap addWatermark(Bitmap source, SignalData signalData) {
        // 创建可编辑的位图副本
        Bitmap watermarkBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), 
            Bitmap.Config.ARGB_8888);
        
        Canvas canvas = new Canvas(watermarkBitmap);
        // 绘制原图
        canvas.drawBitmap(source, 0, 0, null);
        
        // 创建画笔
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(source.getWidth() * 0.035f); // 根据图片大小调整文字大小
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setShadowLayer(5f, 2f, 2f, Color.BLACK); // 添加阴影使文字更清晰
        
        // 设置半透明背景
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(150, 0, 0, 0)); // 半透明黑色
        bgPaint.setStyle(Paint.Style.FILL);
        
        // 计算文本位置
        int padding = source.getWidth() / 30;
        int lineHeight = (int)(source.getHeight() * 0.05f);
        int textY = padding + lineHeight;
        
        // 准备水印文本列表
        java.util.List<String> watermarkTexts = new java.util.ArrayList<>();
        
        // 添加时间戳
        if (cameraConfig.isEnableTimestamp()) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
            watermarkTexts.add(timestamp);
        }
        
        // 添加位置信息
        if (cameraConfig.isEnableLocation() && signalData.getLocation() != null) {
            watermarkTexts.add("位置: " + signalData.getLocation());
        }
        
        // 添加经纬度
        if (cameraConfig.isEnableCoordinates() && signalData.getLatitude() != 0 && signalData.getLongitude() != 0) {
            String coordinates = String.format(Locale.CHINA, "经纬度: %.6f, %.6f", 
                signalData.getLatitude(), signalData.getLongitude());
            watermarkTexts.add(coordinates);
        }
        
        // 添加信号信息
        if (cameraConfig.isEnableSignalInfo()) {
            String operator = "运营商: " + (signalData.getOperator() != null ? signalData.getOperator() : "未知");
            watermarkTexts.add(operator);
            
            String networkInfo = "";
            if (signalData.getNetworkType() != null && signalData.getNetworkType().equals("5G")) {
                networkInfo = String.format("5G | %s | %s | RSRP: %d dBm", 
                    signalData.getCgi() != null ? signalData.getCgi() : "未知",
                    signalData.getFrequency() != 0 ? signalData.getFrequency() : "未知",
                    signalData.getRsrp());
            } else {
                networkInfo = String.format("4G | %s | %s | RSSI: %d dBm", 
                    signalData.getCgi() != null ? signalData.getCgi() : "未知",
                    signalData.getFrequency() != 0 ? signalData.getFrequency() : "未知",
                    signalData.getRssi());
            }
            watermarkTexts.add(networkInfo);
        }
        
        // 计算背景高度
        int bgHeight = lineHeight * watermarkTexts.size() + padding * 2;
        
        // 根据水印位置设置文本起始位置
        switch (cameraConfig.getWatermarkPosition()) {
            case CameraConfig.WATERMARK_POSITION_TOP:
                // 顶部水印
                Rect topBgRect = new Rect(0, 0, source.getWidth(), bgHeight);
                canvas.drawRect(topBgRect, bgPaint);
                textY = padding + lineHeight;
                break;
            case CameraConfig.WATERMARK_POSITION_CUSTOM:
                // 自定义位置（暂时放在中间）
                int customY = source.getHeight() / 2 - bgHeight / 2;
                Rect customBgRect = new Rect(0, customY, source.getWidth(), customY + bgHeight);
                canvas.drawRect(customBgRect, bgPaint);
                textY = customY + padding + lineHeight;
                break;
            case CameraConfig.WATERMARK_POSITION_BOTTOM:
            default:
                // 底部水印
                Rect bottomBgRect = new Rect(0, source.getHeight() - bgHeight, source.getWidth(), source.getHeight());
                canvas.drawRect(bottomBgRect, bgPaint);
                textY = source.getHeight() - bgHeight + padding + lineHeight;
                break;
        }
        
        // 绘制水印文本
        for (String text : watermarkTexts) {
            canvas.drawText(text, padding, textY, paint);
            textY += lineHeight;
        }
        
        return watermarkBitmap;
    }
    
    /**
     * 保存位图到文件
     */
    private String saveBitmapToFile(Bitmap bitmap, SignalData signalData) {
        try {
            // 创建文件名（包含经纬度信息）
            String filename;
            if (signalData.getLatitude() != 0 && signalData.getLongitude() != 0) {
                filename = String.format(Locale.CHINA, "SIGNAL_%s_%.6f_%.6f.jpg",
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()),
                    signalData.getLatitude(),
                    signalData.getLongitude());
            } else {
                filename = "SIGNAL_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".jpg";
            }
            
            File photoDir = cameraUtils.getPhotoDirectory();
            if (!photoDir.exists()) {
                photoDir.mkdirs();
            }
            
            File outputFile = new File(photoDir, filename);
            
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, cameraConfig.getImageQuality(), out);
                out.flush();
            }
            
            return outputFile.getAbsolutePath();
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            mainHandler.post(() -> Toast.makeText(this, "获取图像失败", Toast.LENGTH_SHORT).show());
            return null;
        }
        
        Bitmap bitmap = null;
        
        try {
            // 根据图像格式处理
            if (image.getFormat() == ImageFormat.YUV_420_888 || 
                image.getFormat() == ImageFormat.NV21) {
                bitmap = yuv420ToBitmap(image);
            } else if (image.getFormat() == ImageFormat.JPEG) {
                bitmap = jpegToBitmap(image);
            } else {
                // 尝试使用通用方法处理其他格式
                bitmap = imageToBitmap(image);
            }
            
            // 如果需要旋转
            if (bitmap != null && imageProxy.getImageInfo().getRotationDegrees() != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 
                    bitmap.getHeight(), matrix, true);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() -> Toast.makeText(this, "图像处理失败", Toast.LENGTH_SHORT).show());
        }
        
        return bitmap;
    }
    
    private Bitmap yuv420ToBitmap(Image image) {
        // 优化的YUV转Bitmap方法
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        
        // 计算图像尺寸
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 分配NV21数据空间
        byte[] nv21 = new byte[ySize + uSize + vSize];
        
        // 复制Y数据
        yBuffer.get(nv21, 0, ySize);
        
        // 复制UV数据（转换为NV21格式）
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        
        // 使用YuvImage转换为JPEG再解码为Bitmap
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, 
            width, height, null);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 使用配置的图像质量
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 
                cameraConfig.getImageQuality(), out);
            
            byte[] jpegData = out.toByteArray();
            // 优化Bitmap解码
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private Bitmap jpegToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    
    private Bitmap imageToBitmap(Image image) {
        // 保留原有方法，但增强健壮性
        if (image.getFormat() == ImageFormat.JPEG) {
            return jpegToBitmap(image);
        } else {
            return yuv420ToBitmap(image);
        }
    }
    
    private void updateOverlayInfo() {
        SignalData data = signalCollector.getSignalData();
        
        runOnUiThread(() -> {
            overlayOperator.setText("运营商: " + (data.getOperator() != null ? 
                data.getOperator() : "未知"));
            overlayCgi.setText("CGI: " + (data.getCgi() != null ? 
                data.getCgi() : "未知"));
            overlayFrequency.setText("频点: " + (data.getFrequency() != 0 ? 
                data.getFrequency() : "未知"));
            overlayBand.setText("频段: " + (data.getBand() != null ? 
                data.getBand() : "未知"));
            
            // 添加经纬度显示到UI覆盖层
            if (data.getLatitude() != 0 && data.getLongitude() != 0) {
                // 如果有单独的经纬度显示控件可以在这里更新
                // 如果没有，可以添加到现有控件中
            }
            
            if (data.getNetworkType() != null && data.getNetworkType().equals("5G")) {
                overlayRssi.setText("RSRP: " + (data.getRsrp() != 0 ? 
                    data.getRsrp() + " dBm" : "N/A"));
            } else {
                overlayRssi.setText("电平: " + (data.getRssi() != 0 ? 
                    data.getRssi() + " dBm" : "N/A"));
            }
        });
    }
    
    private void openGallery() {
        // 如果项目中有GalleryActivity则使用，否则可以打开系统相册
        try {
            Intent intent = new Intent(this, Class.forName("com.signal.test.activities.GalleryActivity"));
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            // 打开系统相册
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            startActivity(intent);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (locationService != null) {
            locationService.startLocationUpdates();
        }
        updateOverlayInfo();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 关闭服务
        if (storageUtils != null) {
            storageUtils.close();
        }
        
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
        
        // 关闭线程池
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
        
        // 释放相机资源
        imageCapture = null;
    }
}