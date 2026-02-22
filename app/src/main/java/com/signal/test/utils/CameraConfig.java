package com.signal.test.utils;

import android.util.Size;

import androidx.camera.core.ImageCapture;

public class CameraConfig {
    // 相机预览分辨率选项
    public static final Size PREVIEW_RESOLUTION_HIGH = new Size(1920, 1080);
    public static final Size PREVIEW_RESOLUTION_MEDIUM = new Size(1280, 720);
    public static final Size PREVIEW_RESOLUTION_LOW = new Size(640, 480);
    
    // 图像捕获分辨率选项
    public static final Size CAPTURE_RESOLUTION_HIGH = new Size(4032, 3024);
    public static final Size CAPTURE_RESOLUTION_MEDIUM = new Size(2592, 1944);
    public static final Size CAPTURE_RESOLUTION_LOW = new Size(1920, 1080);
    
    // 照片质量选项
    public static final int QUALITY_HIGH = 95;
    public static final int QUALITY_MEDIUM = 80;
    public static final int QUALITY_LOW = 60;
    
    // 水印位置选项
    public static final int WATERMARK_POSITION_BOTTOM = 0;
    public static final int WATERMARK_POSITION_TOP = 1;
    public static final int WATERMARK_POSITION_CUSTOM = 2;
    
    // 当前配置
    private Size previewResolution;
    private Size captureResolution;
    private int imageQuality;
    private int captureMode;
    private int flashMode;
    private int watermarkPosition;
    private boolean enableTimestamp;
    private boolean enableLocation;
    private boolean enableSignalInfo;
    private boolean enableCoordinates;
    
    public CameraConfig() {
        // 默认配置
        this.previewResolution = PREVIEW_RESOLUTION_HIGH;
        this.captureResolution = CAPTURE_RESOLUTION_HIGH;
        this.imageQuality = QUALITY_HIGH;
        this.captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY;
        this.flashMode = ImageCapture.FLASH_MODE_OFF;
        this.watermarkPosition = WATERMARK_POSITION_BOTTOM;
        this.enableTimestamp = true;
        this.enableLocation = true;
        this.enableSignalInfo = true;
        this.enableCoordinates = true;
    }
    
    // Getters and Setters
    public Size getPreviewResolution() {
        return previewResolution;
    }
    
    public void setPreviewResolution(Size previewResolution) {
        this.previewResolution = previewResolution;
    }
    
    public Size getCaptureResolution() {
        return captureResolution;
    }
    
    public void setCaptureResolution(Size captureResolution) {
        this.captureResolution = captureResolution;
    }
    
    public int getImageQuality() {
        return imageQuality;
    }
    
    public void setImageQuality(int imageQuality) {
        this.imageQuality = imageQuality;
    }
    
    public int getCaptureMode() {
        return captureMode;
    }
    
    public void setCaptureMode(int captureMode) {
        this.captureMode = captureMode;
    }
    
    public int getFlashMode() {
        return flashMode;
    }
    
    public void setFlashMode(int flashMode) {
        this.flashMode = flashMode;
    }
    
    public int getWatermarkPosition() {
        return watermarkPosition;
    }
    
    public void setWatermarkPosition(int watermarkPosition) {
        this.watermarkPosition = watermarkPosition;
    }
    
    public boolean isEnableTimestamp() {
        return enableTimestamp;
    }
    
    public void setEnableTimestamp(boolean enableTimestamp) {
        this.enableTimestamp = enableTimestamp;
    }
    
    public boolean isEnableLocation() {
        return enableLocation;
    }
    
    public void setEnableLocation(boolean enableLocation) {
        this.enableLocation = enableLocation;
    }
    
    public boolean isEnableSignalInfo() {
        return enableSignalInfo;
    }
    
    public void setEnableSignalInfo(boolean enableSignalInfo) {
        this.enableSignalInfo = enableSignalInfo;
    }
    
    public boolean isEnableCoordinates() {
        return enableCoordinates;
    }
    
    public void setEnableCoordinates(boolean enableCoordinates) {
        this.enableCoordinates = enableCoordinates;
    }
}