package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChartActivity extends AppCompatActivity {

    private static final String TAG = "ChartActivity";
    private static final int MAX_DATA_POINTS = 100; // 最大显示点数
    private static final float CHART_EXPORT_SCALE = 2.0f; // 导出图片放大倍数

    private LineChart chart;
    private Spinner paramSpinner;
    private Button btnExportChart, btnBack;
    private TextView tvChartTitle;

    private StorageUtils storageUtils;
    private List<SignalData> signalDataList;
    private List<SignalData> filteredDataList;
    private List<String> timeLabels;
    private Map<Integer, String> paramNames;

    private ExecutorService executorService;
    private Handler mainHandler;

    // 当前选中的参数索引
    private int currentParamIndex = 0;

    // 统计信息
    private float minValue = 0;
    private float maxValue = 0;
    private float avgValue = 0;
    private float currentValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        initThreading();
        initViews();
        initServices();
        initParamNames();
        loadData();
        setupChart();
        setupSpinner();
    }

    private void initThreading() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        chart = findViewById(R.id.line_chart);
        paramSpinner = findViewById(R.id.param_spinner);
        btnExportChart = findViewById(R.id.btn_export_chart);
        btnBack = findViewById(R.id.btn_back);
        tvChartTitle = findViewById(R.id.tv_chart_title);

        btnBack.setOnClickListener(v -> finish());
        btnExportChart.setOnClickListener(v -> showExportDialog());

        // 设置标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("信号数据分析");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initServices() {
        storageUtils = new StorageUtils(this);
    }

    private void initParamNames() {
        paramNames = new HashMap<>();
        paramNames.put(0, "接收电平 (RSSI)");
        paramNames.put(1, "参考信号接收功率 (RSRP)");
        paramNames.put(2, "信号与干扰加噪声比 (SINR)");
        paramNames.put(3, "参考信号接收质量 (RSRQ)");
        paramNames.put(4, "定时提前量 (TA)");
        paramNames.put(5, "物理小区ID (PCI)");
    }

    private void setupSpinner() {
        // 创建下拉列表适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>(paramNames.values()));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paramSpinner.setAdapter(adapter);

        paramSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentParamIndex = position;
                updateChartAsync(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadData() {
        signalDataList = storageUtils.getAllSignalData();
        
        // 数据太多时进行采样
        if (signalDataList.size() > MAX_DATA_POINTS) {
            filteredDataList = sampleData(signalDataList, MAX_DATA_POINTS);
        } else {
            filteredDataList = new ArrayList<>(signalDataList);
        }

        // 反转数据使最新的在右边
        Collections.reverse(filteredDataList);

        timeLabels = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);

        for (int i = 0; i < filteredDataList.size(); i++) {
            SignalData data = filteredDataList.get(i);
            if (data.getTimestamp() != null) {
                try {
                    // 根据数据量选择不同的时间格式
                    if (filteredDataList.size() > 30) {
                        // 数据点多时显示紧凑格式
                        String[] parts = data.getTimestamp().split(" ");
                        if (parts.length > 1) {
                            String[] timeParts = parts[1].split(":");
                            timeLabels.add(timeParts[0] + ":" + timeParts[1]);
                        } else {
                            timeLabels.add(String.valueOf(i));
                        }
                    } else {
                        // 数据点少时显示完整时间
                        String[] parts = data.getTimestamp().split(" ");
                        if (parts.length > 1) {
                            timeLabels.add(parts[1]);
                        } else {
                            timeLabels.add(data.getTimestamp());
                        }
                    }
                } catch (Exception e) {
                    timeLabels.add("点" + i);
                }
            } else {
                timeLabels.add("点" + i);
            }
        }
    }

    /**
     * 数据采样，保留关键数据点
     */
    private List<SignalData> sampleData(List<SignalData> original, int maxPoints) {
        if (original.size() <= maxPoints) {
            return new ArrayList<>(original);
        }

        List<SignalData> sampled = new ArrayList<>();
        int step = original.size() / maxPoints;

        for (int i = 0; i < original.size(); i += step) {
            if (sampled.size() < maxPoints) {
                sampled.add(original.get(i));
            }
        }

        // 确保包含最后一个点
        if (!sampled.contains(original.get(original.size() - 1))) {
            if (sampled.size() >= maxPoints) {
                sampled.remove(sampled.size() - 1);
            }
            sampled.add(original.get(original.size() - 1));
        }

        return sampled;
    }

    private void setupChart() {
        // 设置图表基本属性
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawBorders(true);
        chart.setBorderColor(Color.LTGRAY);
        chart.setBorderWidth(1f);

        // 设置图例
        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(45f); // 标签旋转，避免重叠

        // 设置Y轴左侧
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(-140f);
        leftAxis.setAxisMaximum(-30f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.CHINA, "%.0f dBm", value);
            }
        });

        // 设置Y轴右侧
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // 移除标记视图，使用默认实现
        chart.setMarker(null);

        // 设置值选择监听器
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < filteredDataList.size()) {
                    SignalData data = filteredDataList.get(index);
                    showDataDetails(data);
                }
            }

            @Override
            public void onNothingSelected() {
                hideDataDetails();
            }
        });
    }

    /**
     * 异步更新图表
     */
    private void updateChartAsync(int paramIndex) {
        executorService.execute(() -> {
            final ChartData chartData = prepareChartData(paramIndex);
            mainHandler.post(() -> displayChart(chartData));
        });
    }

    /**
     * 准备图表数据
     */
    private ChartData prepareChartData(int paramIndex) {
        if (filteredDataList == null || filteredDataList.isEmpty()) {
            return new ChartData(new ArrayList<>(), 0, 0, 0, 0, "");
        }

        ArrayList<Entry> entries = new ArrayList<>();
        String paramName = paramNames.get(paramIndex);
        float sum = 0;
        int validCount = 0;
        minValue = Float.MAX_VALUE;
        maxValue = Float.MIN_VALUE;

        for (int i = 0; i < filteredDataList.size(); i++) {
            SignalData data = filteredDataList.get(i);
            float value = getParameterValue(data, paramIndex);

            if (value != 0) {
                entries.add(new Entry(i, value));
                sum += value;
                validCount++;
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            } else {
                // 对于无效数据，设置为前一个有效值或0
                if (!entries.isEmpty()) {
                    entries.add(new Entry(i, entries.get(entries.size() - 1).getY()));
                } else {
                    entries.add(new Entry(i, 0));
                }
            }
        }

        avgValue = validCount > 0 ? sum / validCount : 0;
        currentValue = filteredDataList.isEmpty() ? 0 :
                getParameterValue(filteredDataList.get(filteredDataList.size() - 1), paramIndex);

        return new ChartData(entries, minValue, maxValue, avgValue, currentValue, paramName);
    }

    /**
     * 获取参数值
     */
    private float getParameterValue(SignalData data, int paramIndex) {
        switch (paramIndex) {
            case 0:
                return data.getRssi();
            case 1:
                return data.getRsrp();
            case 2:
                return data.getSinr();
            case 3:
                return data.getRsrq();
            case 4:
                return data.getTa();
            case 5:
                return data.getPci();
            default:
                return 0;
        }
    }

    /**
     * 显示图表
     */
    private void displayChart(ChartData chartData) {
        if (chartData.entries.isEmpty()) {
            tvChartTitle.setText("暂无数据");
            chart.clear();
            return;
        }

        tvChartTitle.setText(chartData.paramName + "变化趋势");

        // 创建数据集
        LineDataSet dataSet = createDataSet(chartData.entries, chartData.paramName);
        
        // 添加参考线
        addLimitLines(chartData);

        // 创建LineData
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // 设置X轴标签
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
        xAxis.setLabelCount(Math.min(10, timeLabels.size()), true);

        // 刷新图表
        chart.invalidate();
    }

    /**
     * 创建数据集
     */
    private LineDataSet createDataSet(ArrayList<Entry> entries, String paramName) {
        LineDataSet dataSet = new LineDataSet(entries, paramName);
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.RED);
        dataSet.setCircleRadius(3f);
        dataSet.setCircleHoleRadius(1.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setDrawFilled(true);
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 平滑曲线
        dataSet.setCubicIntensity(0.2f);
        
        return dataSet;
    }

    /**
     * 添加参考线
     */
    private void addLimitLines(ChartData chartData) {
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();

        // 添加平均值线
        if (chartData.avgValue != 0) {
            LimitLine avgLine = new LimitLine(chartData.avgValue, "平均值");
            avgLine.setLineWidth(1f);
            avgLine.setLineColor(Color.GREEN);
            avgLine.setTextSize(10f);
            leftAxis.addLimitLine(avgLine);
        }

        // 根据参数类型设置合适的范围
        if (currentParamIndex <= 1) { // RSSI或RSRP
            leftAxis.setAxisMinimum(Math.max(-140f, chartData.minValue - 10));
            leftAxis.setAxisMaximum(Math.min(-30f, chartData.maxValue + 10));
        } else if (currentParamIndex == 2) { // SINR
            leftAxis.setAxisMinimum(Math.max(-10f, chartData.minValue - 5));
            leftAxis.setAxisMaximum(Math.min(40f, chartData.maxValue + 5));
        }
    }



    /**
     * 显示数据详情
     */
    private void showDataDetails(SignalData data) {
        String details = String.format(Locale.CHINA,
                "时间: %s\n运营商: %s\nCGI: %s\n频点: %s\n频段: %s\nRSSI: %d dBm\nRSRP: %d dBm\nSINR: %d dB\nPCI: %d",
                data.getTimestamp(),
                data.getOperator() != null ? data.getOperator() : "未知",
                data.getCgi() != null ? data.getCgi() : "未知",
                data.getFrequency() != 0 ? data.getFrequency() : "未知",
                data.getBand() != null ? data.getBand() : "未知",
                data.getRssi(),
                data.getRsrp(),
                data.getSinr(),
                data.getPci());

        Toast.makeText(this, details, Toast.LENGTH_LONG).show();
    }

    private void hideDataDetails() {
        // 可以隐藏详情视图
    }

    /**
     * 显示导出对话框
     */
    private void showExportDialog() {
        String[] options = {"导出为PNG图片", "导出为PDF报告", "分享图表"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("导出图表")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportChartAsImage();
                            break;
                        case 1:
                            exportChartAsPdf();
                            break;
                        case 2:
                            shareChart();
                            break;
                    }
                })
                .show();
    }

    /**
     * 导出图表为图片
     */
    private void exportChartAsImage() {
        if (signalDataList.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                // 创建目录
                File directory = new File(getExternalFilesDir(null), "SignalTest/Charts");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String fileName = "SignalChart_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".png";
                File file = new File(directory, fileName);

                // 保存图表
                boolean saved = chart.saveToPath(fileName, 
                    directory.getAbsolutePath());

                mainHandler.post(() -> {
                    if (saved && file.exists()) {
                        Toast.makeText(ChartActivity.this, 
                            "图表导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        
                        // 询问是否分享
                        promptShare(file);
                    } else {
                        Toast.makeText(ChartActivity.this, 
                            "图表导出失败", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
                mainHandler.post(() -> Toast.makeText(ChartActivity.this, 
                    "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 导出图表为PDF
     */
    private void exportChartAsPdf() {
        executorService.execute(() -> {
            try {
                // 创建PDF文档
                PdfDocument document = new PdfDocument();
                
                // 创建页面
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        chart.getWidth(), chart.getHeight() + 200, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);

                // 绘制图表
                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();
                
                // 绘制标题
                paint.setColor(Color.BLACK);
                paint.setTextSize(30f);
                paint.setFakeBoldText(true);
                canvas.drawText(tvChartTitle.getText().toString(), 50, 50, paint);
                
                // 绘制统计信息
                paint.setTextSize(20f);
                paint.setFakeBoldText(false);
                canvas.drawText("统计信息: 图表数据", 50, 90, paint);
                
                // 绘制图表
                canvas.save();
                canvas.translate(0, 120);
                chart.draw(canvas);
                canvas.restore();

                document.finishPage(page);

                // 保存PDF
                File directory = new File(getExternalFilesDir(null), "SignalTest/Reports");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String fileName = "SignalReport_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".pdf";
                File file = new File(directory, fileName);

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    document.writeTo(fos);
                }

                document.close();

                mainHandler.post(() -> {
                    Toast.makeText(ChartActivity.this, 
                        "PDF报告导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    promptShare(file);
                });

            } catch (IOException e) {
                Log.e(TAG, "PDF export failed", e);
                mainHandler.post(() -> Toast.makeText(ChartActivity.this, 
                    "PDF导出失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 分享图表
     */
    private void shareChart() {
        exportChartAsImage(); // 先导出图片，然后分享
    }

    /**
     * 提示分享文件
     */
    private void promptShare(File file) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("文件已保存")
                .setMessage("文件保存在: " + file.getAbsolutePath() + "\n是否分享？")
                .setPositiveButton("分享", (dialog, which) -> shareFile(file))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 分享文件
     */
    private void shareFile(File file) {
        Uri fileUri;
        
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, "分享图表"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshData();
            return true;
        } else if (id == R.id.action_export) {
            showExportDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        Toast.makeText(this, "刷新数据...", Toast.LENGTH_SHORT).show();
        loadData();
        updateChartAsync(currentParamIndex);
    }

    /**
     * 切换网格显示
     */
    private void toggleGrid() {
        XAxis xAxis = chart.getXAxis();
        YAxis leftAxis = chart.getAxisLeft();
        
        boolean drawGrid = !xAxis.isDrawGridLinesEnabled();
        xAxis.setDrawGridLines(drawGrid);
        leftAxis.setDrawGridLines(drawGrid);
        chart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (storageUtils != null) {
            storageUtils.close();
        }
    }



    /**
     * 图表数据容器类
     */
    private static class ChartData {
        ArrayList<Entry> entries;
        float minValue;
        float maxValue;
        float avgValue;
        float currentValue;
        String paramName;

        ChartData(ArrayList<Entry> entries, float minValue, float maxValue,
                  float avgValue, float currentValue, String paramName) {
            this.entries = entries;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.avgValue = avgValue;
            this.currentValue = currentValue;
            this.paramName = paramName;
        }
    }
}