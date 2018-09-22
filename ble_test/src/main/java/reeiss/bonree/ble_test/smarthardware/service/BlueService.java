package reeiss.bonree.ble_test.smarthardware.service;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import org.litepal.LitePal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import reeiss.bonree.ble_test.LocationApplication;
import reeiss.bonree.ble_test.R;
import reeiss.bonree.ble_test.bean.BleDevConfig;
import reeiss.bonree.ble_test.bean.Location;
import reeiss.bonree.ble_test.bean.PreventLosingCommon;
import reeiss.bonree.ble_test.bean.WuRaoWifiConfig;
import reeiss.bonree.ble_test.blehelp.XFBluetooth;
import reeiss.bonree.ble_test.blehelp.XFBluetoothCallBack;
import reeiss.bonree.ble_test.utils.ScreenManager;
import reeiss.bonree.ble_test.utils.ScreenReceiverUtil;
import reeiss.bonree.ble_test.utils.T;

import static reeiss.bonree.ble_test.bean.CommonHelp.getLinkLostAlert;
import static reeiss.bonree.ble_test.bean.CommonHelp.getOnClick;
import static reeiss.bonree.ble_test.bean.PreventLosingCommon.Dev_Type_Shuidi;
import static reeiss.bonree.ble_test.blehelp.XFBluetooth.CURRENT_DEV_MAC;
import static reeiss.bonree.ble_test.blehelp.XFBluetooth.getCurrentDevConfig;

public class BlueService extends Service {

