package com.signal.test.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;

import com.signal.test.models.SignalData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraUtils {
    private Context context;
    
    public CameraUtils(Context context) {
        this.context = context;
    }
    
    // 保存照片并叠加信号信息
    public String savePhotoWithSignalInfo(Bitmap bitmap, SignalData signalData) {
        try {
            // 计算合适的图像尺寸，避免OOM
            int targetWidth = Math.min(bitmap.getWidth(), 4032);
            int targetHeight = Math.min(bitmap.getHeight(), 3024);
            
            // 创建缩放后的Bitmap
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            
            // 创建新的Bitmap用于绘制
            Bitmap resultBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(resultBitmap);
            
            // 绘制信号信息
            drawSignalInfo(canvas, resultBitmap.getWidth(), resultBitmap.getHeight(), signalData);
            
            // 保存图片
            String path = saveBitmap(resultBitmap);
            
            // 释放Bitmap资源
            scaledBitmap.recycle();
            resultBitmap.recycle();
            
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 绘制信号信息到画布
    private void drawSignalInfo(Canvas canvas, int width, int height, SignalData signalData) {
        // 计算合适的字体大小和背景尺寸
        float textSize = Math.min(width, height) * 0.02f; // 自适应字体大小
        int lineHeight = (int)(textSize * 1.5f);
        
        // 绘制背景
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setAlpha(150); // 稍微提高透明度
        
        // 根据网络类型调整背景高度
        int lineCount = signalData.getNetworkType().equals("5G") ? 7 : 5;
        int backgroundHeight = lineCount * lineHeight + 40;
        int backgroundWidth = (int)(width * 0.35f);
        canvas.drawRect(0f, height - backgroundHeight, backgroundWidth, height, backgroundPaint);
        
        // 绘制文字
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true); // 开启抗锯齿
        textPaint.setSubpixelText(true); // 开启亚像素渲染
        
        // 绘制文本（逐行绘制）
        int yOffset = height - backgroundHeight + 30;
        
        canvas.drawText("运营商: " + signalData.getOperator(), 20f, yOffset, textPaint);
        yOffset += lineHeight;
        
        canvas.drawText("CGI: " + signalData.getCgi(), 20f, yOffset, textPaint);
        yOffset += lineHeight;
        
        canvas.drawText("频点: " + signalData.getFrequency(), 20f, yOffset, textPaint);
        yOffset += lineHeight;
        
        canvas.drawText("频段: " + signalData.getBand(), 20f, yOffset, textPaint);
        yOffset += lineHeight;
        
        // 根据网络类型添加不同的信号强度参数
        if (signalData.getNetworkType().equals("5G")) {
            canvas.drawText("RSRP: " + (signalData.getRsrp() != 0 ? signalData.getRsrp() + " dBm" : "N/A"), 20f, yOffset, textPaint);
            yOffset += lineHeight;
            
            canvas.drawText("SINR: " + (signalData.getSinr() != 0 ? signalData.getSinr() + " dB" : "N/A"), 20f, yOffset, textPaint);
            yOffset += lineHeight;
            
            canvas.drawText("RSRQ: " + (signalData.getRsrq() != 0 ? signalData.getRsrq() + " dB" : "N/A"), 20f, yOffset, textPaint);
        } else {
            canvas.drawText("电平: " + signalData.getRssi() + " dBm", 20f, yOffset, textPaint);
        }
    }
    
    // 保存Bitmap到文件
    private String saveBitmap(Bitmap bitmap) throws IOException {
        // 创建存储目录
        File directory = getPhotoDirectory();
        
        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "SIGNAL_" + timeStamp + ".jpg";
        File file = new File(directory, fileName);
        
        // 保存文件
        FileOutputStream fos = new FileOutputStream(file);
        // 提高压缩质量到95
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
        fos.flush();
        fos.close();
        
        return file.getAbsolutePath();
    }
    
    // 获取照片存储目录 - 使用应用专属存储
    public File getPhotoDirectory() {
        // 使用应用专属外部存储目录，无需权限且在卸载时自动清理
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SignalTest");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }
}