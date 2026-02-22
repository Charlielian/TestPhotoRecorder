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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private boolean isTesting = false;
    private int testCount = 0;
    private int totalTests = 0;
    private int interval = 0;
    private List<SignalData> batchTestData;
    
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
        String countStr = etTestCount.getText().toString().trim();
        String intervalStr = etInterval.getText().toString().trim();
        
        if (countStr.isEmpty() || intervalStr.isEmpty()) {
            Toast.makeText(this, "请输入测试次数和间隔时间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            totalTests = Integer.parseInt(countStr);
            interval = Integer.parseInt(intervalStr);
            
            if (totalTests <= 0 || interval <= 0) {
                Toast.makeText(this, "测试次数和间隔时间必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 初始化测试数据 - 使用线程安全的列表
        testCount = 0;
        batchTestData = Collections.synchronizedList(new ArrayList<>());
        isTesting = true;
        
        // 更新UI状态
        btnStartTest.setEnabled(false);
        btnStopTest.setEnabled(true);
        btnGenerateReport.setEnabled(false);
        tvStatus.setText("测试中...");
        tvProgress.setText("进度: 0/" + totalTests);
        
        // 开始批量测试
        executorService.execute(() -> {
            while (isTesting && testCount < totalTests) {
                // 执行单次测试
                performSingleTest();
                
                // 增加测试计数
                testCount++;
                
                // 更新进度
                final int currentCount = testCount;
                mainHandler.post(() -> {
                    tvProgress.setText("进度: " + currentCount + "/" + totalTests);
                });
                
                // 如果不是最后一次测试，等待指定间隔
                if (testCount < totalTests) {
                    try {
                        Thread.sleep(interval * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // 测试完成
            isTesting = false;
            mainHandler.post(() -> {
                tvStatus.setText("测试完成");
                btnStartTest.setEnabled(true);
                btnStopTest.setEnabled(false);
                btnGenerateReport.setEnabled(!batchTestData.isEmpty());
                Toast.makeText(BatchTestActivity.this, "批量测试完成", Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    private void performSingleTest() {
        // 获取信号数据
        SignalData data = signalCollector.getSignalData();
        
        // 更新位置信息和时间戳
        data.setLocation(locationService.getLocationDescription());
        data.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        // 保存到批量测试数据
        batchTestData.add(data);
        
        // 保存到数据库
        storageUtils.insertSignalData(data);
    }
    
    private void stopBatchTest() {
        isTesting = false;
        tvStatus.setText("测试已停止");
        btnStartTest.setEnabled(true);
        btnStopTest.setEnabled(false);
        btnGenerateReport.setEnabled(!batchTestData.isEmpty());
    }
    
    private void generateReport() {
        if (batchTestData.isEmpty()) {
            Toast.makeText(this, "没有测试数据可生成报告", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 生成报告
        String reportPath = exportUtils.generateBatchTestReport(batchTestData);
        
        if (reportPath != null) {
            Toast.makeText(this, "报告生成成功: " + reportPath, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "报告生成失败", Toast.LENGTH_SHORT).show();
        }
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
        isTesting = false;
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        locationService.stopLocationUpdates();
        storageUtils.close();
    }
}