    int GRAY_SERVICE_ID = 1001;
    private XFBluetooth xfBluetooth;
    private MediaPlayer mPlayer;
    private ServiceHandler handler;
    private boolean dontAlert;
    private LocationApplication locationApplication;
    private AlertDialog alertDialog;
    private long lastTimeMillis;
    private XFBluetoothCallBack gattCallback = new XFBluetoothCallBack() {

        //链接状态发生改变
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {

            BleDevConfig currentDevConfig = getCurrentDevConfig();
            if (currentDevConfig == null) {
                currentDevConfig = getCurrentDevConfig(gatt.getDevice().getAddress());
            }
            final BleDevConfig finalCurrentDevConfig = currentDevConfig;
            connectionStateChange(finalCurrentDevConfig, status, newState);
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                PreventLosingCommon.setDeviceType(xfBluetooth.getXFBluetoothGatt());

                Log.e("JerryZhu", "onServicesDiscovered: 服务扫描成功，开启按键通知！");
                BluetoothGattCharacteristic chOnclick = getOnClick(xfBluetooth.getXFBluetoothGatt());
                boolean isEnable = xfBluetooth.getXFBluetoothGatt().setCharacteristicNotification(chOnclick, true);
                if (isEnable) {
                    List<BluetoothGattDescriptor> descriptorList = chOnclick.getDescriptors();
                    if (descriptorList != null && descriptorList.size() > 0) {
                        for (BluetoothGattDescriptor descriptor : descriptorList) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("JerryZhu", "onServicesDiscovered: 写入按键通知成功" + Arrays.toString(characteristic.getValue()));
            } else {
                Log.e("JerryZhu", "onServicesDiscovered: 写入按键通知失败" + Arrays.toString(characteristic.getValue()));
            }
            if (PreventLosingCommon.Dev_Type != Dev_Type_Shuidi) {
                BluetoothGattCharacteristic linkLostAlert = getLinkLostAlert(xfBluetooth.getXFBluetoothGatt());
                Log.e("JerryZhu", "onServicesDiscovered:  监测到不是水滴！" + linkLostAlert);
                if (linkLostAlert == null) return;
                if (!characteristic.getUuid().equals(linkLostAlert.getUuid())) {
                    Log.e("JerryZhu", "onServicesDiscovered: 正在开启报警");
                    //说明不是写入开启报警返回的，需要写入开启报警
                    linkLostAlert.setValue(new byte[]{1});
                    boolean b = xfBluetooth.getXFBluetoothGatt().writeCharacteristic(linkLostAlert);
                    Log.e("JerryZhu", "写入开启报警: " + b);
                } /*else {
                            //说明是写入报警返回的，排除水滴
                        }*/
            }
        }

        //通知操作的回调（此处接收BLE设备返回数据） 点击返回1
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            String value = Arrays.toString(characteristic.getValue());
            if (PreventLosingCommon.Dev_Type == Dev_Type_Shuidi) {
                long currentTimeMillis = System.currentTimeMillis();
                if ((currentTimeMillis - lastTimeMillis) < 500) {
                    if (value.equals("[1]")) {
                        value = "[2]";
                        lastTimeMillis = 0;
                    }
                } else lastTimeMillis = currentTimeMillis;

            }
            if (value.equals("[2]")) {
                Message message = handler.obtainMessage(100);
//                message.what = 100;
                handler.sendMessage(message);
            }
        }

    };
    // 动态注册锁屏等广播
    private ScreenReceiverUtil mScreenListener;
    // 1像素Activity管理类
    private ScreenManager mScreenManager;
    // 代码省略...
    private ScreenReceiverUtil.SreenStateListener
            mScreenListenerer = new ScreenReceiverUtil.SreenStateListener() {
        @Override
        public void onSreenOn() {
            // 移除"1像素"
            mScreenManager.finishActivity();
        }

        @Override
        public void onSreenOff() {
            // 接到锁屏广播，将SportsActivity切换到可见模式
            // "咕咚"、"乐动力"、"悦动圈"就是这么做滴
            // Intent intent =
            //new Intent(SportsActivity.this,SportsActivity.class);
            // startActivity(intent);
            // 如果你觉得，直接跳出SportActivity很不爽
            // 那么，我们就制造个"1像素"惨案
            mScreenManager.startActivity();
        }

        @Override
        public void onUserPresent() {
            // 解锁，暂不用，保留
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("jerry", "onCreate: ");

        // 默认情况下Service是运行在主线程中，而服务一般又十分耗费时间，如果
        // 放在主线程中，将会影响程序与用户的交互，因此把Service
        // 放在一个单独的线程中执行
        HandlerThread thread = new HandlerThread("MessageDemoThread", Thread.MAX_PRIORITY);
        thread.start();
        // 获取当前线程中的looper对象
        Looper looper = thread.getLooper();
        //创建Handler对象，把looper传递过来使得handler、
        //looper和messageQueue三者建立联系
        handler = new ServiceHandler(looper);

    }

    /**
     * Notification
     */
    public void createNotification() {
        //使用兼容版本
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        //设置状态栏的通知图标
        builder.setSmallIcon(R.mipmap.ic_launcher);
        //设置通知栏横条的图标
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.device_main));
        //禁止用户点击删除按钮删除
        builder.setAutoCancel(false);
        //禁止滑动删除
        builder.setOngoing(true);
        //右上角的时间显示
        builder.setShowWhen(true);
        //设置通知栏的标题内容
        builder.setContentTitle("防丢器正在后台运行");
        //创建通知
        Notification notification = builder.build();
        //设置为前台服务
        startForeground(GRAY_SERVICE_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
     /*   //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，此方法能有效隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25) {
            //Android4.3 - Android7.0，此方法能有效隐藏Notification上的图标
            Intent innerIntent = new Intent(this, BlueService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            //Android7.1 google修复了此漏洞，暂无解决方法（现状：Android7.1以上app启动后通知栏会出现一条"正在运行"的通知消息）
            startForeground(GRAY_SERVICE_ID, new Notification());
        }*/
        Log.e("jerry", "onStartCommand: ");
        createNotification();
        // 1. 注册锁屏广播监听器
        mScreenListener = new ScreenReceiverUtil(this);
        mScreenManager = ScreenManager.getScreenManagerInstance(this);
        mScreenListener.setScreenReceiverListener(mScreenListenerer);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("jerry", "onBind: ");
        return new MyBinder();
    }

