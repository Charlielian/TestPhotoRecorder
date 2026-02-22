package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private LineChart chart;
    private Spinner paramSpinner;
    private Button btnExportChart, btnBack;
    private TextView tvChartTitle;
    
    private StorageUtils storageUtils;
    private List<SignalData> signalDataList;
    private List<String> timeLabels;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        
        initViews();
        initServices();
        loadData();
        setupChart();
        updateChart(0); // 默认显示RSSI
    }
    
    private void initViews() {
        chart = findViewById(R.id.line_chart);
        paramSpinner = findViewById(R.id.param_spinner);
        btnExportChart = findViewById(R.id.btn_export_chart);
        btnBack = findViewById(R.id.btn_back);
        tvChartTitle = findViewById(R.id.tv_chart_title);
        
        btnBack.setOnClickListener(v -> finish());
        btnExportChart.setOnClickListener(v -> exportChart());
        
        // 参数选择监听器
        paramSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateChart(position);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }
    
    private void initServices() {
        storageUtils = new StorageUtils(this);
    }
    
    private void loadData() {
        signalDataList = storageUtils.getAllSignalData();
        timeLabels = new ArrayList<>();
        
        // 提取时间标签
        for (SignalData data : signalDataList) {
            if (data.getTimestamp() != null) {
                // 提取时间部分（去掉日期）
                String[] parts = data.getTimestamp().split(" ");
                if (parts.length > 1) {
                    timeLabels.add(parts[1]);
                } else {
                    timeLabels.add(data.getTimestamp());
                }
            } else {
                timeLabels.add("N/A");
            }
        }
    }
    
    private void setupChart() {
        // 设置图表基本属性
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        
        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        
        // 设置Y轴
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-150f);
        leftAxis.setAxisMaximum(-30f);
        
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }
    
    // 更新图表数据
    private void updateChart(int paramIndex) {
        if (signalDataList.isEmpty()) {
            Toast.makeText(this, "没有数据可显示", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ArrayList<Entry> entries = new ArrayList<>();
        String paramName = "";
        
        // 根据选择的参数类型填充数据
        switch (paramIndex) {
            case 0: // RSSI
                paramName = "接收电平 (RSSI)";
                for (int i = 0; i < signalDataList.size(); i++) {
                    entries.add(new Entry(i, signalDataList.get(i).getRssi()));
                }
                break;
            case 1: // RSRP
                paramName = "参考信号接收功率 (RSRP)";
                for (int i = 0; i < signalDataList.size(); i++) {
                    entries.add(new Entry(i, signalDataList.get(i).getRsrp()));
                }
                break;
            case 2: // SINR
                paramName = "信号与干扰加噪声比 (SINR)";
                for (int i = 0; i < signalDataList.size(); i++) {
                    entries.add(new Entry(i, signalDataList.get(i).getSinr()));
                }
                break;
            case 3: // RSRQ
                paramName = "参考信号接收质量 (RSRQ)";
                for (int i = 0; i < signalDataList.size(); i++) {
                    entries.add(new Entry(i, signalDataList.get(i).getRsrq()));
                }
                break;
        }
        
        // 更新图表标题
        tvChartTitle.setText(paramName + "变化趋势");
        
        // 创建数据集
        LineDataSet dataSet = new LineDataSet(entries, paramName);
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.RED);
        dataSet.setCircleRadius(4f);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawValues(false);
        
        // 创建LineData
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        // 设置X轴标签
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
        
        // 刷新图表
        chart.invalidate();
    }
    
    // 导出图表为图片
    private void exportChart() {
        if (signalDataList.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存图表为图片
        File directory = new File(getExternalFilesDir(null), "SignalTest/Charts");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String fileName = "SignalChart_" + System.currentTimeMillis() + ".png";
        File file = new File(directory, fileName);
        
        try {
            FileOutputStream fos = new FileOutputStream(file);
            chart.saveToGallery(fileName, 100);
            fos.close();
            Toast.makeText(this, "图表导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "图表导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        storageUtils.close();
    }
}
