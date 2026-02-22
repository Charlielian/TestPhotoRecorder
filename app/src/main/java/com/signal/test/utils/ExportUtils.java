package com.signal.test.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.signal.test.models.SignalData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class ExportUtils {

    private Context context;

    public ExportUtils(Context context) {
        this.context = context;
    }

    // 导出为CSV格式
    public boolean exportToCSV(List<SignalData> dataList, String fileName) {
        try {
            // 使用应用专属外部存储目录
            File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SignalTest/Export");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 创建文件
            File file = new File(directory, fileName + ".csv");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            // 写入表头
            writer.write("时间戳,运营商,网络类型,小区CGI,频点,频段,接收电平,5G CGI,5G频点,5G频段,RSRP,SINR,RSRQ,位置,纬度,经度,照片路径\n");

            // 写入数据
            for (SignalData data : dataList) {
                writer.write(data.getTimestamp() + ",");
                writer.write(data.getOperator() + ",");
                writer.write(data.getNetworkType() + ",");
                writer.write(data.getCgi() + ",");
                writer.write(data.getFrequency() + ",");
                writer.write(data.getBand() + ",");
                writer.write(data.getRssi() + ",");
                writer.write((data.getNrCgi() != null ? data.getNrCgi() : "N/A") + ",");
                writer.write(data.getNrFrequency() + ",");
                writer.write((data.getNrBand() != null ? data.getNrBand() : "N/A") + ",");
                writer.write(data.getRsrp() + ",");
                writer.write(data.getSinr() + ",");
                writer.write(data.getRsrq() + ",");
                writer.write((data.getLocation() != null ? data.getLocation() : "N/A") + ",");
                writer.write(data.getLatitude() + ",");
                writer.write(data.getLongitude() + ",");
                writer.write((data.getPhotoPath() != null ? data.getPhotoPath() : "N/A") + "\n");
            }

            // 关闭资源
            writer.flush();
            writer.close();
            fos.close();

            Toast.makeText(context, "CSV导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "CSV导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // 获取默认文件名（基于当前时间）
    public String getDefaultFileName() {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return "SignalTest_" + timeStamp;
    }
    
    // 生成批量测试报告（CSV格式）
    public String generateBatchTestReport(List<SignalData> batchTestData) {
        try {
            // 使用应用专属外部存储目录
            File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SignalTest/Reports");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 生成文件名
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "BatchTestReport_" + timeStamp;
            File file = new File(directory, fileName + ".csv");

            // 创建文件输出流
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            
            // 写入摘要信息
            writer.write("批量测试报告\n");
            writer.write("生成时间," + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
            writer.write("\n");
            writer.write("测试摘要\n");
            writer.write("项目,值\n");
            
            // 测试时间范围
            if (!batchTestData.isEmpty()) {
                writer.write("测试时间范围," + batchTestData.get(0).getTimestamp() + " 至 " + batchTestData.get(batchTestData.size() - 1).getTimestamp() + "\n");
            } else {
                writer.write("测试时间范围,无数据\n");
            }
            
            // 测试总次数
            writer.write("测试总次数," + batchTestData.size() + "次\n");
            
            // 网络类型分布
            writer.write("网络类型分布," + getNetworkTypeDistribution(batchTestData) + "\n");
            
            // 信号强度统计
            writer.write("平均信号强度," + calculateAverageRssi(batchTestData) + " dBm\n");
            writer.write("最大信号强度," + calculateMaxRssi(batchTestData) + " dBm\n");
            writer.write("最小信号强度," + calculateMinRssi(batchTestData) + " dBm\n");
            writer.write("信号强度标准差," + String.format("%.2f", calculateRssiStdDev(batchTestData)) + " dBm\n");
            
            // 5G相关统计
            writer.write("\n");
            writer.write("5G测试统计\n");
            writer.write("项目,值\n");
            writer.write("5G测试次数," + get5GTestCount(batchTestData) + "次\n");
            writer.write("平均RSRP," + calculateAverageRsrp(batchTestData) + " dBm\n");
            writer.write("平均SINR," + calculateAverageSinr(batchTestData) + " dB\n");
            writer.write("平均RSRQ," + calculateAverageRsrq(batchTestData) + " dB\n");
            
            // 详细数据
            writer.write("\n");
            writer.write("详细数据\n");
            writer.write("序号,时间戳,运营商,网络类型,小区CGI,频点,频段,接收电平,5G CGI,5G频点,5G频段,RSRP,SINR,RSRQ,位置\n");
            
            // 写入详细数据
            for (int i = 0; i < batchTestData.size(); i++) {
                SignalData data = batchTestData.get(i);
                writer.write((i + 1) + ",");
                writer.write(data.getTimestamp() + ",");
                writer.write(data.getOperator() + ",");
                writer.write(data.getNetworkType() + ",");
                writer.write(data.getCgi() + ",");
                writer.write(data.getFrequency() + ",");
                writer.write(data.getBand() + ",");
                writer.write(data.getRssi() + ",");
                writer.write((data.getNrCgi() != null ? data.getNrCgi() : "N/A") + ",");
                writer.write(data.getNrFrequency() + ",");
                writer.write((data.getNrBand() != null ? data.getNrBand() : "N/A") + ",");
                writer.write(data.getRsrp() + ",");
                writer.write(data.getSinr() + ",");
                writer.write(data.getRsrq() + ",");
                writer.write((data.getLocation() != null ? data.getLocation() : "N/A") + "\n");
            }
            
            // 关闭资源
            writer.flush();
            writer.close();
            fos.close();
            
            Toast.makeText(context, "测试报告生成成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "测试报告生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    // 计算平均信号强度
    private double calculateAverageRssi(List<SignalData> dataList) {
        if (dataList.isEmpty()) return 0;
        int sum = 0;
        for (SignalData data : dataList) {
            sum += data.getRssi();
        }
        return (double) sum / dataList.size();
    }
    
    // 计算最大信号强度
    private int calculateMaxRssi(List<SignalData> dataList) {
        if (dataList.isEmpty()) return 0;
        int max = dataList.get(0).getRssi();
        for (SignalData data : dataList) {
            if (data.getRssi() > max) {
                max = data.getRssi();
            }
        }
        return max;
    }
    
    // 计算最小信号强度
    private int calculateMinRssi(List<SignalData> dataList) {
        if (dataList.isEmpty()) return 0;
        int min = dataList.get(0).getRssi();
        for (SignalData data : dataList) {
            if (data.getRssi() < min) {
                min = data.getRssi();
            }
        }
        return min;
    }
    
    // 计算信号强度标准差
    private double calculateRssiStdDev(List<SignalData> dataList) {
        if (dataList.size() <= 1) return 0;
        double avg = calculateAverageRssi(dataList);
        double sum = 0;
        for (SignalData data : dataList) {
            sum += Math.pow(data.getRssi() - avg, 2);
        }
        return Math.sqrt(sum / (dataList.size() - 1));
    }
    
    // 计算平均RSRP
    private double calculateAverageRsrp(List<SignalData> dataList) {
        int count = 0;
        int sum = 0;
        for (SignalData data : dataList) {
            if (data.getRsrp() != 0) {
                sum += data.getRsrp();
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0;
    }
    
    // 计算平均SINR
    private double calculateAverageSinr(List<SignalData> dataList) {
        int count = 0;
        int sum = 0;
        for (SignalData data : dataList) {
            if (data.getSinr() != 0) {
                sum += data.getSinr();
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0;
    }
    
    // 计算平均RSRQ
    private double calculateAverageRsrq(List<SignalData> dataList) {
        int count = 0;
        int sum = 0;
        for (SignalData data : dataList) {
            if (data.getRsrq() != 0) {
                sum += data.getRsrq();
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0;
    }
    
    // 获取网络类型分布
    private String getNetworkTypeDistribution(List<SignalData> dataList) {
        if (dataList.isEmpty()) return "无数据";
        int count2G = 0, count3G = 0, count4G = 0, count5G = 0;
        for (SignalData data : dataList) {
            switch (data.getNetworkType()) {
                case "2G": count2G++;
                    break;
                case "3G": count3G++;
                    break;
                case "4G": count4G++;
                    break;
                case "5G": count5G++;
                    break;
            }
        }
        return "2G: " + count2G + "次, 3G: " + count3G + "次, 4G: " + count4G + "次, 5G: " + count5G + "次";
    }
    
    // 获取5G测试次数
    private int get5GTestCount(List<SignalData> dataList) {
        int count = 0;
        for (SignalData data : dataList) {
            if (data.getNetworkType().equals("5G")) {
                count++;
            }
        }
        return count;
    }
} 
