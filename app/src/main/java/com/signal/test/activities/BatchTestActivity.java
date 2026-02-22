package com.signal.test.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import com.signal.test.services.SignalCollector;
import com.signal.test.services.LocationService;
import com.signal.test.models.SignalData;
import com.signal.test.utils.StorageUtils;
import com.signal.test.utils.ExportUtils;
import com.signal.test.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchTestActivity extends AppCompatActivity {

    private EditText etTestCount, etInterval;
    private Button btnStartTest, btnStopTest, btnBack, btnGenerateReport;
    private TextView tvStatus, tvProgress;
    
    private SignalCollector signalCollector;
    private LocationService locationService;
    private StorageUtils storageUtils;
    private ExportUtils exportUtils;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private AtomicBoolean isTesting = new AtomicBoolean(false);
    private AtomicInteger testCount = new AtomicInteger(0);
    private AtomicInteger totalTests = new AtomicInteger(0);
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failCount = new AtomicInteger(0);
    
    private int interval = 0;
    private CopyOnWriteArrayList<SignalData> batchTestData;
    private long startTime = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_test);
        
        initViews();
        initServices();
        initHandlers();
    }
    
    private void initViews() {
        etTestCount = findViewById(R.id.et_test_count);
        etInterval = findViewById(R.id.et_interval);
        btnStartTest = findViewById(R.id.btn_start_test);
        btnStopTest = findViewById(R.id.btn_stop_test);
        btnBack = findViewById(R.id.btn_back);
        btnGenerateReport = findViewById(R.id.btn_generate_report);
        tvStatus = findViewById(R.id.tv_status);
        tvProgress = findViewById(R.id.tv_progress);
        
        btnStartTest.setOnClickListener(v -> startBatchTest());
        btnStopTest.setOnClickListener(v -> stopBatchTest());
        btnBack.setOnClickListener(v -> finish());
        btnGenerateReport.setOnClickListener(v -> generateReport());
        
        btnStopTest.setEnabled(false);
        btnGenerateReport.setEnabled(false);
        
        // 设置输入框默认值
        etTestCount.setText("10");
        etInterval.setText("5");
    }
    
    private void initServices() {
        signalCollector = new SignalCollector(this);
        locationService = new LocationService(this);
        storageUtils = new StorageUtils(this);
        exportUtils = new ExportUtils(this);
        locationService.startLocationUpdates();
    }
    
    private void initHandlers() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    private void startBatchTest() {
        // 验证输入
        if (!validateInputs()) {
            return;
        }
        
        // 检查位置服务
        if (!locationService.hasValidLocation()) {
            Toast.makeText(this, "正在获取位置，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 初始化测试数据
        initializeTest();
        
        // 开始批量测试
        startTime = System.currentTimeMillis();
        executorService.execute(this::runBatchTest);
    }
    
    private boolean validateInputs() {
        String countStr = etTestCount.getText().toString().trim();
        String intervalStr = etInterval.getText().toString().trim();
        
        if (countStr.isEmpty() || intervalStr.isEmpty()) {
            Toast.makeText(this, "请输入测试次数和间隔时间", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            totalTests.set(Integer.parseInt(countStr));
            interval = Integer.parseInt(intervalStr);
            
            if (totalTests.get() <= 0 || interval <= 0) {
                Toast.makeText(this, "测试次数和间隔时间必须大于0", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (interval < 1) {
                Toast.makeText(this, "间隔时间不能小于1秒", Toast.LENGTH_SHORT).show();
                return false;
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void initializeTest() {
        testCount.set(0);
        successCount.set(0);
        failCount.set(0);
        batchTestData = new CopyOnWriteArrayList<>();
        isTesting.set(true);
        startTime = System.currentTimeMillis();
        
        // 更新UI状态
        mainHandler.post(() -> {
            btnStartTest.setEnabled(false);
            btnStopTest.setEnabled(true);
            btnGenerateReport.setEnabled(false);
            tvStatus.setText("测试中...");
            tvProgress.setText(String.format(Locale.CHINA, 
                "进度: 0/%d | 成功: 0 | 失败: 0", totalTests.get()));

        });
    }
    
    private void runBatchTest() {
        while (isTesting.get() && testCount.get() < totalTests.get()) {
            performSingleTest();
            
            testCount.incrementAndGet();
            
            updateProgress();
            
            // 如果不是最后一次测试，等待指定间隔
            if (testCount.get() < totalTests.get()) {
                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        completeTest();
    }
    
    private void performSingleTest() {
        int maxRetries = 2;
        boolean success = false;
        
        for (int retry = 0; retry < maxRetries && !success; retry++) {
            try {
                // 强制刷新信号数据
                signalCollector.refreshSignalData();
                Thread.sleep(100); // 等待信号采集
                
                SignalData data = signalCollector.getSignalData();
                
                // 获取位置信息
                android.location.Location location = locationService.getCurrentLocation();
                if (location != null) {
                    data.setLatitude(location.getLatitude());
                    data.setLongitude(location.getLongitude());
                    data.setLocation(locationService.getLocationDescription());
                } else {
                    data.setLocation("位置不可用");
                }
                
                data.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", 
                    Locale.CHINA).format(new Date()));
                
                // 保存数据
                batchTestData.add(data);
                storageUtils.insertSignalData(data);
                
                successCount.incrementAndGet();
                success = true;
                
            } catch (Exception e) {
                e.printStackTrace();
                if (retry == maxRetries - 1) {
                    failCount.incrementAndGet();
                    final int currentTest = testCount.get() + 1;
                    mainHandler.post(() -> 
                        Toast.makeText(BatchTestActivity.this, 
                            "第" + currentTest + "次测试失败", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    try {
                        Thread.sleep(1000); // 重试前等待
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
    
    private void updateProgress() {
        mainHandler.post(() -> {
            tvProgress.setText(String.format(Locale.CHINA, 
                "进度: %d/%d | 成功: %d | 失败: %d", 
                testCount.get(), totalTests.get(), 
                successCount.get(), failCount.get()));
            
            if (testCount.get() < totalTests.get() && startTime > 0) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long avgTimePerTest = elapsedTime / testCount.get();
                long remainingTests = totalTests.get() - testCount.get();
                long remainingTime = remainingTests * avgTimePerTest / 1000;
            }
            
            // 显示最新信号值
            if (!batchTestData.isEmpty()) {
                SignalData lastData = batchTestData.get(batchTestData.size() - 1);
                String signalStr = lastData.getNetworkType() != null && 
                    lastData.getNetworkType().equals("5G") ?
                    "RSRP: " + lastData.getRsrp() + " dBm" :
                    "RSSI: " + lastData.getRssi() + " dBm";
                
                Toast.makeText(BatchTestActivity.this, 
                    "最新信号: " + signalStr, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void completeTest() {
        isTesting.set(false);
        mainHandler.post(() -> {
            String result = String.format(Locale.CHINA, 
                "测试完成 | 总次数: %d | 成功: %d | 失败: %d", 
                totalTests.get(), successCount.get(), failCount.get());
            tvStatus.setText(result);
            tvProgress.setText(result);
            
            btnStartTest.setEnabled(true);
            btnStopTest.setEnabled(false);
            btnGenerateReport.setEnabled(!batchTestData.isEmpty());
            
            Toast.makeText(BatchTestActivity.this, 
                "批量测试完成，成功率: " + 
                String.format(Locale.CHINA, "%.1f%%", 
                    (successCount.get() * 100.0f / totalTests.get())), 
                Toast.LENGTH_LONG).show();
        });
    }
    
    private void stopBatchTest() {
        isTesting.set(false);
        tvStatus.setText("测试已停止");
        btnStartTest.setEnabled(true);
        btnStopTest.setEnabled(false);
        btnGenerateReport.setEnabled(!batchTestData.isEmpty() && successCount.get() > 0);
    }
    
    private void generateReport() {
        if (batchTestData == null || batchTestData.isEmpty()) {
            Toast.makeText(this, "没有测试数据可生成报告", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (successCount.get() == 0) {
            Toast.makeText(this, "没有成功采集的数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnGenerateReport.setEnabled(false);
        
        executorService.execute(() -> {
            String reportPath = exportUtils.generateBatchTestReport(
                new ArrayList<>(batchTestData), 
                totalTests.get(), 
                successCount.get(), 
                failCount.get(),
                interval
            );
            
            final String result = reportPath;
            mainHandler.post(() -> {
                btnGenerateReport.setEnabled(true);
                if (result != null) {
                    Toast.makeText(BatchTestActivity.this, 
                        "报告生成成功: " + result, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(BatchTestActivity.this, 
                        "报告生成失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        locationService.startLocationUpdates();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        locationService.stopLocationUpdates();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isTesting.set(false);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        locationService.stopLocationUpdates();
        if (storageUtils != null) {
            storageUtils.close();
        }
    }
}