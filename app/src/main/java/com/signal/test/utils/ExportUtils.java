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
            writer.write("时间戳,运营商,网络类型,小区CGI,PCI,频点,频段,接收电平,5G CGI,5G PCI,5G频点,5G频段,RSRP,SINR,RSRQ,信号质量,网络稳定性,信号质量评分,稳定性评分,是否异常,位置,纬度,经度,照片路径\n");

            // 写入数据
            for (SignalData data : dataList) {
                writer.write(data.getTimestamp() + ",");
                writer.write(data.getOperator() + ",");
                writer.write(data.getNetworkType() + ",");
                writer.write(data.getCgi() + ",");
                writer.write(data.getPci() + ",");
                writer.write(data.getFrequency() + ",");
                writer.write(data.getBand() + ",");
                writer.write(data.getRssi() + ",");
                writer.write((data.getNrCgi() != null ? data.getNrCgi() : "N/A") + ",");
                writer.write(data.getNrPci() + ",");
                writer.write(data.getNrFrequency() + ",");
                writer.write((data.getNrBand() != null ? data.getNrBand() : "N/A") + ",");
                writer.write(data.getRsrp() + ",");
                writer.write(data.getSinr() + ",");
                writer.write(data.getRsrq() + ",");
                writer.write((data.getSignalQuality() != null ? data.getSignalQuality() : "N/A") + ",");
                writer.write((data.getNetworkStability() != null ? data.getNetworkStability() : "N/A") + ",");
                writer.write(data.getSignalQualityScore() + ",");
                writer.write(data.getStabilityScore() + ",");
                writer.write(data.isAnomaly() ? "是" : "否" + ",");
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

    // 导出为增强版CSV格式（包含详细统计信息）
    public boolean exportEnhancedCSV(List<SignalData> dataList, String fileName) {
        try {
            // 使用应用专属外部存储目录
            File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SignalTest/Export/Enhanced");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 创建文件
            File file = new File(directory, fileName + "_Enhanced.csv");
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            // 写入统计摘要
            writer.write("=== 信号测试统计摘要 ===\n");
            writer.write("导出时间," + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
            writer.write("测试点数," + dataList.size() + "\n");
            writer.write("平均信号强度," + String.format("%.2f", calculateAverageRssi(dataList)) + " dBm\n");
            writer.write("平均RSRP," + String.format("%.2f", calculateAverageRsrp(dataList)) + " dBm\n");
            writer.write("平均SINR," + String.format("%.2f", calculateAverageSinr(dataList)) + " dB\n");
            writer.write("平均RSRQ," + String.format("%.2f", calculateAverageRsrq(dataList)) + " dB\n");
            writer.write("网络类型分布," + getNetworkTypeDistribution(dataList) + "\n");
            writer.write("信号质量分布," + getSignalQualityDistribution(dataList) + "\n");
            writer.write("网络稳定性分布," + getNetworkStabilityDistribution(dataList) + "\n");
            writer.write("异常信号数量," + getAnomalyCount(dataList) + "\n");
            writer.write("5G覆盖率," + String.format("%.2f%%", (get5GTestCount(dataList) * 100.0 / dataList.size())) + "\n");
            writer.write("\n");

            // 写入表头
            writer.write("序号,时间戳,运营商,网络类型,小区CGI,PCI,频点,频段,接收电平,5G CGI,5G PCI,5G频点,5G频段,RSRP,SINR,RSRQ,信号质量,网络稳定性,信号质量评分,稳定性评分,是否异常,位置,纬度,经度,照片路径\n");

            // 写入数据
            for (int i = 0; i < dataList.size(); i++) {
                SignalData data = dataList.get(i);
                writer.write((i + 1) + ",");
                writer.write(data.getTimestamp() + ",");
                writer.write(data.getOperator() + ",");
                writer.write(data.getNetworkType() + ",");
                writer.write(data.getCgi() + ",");
                writer.write(data.getPci() + ",");
                writer.write(data.getFrequency() + ",");
                writer.write(data.getBand() + ",");
                writer.write(data.getRssi() + ",");
                writer.write((data.getNrCgi() != null ? data.getNrCgi() : "N/A") + ",");
                writer.write(data.getNrPci() + ",");
                writer.write(data.getNrFrequency() + ",");
                writer.write((data.getNrBand() != null ? data.getNrBand() : "N/A") + ",");
                writer.write(data.getRsrp() + ",");
                writer.write(data.getSinr() + ",");
                writer.write(data.getRsrq() + ",");
                writer.write((data.getSignalQuality() != null ? data.getSignalQuality() : "N/A") + ",");
                writer.write((data.getNetworkStability() != null ? data.getNetworkStability() : "N/A") + ",");
                writer.write(data.getSignalQualityScore() + ",");
                writer.write(data.getStabilityScore() + ",");
                writer.write(data.isAnomaly() ? "是" : "否" + ",");
                writer.write((data.getLocation() != null ? data.getLocation() : "N/A") + ",");
                writer.write(data.getLatitude() + ",");
                writer.write(data.getLongitude() + ",");
                writer.write((data.getPhotoPath() != null ? data.getPhotoPath() : "N/A") + "\n");
            }

            // 关闭资源
            writer.flush();
            writer.close();
            fos.close();

            Toast.makeText(context, "增强版CSV导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "增强版CSV导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // 获取默认文件名（基于当前时间）
    public String getDefaultFileName() {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return "SignalTest_" + timeStamp;
    }
    
    // 生成批量测试报告（CSV格式）
    public String generateBatchTestReport(List<SignalData> batchTestData, int totalTests, int successCount, int failCount, int interval) {
        return generateEnhancedBatchTestReport(batchTestData, totalTests, successCount, failCount, interval, false);
    }

    // 生成增强版批量测试报告（包含详细分析）
    public String generateEnhancedBatchTestReport(List<SignalData> batchTestData, int totalTests, int successCount, int failCount, int interval, boolean includeCharts) {
        try {
            // 使用应用专属外部存储目录
            File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SignalTest/Reports/Enhanced");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 生成文件名
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = "BatchTestReport_" + timeStamp + (includeCharts ? "_WithCharts" : "");
            File file = new File(directory, fileName + ".csv");

            // 创建文件输出流
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            
            // 写入报告标题和基本信息
            writer.write("=== 信号测试批量报告 ===\n");
            writer.write("报告版本,1.1\n");
            writer.write("生成时间," + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
            writer.write("\n");
            
            // 测试摘要
            writer.write("=== 测试摘要 ===\n");
            writer.write("项目,值\n");
            
            // 测试时间范围
            if (!batchTestData.isEmpty()) {
                writer.write("测试时间范围," + batchTestData.get(0).getTimestamp() + " 至 " + batchTestData.get(batchTestData.size() - 1).getTimestamp() + "\n");
            } else {
                writer.write("测试时间范围,无数据\n");
            }
            
            // 测试基本信息
            writer.write("测试总次数," + totalTests + "次\n");
            writer.write("成功次数," + successCount + "次\n");
            writer.write("失败次数," + failCount + "次\n");
            writer.write("测试间隔," + interval + "秒\n");
            writer.write("成功率," + String.format("%.2f%%", (successCount * 100.0f / totalTests)) + "\n");
            writer.write("测试持续时间," + String.format("%.1f", (totalTests - 1) * interval / 60.0) + " 分钟\n");
            
            // 网络类型分布
            writer.write("网络类型分布," + getNetworkTypeDistribution(batchTestData) + "\n");
            writer.write("5G覆盖率," + String.format("%.2f%%", (get5GTestCount(batchTestData) * 100.0 / totalTests)) + "\n");
            
            // 信号强度统计
            writer.write("\n");
            writer.write("=== 信号强度统计 ===\n");
            writer.write("项目,值\n");
            writer.write("平均信号强度," + String.format("%.2f", calculateAverageRssi(batchTestData)) + " dBm\n");
            writer.write("最大信号强度," + calculateMaxRssi(batchTestData) + " dBm\n");
            writer.write("最小信号强度," + calculateMinRssi(batchTestData) + " dBm\n");
            writer.write("信号强度标准差," + String.format("%.2f", calculateRssiStdDev(batchTestData)) + " dBm\n");
            writer.write("信号强度变异系数," + String.format("%.2f%%", (calculateRssiStdDev(batchTestData) / Math.abs(calculateAverageRssi(batchTestData))) * 100) + "\n");
            
            // 5G相关统计
            writer.write("\n");
            writer.write("=== 5G性能统计 ===\n");
            writer.write("项目,值\n");
            writer.write("5G测试次数," + get5GTestCount(batchTestData) + "次\n");
            writer.write("平均RSRP," + String.format("%.2f", calculateAverageRsrp(batchTestData)) + " dBm\n");
            writer.write("平均SINR," + String.format("%.2f", calculateAverageSinr(batchTestData)) + " dB\n");
            writer.write("平均RSRQ," + String.format("%.2f", calculateAverageRsrq(batchTestData)) + " dB\n");
            
            // 信号质量和稳定性分析
            writer.write("\n");
            writer.write("=== 信号质量与稳定性分析 ===\n");
            writer.write("项目,值\n");
            writer.write("信号质量分布," + getSignalQualityDistribution(batchTestData) + "\n");
            writer.write("网络稳定性分布," + getNetworkStabilityDistribution(batchTestData) + "\n");
            writer.write("异常信号数量," + getAnomalyCount(batchTestData) + "\n");
            writer.write("异常信号比例," + String.format("%.2f%%", (getAnomalyCount(batchTestData) * 100.0 / totalTests)) + "\n");
            writer.write("平均信号质量评分," + String.format("%.2f", calculateAverageSignalQualityScore(batchTestData)) + "\n");
            writer.write("平均稳定性评分," + String.format("%.2f", calculateAverageStabilityScore(batchTestData)) + "\n");
            
            // 网络类型切换分析
            writer.write("\n");
            writer.write("=== 网络类型切换分析 ===\n");
            writer.write("项目,值\n");
            writer.write("网络切换次数," + calculateNetworkSwitchCount(batchTestData) + "\n");
            writer.write("平均网络保持时间," + String.format("%.2f", calculateAverageNetworkHoldTime(batchTestData, interval)) + " 秒\n");
            
            // 图表数据（如果需要）
            if (includeCharts) {
                writer.write("\n");
                writer.write("=== 图表数据 ===\n");
                writer.write("图表类型,数据点\n");
                writer.write("信号强度趋势," + generateChartData(batchTestData, "rssi") + "\n");
                writer.write("RSRP趋势," + generateChartData(batchTestData, "rsrp") + "\n");
                writer.write("SINR趋势," + generateChartData(batchTestData, "sinr") + "\n");
                writer.write("网络类型分布," + generateNetworkTypeChartData(batchTestData) + "\n");
            }
            
            // 详细数据
            writer.write("\n");
            writer.write("=== 详细测试数据 ===\n");
            writer.write("序号,时间戳,运营商,网络类型,小区CGI,PCI,频点,频段,接收电平,5G CGI,5G PCI,5G频点,5G频段,RSRP,SINR,RSRQ,信号质量,网络稳定性,信号质量评分,稳定性评分,是否异常,位置,纬度,经度,照片路径\n");
            
            // 写入详细数据
            for (int i = 0; i < batchTestData.size(); i++) {
                SignalData data = batchTestData.get(i);
                writer.write((i + 1) + ",");
                writer.write(data.getTimestamp() + ",");
                writer.write(data.getOperator() + ",");
                writer.write(data.getNetworkType() + ",");
                writer.write(data.getCgi() + ",");
                writer.write(data.getPci() + ",");
                writer.write(data.getFrequency() + ",");
                writer.write(data.getBand() + ",");
                writer.write(data.getRssi() + ",");
                writer.write((data.getNrCgi() != null ? data.getNrCgi() : "N/A") + ",");
                writer.write(data.getNrPci() + ",");
                writer.write(data.getNrFrequency() + ",");
                writer.write((data.getNrBand() != null ? data.getNrBand() : "N/A") + ",");
                writer.write(data.getRsrp() + ",");
                writer.write(data.getSinr() + ",");
                writer.write(data.getRsrq() + ",");
                writer.write((data.getSignalQuality() != null ? data.getSignalQuality() : "N/A") + ",");
                writer.write((data.getNetworkStability() != null ? data.getNetworkStability() : "N/A") + ",");
                writer.write(data.getSignalQualityScore() + ",");
                writer.write(data.getStabilityScore() + ",");
                writer.write(data.isAnomaly() ? "是" : "否" + ",");
                writer.write((data.getLocation() != null ? data.getLocation() : "N/A") + ",");
                writer.write(data.getLatitude() + ",");
                writer.write(data.getLongitude() + ",");
                writer.write((data.getPhotoPath() != null ? data.getPhotoPath() : "N/A") + "\n");
            }
            
            // 写入分析总结
            writer.write("\n");
            writer.write("=== 测试总结与建议 ===\n");
            writer.write("项目,内容\n");
            writer.write("测试结论," + generateTestConclusion(batchTestData, successCount, totalTests) + "\n");
            writer.write("网络性能评估," + evaluateNetworkPerformance(batchTestData) + "\n");
            writer.write("改进建议," + generateImprovementSuggestions(batchTestData) + "\n");
            
            // 关闭资源
            writer.flush();
            writer.close();
            fos.close();
            
            Toast.makeText(context, "增强版测试报告生成成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
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
    
    // 获取信号质量分布
    private String getSignalQualityDistribution(List<SignalData> dataList) {
        if (dataList.isEmpty()) return "无数据";
        int excellent = 0, good = 0, fair = 0, poor = 0;
        for (SignalData data : dataList) {
            if (data.getSignalQuality() != null) {
                switch (data.getSignalQuality()) {
                    case "优秀": excellent++;
                        break;
                    case "良好": good++;
                        break;
                    case "一般": fair++;
                        break;
                    case "较差": poor++;
                        break;
                }
            }
        }
        return "优秀: " + excellent + "次, 良好: " + good + "次, 一般: " + fair + "次, 较差: " + poor + "次";
    }
    
    // 获取网络稳定性分布
    private String getNetworkStabilityDistribution(List<SignalData> dataList) {
        if (dataList.isEmpty()) return "无数据";
        int stable = 0, moderate = 0, unstable = 0;
        for (SignalData data : dataList) {
            if (data.getNetworkStability() != null) {
                switch (data.getNetworkStability()) {
                    case "稳定": stable++;
                        break;
                    case "一般": moderate++;
                        break;
                    case "不稳定": unstable++;
                        break;
                }
            }
        }
        return "稳定: " + stable + "次, 一般: " + moderate + "次, 不稳定: " + unstable + "次";
    }
    
    // 获取异常信号数量
    private int getAnomalyCount(List<SignalData> dataList) {
        int count = 0;
        for (SignalData data : dataList) {
            if (data.isAnomaly()) {
                count++;
            }
        }
        return count;
    }
    
    // 计算平均信号质量评分
    private double calculateAverageSignalQualityScore(List<SignalData> dataList) {
        if (dataList.isEmpty()) return 0;
        int sum = 0;
        for (SignalData data : dataList) {
            sum += data.getSignalQualityScore();
        }
        return (double) sum / dataList.size();
    }
    
    // 计算平均稳定性评分
    private double calculateAverageStabilityScore(List<SignalData> dataList) {
        if (dataList.isEmpty()) return 0;
        int sum = 0;
        for (SignalData data : dataList) {
            sum += data.getStabilityScore();
        }
        return (double) sum / dataList.size();
    }
    
    // 计算网络切换次数
    private int calculateNetworkSwitchCount(List<SignalData> dataList) {
        if (dataList.size() < 2) return 0;
        int switchCount = 0;
        String previousNetwork = dataList.get(0).getNetworkType();
        for (int i = 1; i < dataList.size(); i++) {
            String currentNetwork = dataList.get(i).getNetworkType();
            if (!currentNetwork.equals(previousNetwork)) {
                switchCount++;
                previousNetwork = currentNetwork;
            }
        }
        return switchCount;
    }
    
    // 计算平均网络保持时间
    private double calculateAverageNetworkHoldTime(List<SignalData> dataList, int interval) {
        if (dataList.size() < 2) return 0;
        int totalHoldTime = (dataList.size() - 1) * interval;
        int switchCount = calculateNetworkSwitchCount(dataList);
        int networkChanges = switchCount + 1;
        return (double) totalHoldTime / networkChanges;
    }
    
    // 生成图表数据
    private String generateChartData(List<SignalData> dataList, String type) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < dataList.size(); i++) {
            SignalData signalData = dataList.get(i);
            double value = 0;
            switch (type) {
                case "rssi": value = signalData.getRssi();
                    break;
                case "rsrp": value = signalData.getRsrp();
                    break;
                case "sinr": value = signalData.getSinr();
                    break;
                case "rsrq": value = signalData.getRsrq();
                    break;
            }
            data.append(i).append(":").append(value);
            if (i < dataList.size() - 1) {
                data.append("; ");
            }
        }
        return data.toString();
    }
    
    // 生成网络类型分布图表数据
    private String generateNetworkTypeChartData(List<SignalData> dataList) {
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
        return "2G:" + count2G + "; 3G:" + count3G + "; 4G:" + count4G + "; 5G:" + count5G;
    }
    
    // 生成测试结论
    private String generateTestConclusion(List<SignalData> dataList, int successCount, int totalTests) {
        double successRate = (successCount * 100.0) / totalTests;
        int anomalyCount = getAnomalyCount(dataList);
        double anomalyRate = (anomalyCount * 100.0) / totalTests;
        int fiveGCount = get5GTestCount(dataList);
        double fiveGCoverage = (fiveGCount * 100.0) / totalTests;
        
        if (successRate >= 95 && anomalyRate < 5 && fiveGCoverage >= 80) {
            return "测试结果优秀，网络连接稳定，5G覆盖率高，无明显异常";
        } else if (successRate >= 80 && anomalyRate < 10) {
            return "测试结果良好，网络连接基本稳定，存在少量异常";
        } else if (successRate >= 60) {
            return "测试结果一般，网络连接稳定性有待提高，存在一定数量的异常";
        } else {
            return "测试结果较差，网络连接不稳定，异常信号较多，建议进一步排查";
        }
    }
    
    // 评估网络性能
    private String evaluateNetworkPerformance(List<SignalData> dataList) {
        double avgRssi = calculateAverageRssi(dataList);
        double avgRsrp = calculateAverageRsrp(dataList);
        double avgSinr = calculateAverageSinr(dataList);
        int switchCount = calculateNetworkSwitchCount(dataList);
        
        StringBuilder evaluation = new StringBuilder();
        
        if (avgRssi > -80) {
            evaluation.append("信号强度优秀，");
        } else if (avgRssi > -90) {
            evaluation.append("信号强度良好，");
        } else if (avgRssi > -100) {
            evaluation.append("信号强度一般，");
        } else {
            evaluation.append("信号强度较差，");
        }
        
        if (avgRsrp > -85) {
            evaluation.append("5G信号质量优秀，");
        } else if (avgRsrp > -95) {
            evaluation.append("5G信号质量良好，");
        } else if (avgRsrp > -105) {
            evaluation.append("5G信号质量一般，");
        } else {
            evaluation.append("5G信号质量较差，");
        }
        
        if (avgSinr > 20) {
            evaluation.append("信号与干扰比优秀，");
        } else if (avgSinr > 10) {
            evaluation.append("信号与干扰比良好，");
        } else if (avgSinr > 0) {
            evaluation.append("信号与干扰比一般，");
        } else {
            evaluation.append("信号与干扰比较差，");
        }
        
        if (switchCount < dataList.size() * 0.1) {
            evaluation.append("网络切换较少，稳定性好");
        } else if (switchCount < dataList.size() * 0.3) {
            evaluation.append("网络切换适中，稳定性一般");
        } else {
            evaluation.append("网络切换频繁，稳定性较差");
        }
        
        return evaluation.toString();
    }
    
    // 生成改进建议
    private String generateImprovementSuggestions(List<SignalData> dataList) {
        double avgRssi = calculateAverageRssi(dataList);
        double avgRsrp = calculateAverageRsrp(dataList);
        int switchCount = calculateNetworkSwitchCount(dataList);
        int anomalyCount = getAnomalyCount(dataList);
        
        StringBuilder suggestions = new StringBuilder();
        
        if (avgRssi < -90) {
            suggestions.append("1. 考虑调整测试位置，选择信号覆盖更好的区域；");
            suggestions.append("2. 检查设备天线是否正常，是否需要更换或调整；");
        }
        
        if (avgRsrp < -95) {
            suggestions.append("3. 5G信号质量较差，建议在5G覆盖更好的区域进行测试；");
        }
        
        if (switchCount > dataList.size() * 0.2) {
            suggestions.append("4. 网络切换频繁，可能存在网络覆盖重叠区域，建议避开此类区域；");
        }
        
        if (anomalyCount > dataList.size() * 0.1) {
            suggestions.append("5. 异常信号较多，建议检查设备状态，确保无硬件故障；");
            suggestions.append("6. 考虑重启设备或重置网络设置后再次测试；");
        }
        
        suggestions.append("7. 建议在不同时间段重复测试，以获得更全面的网络性能数据；");
        suggestions.append("8. 如需进一步分析，可增加测试点数和测试时间，获得更详细的数据。");
        
        return suggestions.toString();
    }
} 
