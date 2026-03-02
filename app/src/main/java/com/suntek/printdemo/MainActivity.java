package com.suntek.printdemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.printer.sdk.PrinterInstance;
import com.printer.sdk.exception.ParameterErrorException;
import com.printer.sdk.exception.PrinterPortNullException;
import com.printer.sdk.exception.WriteException;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "com.suntek.printdemo.USB_PERMISSION";
    private PrinterInstance mPrinter;
    private UsbDevice mUsbDevice;
    private Button btnConnect, btnOpenCashbox , btn_write_txt;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initUI();
        setupClickListeners();
    }

    private void initUI() {
        btnConnect = findViewById(R.id.btn_connect);
        btnOpenCashbox = findViewById(R.id.btn_open_cashbox);
        btn_write_txt = findViewById(R.id.btn_write_txt);
        tvStatus = findViewById(R.id.tv_status);

        btn_write_txt.setEnabled(false);
        // 初始状态
        btnOpenCashbox.setEnabled(false);

        tvStatus.setText("状态：未连接");
    }

    private void setupClickListeners() {
        btnConnect.setOnClickListener(v -> initUSBConnection());
        btnOpenCashbox.setOnClickListener(v -> openCashBox());
        btn_write_txt.setOnClickListener(v->printTxt());
    }

    private void printTxt() {

            if (mPrinter == null) {
                showStatus("打印机未连接");
                return;
            }

            new Thread(() -> {
                try {
                    // --- 修改为设置GB2312编码的原始指令 ---
                    // 指令: 1B 28 43 3
                    byte[] gb2312Command = new byte[]{0x1B, 0x28, 0x43, 0x3};
                    mPrinter.sendBytesData(gb2312Command);

                    String msg = "\n";
                    mPrinter.printText(msg);

                    runOnUiThread(() -> {
                        showStatus("文字已打印");
                        Toast.makeText(MainActivity.this, "文字已打印", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() ->
                            showStatus("打印失败: " + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();
    }




    private void initUSBConnection() {
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);

        if (usbManager == null) {
            showStatus("无法获取USB服务");
            return;
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            showStatus("未找到USB设备");
            return;
        }

        // 在 initUSBConnection() 方法中，获取 deviceList 后添加：
        for (UsbDevice device : deviceList.values()) {
            Log.d("USB-DEBUG", String.format("Found device: %s, VID: %04X, PID: %04X",
                    device.getDeviceName(), device.getVendorId(), device.getProductId()));
        }


        // 查找打印机设备
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (isPrinterDevice(device)) {
                mUsbDevice = device;
                break;
            }
        }

        if (mUsbDevice == null) {
            showStatus("未找到打印机设备");
            return;
        }
        // --- 新增的权限检查和请求逻辑 ---
        if (usbManager != null && usbManager.hasPermission(mUsbDevice)) {
            // 如果已经有权限，直接连接
            connectPrinter(usbManager);
        } else {
            // 如果没有权限，发起权限请求
            // 注意：这里的PendingIntent会在权限请求结果返回时被触发
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(mUsbDevice, permissionIntent);
            showStatus("正在请求USB权限...");
        }

    }
    // 将原来的连接逻辑封装成一个新方法
    private void connectPrinter(UsbManager usbManager) {
        Handler connectionHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.w("TAG","最终的信息"+msg.what);
                switch (msg.what) {
                    case 101:
                    case 102:
                    case 103:
                        showStatus("打印机连接成功");
                        btnOpenCashbox.setEnabled(true);
                        btn_write_txt.setEnabled(true);
                        break;
//                    case PrinterInstance.CONNECT_FAILED:
//                        showStatus("打印机连接失败");
//                        btnOpenCashbox.setEnabled(false);
//                        break;
//                    case PrinterInstance.CONNECT_CLOSED:
//                        showStatus("打印机连接关闭");
//                        btnOpenCashbox.setEnabled(false);
//                        break;
                }
            }
        };

        mPrinter = PrinterInstance.getPrinterInstance(this, mUsbDevice, connectionHandler);
        if (mPrinter != null) {
            mPrinter.openConnection();
            showStatus("正在连接打印机...");
        }
    }
    private boolean isPrinterDevice(UsbDevice device) {
        // 根据您的打印机设置正确的识别逻辑 已启动设备 USB\VID_0483&PID_5720\2510134290。
        // 示例：return device.getVendorId() == 0x1234 && device.getProductId() == 0x5678;
        // 替换成你打印机的实际 VID 和 PID
        final int PRINTER_VENDOR_ID = 0x0483; // 示例厂商ID
        final int PRINTER_PRODUCT_ID = 0x5720; // 示例产品ID

        return device.getVendorId() == PRINTER_VENDOR_ID && device.getProductId() == PRINTER_PRODUCT_ID;
        //return true; // 测试时返回true
    }

    private void openCashBox() {
        if (mPrinter == null) {
            showStatus("Printer is not connected");
            return;
        }

        new Thread(() -> {
            try {
                //mPrinter.openCashBoxTSPL(1,0);
                mPrinter.openCashbox(true,false);//This is used to open the cash box
//                mPrinter.beepTSPL(2,3);
            //    mPrinter.ringBuzzer(3);
            //    mPrinter.printSelfTestTSPL();
                runOnUiThread(() -> {
                    showStatus("The cash box is open.");
                    Toast.makeText(MainActivity.this, "The cash box is open.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        showStatus("Cash drawer failed to open: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void showStatus(String message) {
        runOnUiThread(() -> tvStatus.setText("状态：" + message));
    }
    //---------
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // 权限被授予，开始连接
                            showStatus("USB权限已获取");
                            connectPrinter(usbManager);
                        }
                    } else {
                        // 权限被拒绝
                        showStatus("USB权限被拒绝");
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // 添加RECEIVER_NOT_EXPORTED标志，表示此接收器不接收其他应用的广播
        registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }


    @Override
    protected void onPause() {
        super.onPause();
        // 注销广播接收器
        unregisterReceiver(usbReceiver);
    }

    //---------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPrinter != null) {
            mPrinter.closeConnection();
        }
    }
}