    //双击寻找手机
    private void FoundPhone() {
        //在勿扰 true
        if (checkWuRao()) return;
        if (mPlayer != null && mPlayer.isPlaying()) {
            return;
        }
        BleDevConfig currentDevConfig = XFBluetooth.getCurrentDevConfig();
        try {
            mPlayer = new MediaPlayer();
            assert currentDevConfig != null;
            Uri setDataSourceuri = Uri.parse("android.resource://reeiss.bonree.ble_test/" + currentDevConfig.getRingResId());
            mPlayer.setDataSource(this, setDataSourceuri);
            mPlayer.prepare();
            mPlayer.setLooping(true);
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
                 /*   final MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), currentDev.getRingResId());//重新设置要播放的音频
                    mediaPlayer.start();*/
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.AlertDialog);
        b.setTitle("寻找手机");
        b.setMessage(currentDevConfig.getAlias().isEmpty() ? xfBluetooth.getXFBluetoothGatt().getDevice().getName() : currentDevConfig.getAlias());
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mPlayer != null) {
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                }
                T.show(BlueService.this, "取消");
            }
        });
        AlertDialog alertDialog = b.setCancelable(false).create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        Log.e("jerryzhu", "service 弹窗  ");
        alertDialog.show();
    }

    //勿扰是否打开，是否在勿扰区域  在勿扰true 不在false
    private boolean checkWuRao() {
        SharedPreferences myPreference = (getSharedPreferences("myPreference", Context.MODE_PRIVATE));
        boolean isOpenWuRao = myPreference.getBoolean("isOpenWuRao", false);
        if (isOpenWuRao) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm == null) return false;
            // 获取当前所连接wifi的信息
            final WifiInfo wi = wm.getConnectionInfo();
            if (wi == null) return false;
            final String macAddress = wi.getMacAddress();
            WuRaoWifiConfig has = LitePal.where("wifiMac=?", macAddress).findFirst(WuRaoWifiConfig.class);
            return has != null;
        }
        return false;
    }

    /**
     * 链接状态发生改变
     *
     * @param
     * @param status
     * @param newState
     */
    private void connectionStateChange(BleDevConfig currentDevConfig, int status, final int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            PreventLosingCommon.Dev_Type = -1;
            if (locationApplication != null) {
                if (locationApplication.locationService.isStart()) {
                    Log.e("jerryzhu", "first 停止定位: ");
                    locationApplication.locationService.stop();
                }
            }
            if (!dontAlert) { //手动断开为true，不需要报警
                if (locationApplication != null && locationApplication.mLocation != null &&
                        !TextUtils.isEmpty(locationApplication.mLocation.getMac())) {
                    boolean save = locationApplication.mLocation.save();
                    T.show(this, "丢失位置已保存！");
                    if (save)
                        locationApplication.mLocation = new Location();
                }

                //如果开启勿扰，并且当前wifi在设置区域内    开始报警
                //在勿扰true 不在false
                if (currentDevConfig != null) {
                    //true 在勿扰区域，不报警 return
                    Message message = handler.obtainMessage(200);
                    message.obj = currentDevConfig;
                    handler.sendMessage(message);
                }
            } else {
                dontAlert = false;
            }
        }

//        BleDevConfig currentDevConfig = XFBluetooth.getCurrentDevConfig();
        //已连接
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            BluetoothGatt xfBluetoothGatt = xfBluetooth.getXFBluetoothGatt();
            xfBluetoothGatt.discoverServices();
            if (locationApplication != null && !locationApplication.locationService.isStart()) {
                Log.e("jerryzhu", " 定位开启: ");
                locationApplication.locationService.start();
            }

            if (currentDevConfig == null) {
                Field[] fields = R.raw.class.getDeclaredFields();
                try {
                    currentDevConfig = new BleDevConfig(CURRENT_DEV_MAC, xfBluetoothGatt.getDevice().getName(), fields[1].getName(), 0, fields[1].getInt(R.raw.class));
                    currentDevConfig.save();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //报警，断开连接或者超出范围
    private boolean PhoneAlert(BleDevConfig currentDevConfig, int type) {
        if (checkWuRao()) return true;//在勿扰true 不在false

        if (mPlayer != null && mPlayer.isPlaying()) {
            return true;
        }
        try {
            mPlayer = new MediaPlayer();
            Uri setDataSourceuri = Uri.parse("android.resource://reeiss.bonree.ble_test/" + currentDevConfig.getRingResId());
            mPlayer.setDataSource(this, setDataSourceuri);
            mPlayer.prepare();
            mPlayer.setLooping(true);
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
//                            mPlayer.setVolume(2f, 2f);

        AlertDialog.Builder dialogAlert = new AlertDialog.Builder(this, R.style.AlertDialog);
        dialogAlert.setTitle("丢失报警")
                .setCancelable(false)
                .setMessage(type == 0 ? "防丢器已断开连接！" : "防丢器位置超出范围！")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mPlayer != null) {
                            mPlayer.stop();
                            mPlayer.release();
                            mPlayer = null;
                        }
                        alertDialog = null;
                    }
                });
        alertDialog = dialogAlert.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();

        return false;
    }

    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    FoundPhone();
                    break;
                case 200:
                    BleDevConfig currentDevConfig = (BleDevConfig) msg.obj;
                    if (PhoneAlert(currentDevConfig, 0)) return;
                    break;
            }
        }
    }


    private class MyBinder extends Binder implements IService {
        BlueService getService() {
            // 返回当前对象LocalService,这样我们就可在客户端端调用Service的公共方法了
            return BlueService.this;
        }

        @Override
        public void init(String path) {
            xfBluetooth = XFBluetooth.getInstance(getApplicationContext());
            xfBluetooth.addBleCallBack(gattCallback);
        }

        @Override
        public void connect(String mac) {
            xfBluetooth.connect(mac);
        }

        @Override
        public void setDontAlert(boolean isDontAlert) {
            dontAlert = isDontAlert;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScreenListener.stopScreenReceiverListener();
    }
}