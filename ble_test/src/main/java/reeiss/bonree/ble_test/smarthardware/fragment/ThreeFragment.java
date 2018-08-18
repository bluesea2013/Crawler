package reeiss.bonree.ble_test.smarthardware.fragment;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;

import reeiss.bonree.ble_test.LocationApplication;
import reeiss.bonree.ble_test.LocationService;
import reeiss.bonree.ble_test.R;
import reeiss.bonree.ble_test.bean.BleDevConfig;
import reeiss.bonree.ble_test.bean.Location;
import reeiss.bonree.ble_test.blehelp.XFBluetooth;
import reeiss.bonree.ble_test.blehelp.XFBluetoothCallBack;
import reeiss.bonree.ble_test.smarthardware.activity.LostHistory;

/**
 * Wang YaHui
 * 2018/6/1822:49
 */

public class ThreeFragment extends Fragment {

    private TextureMapView map;
    private BaiduMap mBaiduMap;
    private LocationService locationService;
    private Location mLocation = new Location();
    //    private boolean isLost = false;
    private XFBluetoothCallBack gattCallback = new XFBluetoothCallBack() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("jerryzhu3", "断开了 ");
                //断开连接，首先停止定位服务
                if (locationService.isStart()) {
                    Log.e("jerryzhu3", "断开了 停止定位: ");
                    locationService.stop();
                }
//                isLost = true;
                //如果断开连接，就保存到数据库
                boolean save = mLocation.save();
                if (save)
                    mLocation = new Location();
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("jerryzhu3", "已连接: ");
//                isLost = false;
                if (!locationService.isStart()) {
                    Log.e("jerryzhu3", " 定位开启: ");
                    locationService.start();
                }
            }
        }
    };
    /***
     * 接收定位结果消息，并显示在地图上
     */
    @SuppressLint("HandlerLeak")
    private Handler locHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            try {
                BDLocation location = msg.getData().getParcelable("loc");
                int isCal = msg.getData().getInt("iscalculate");
                if (location != null) {             //纬度                        //经度
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    BleDevConfig currentDevConfig = XFBluetooth.getCurrentDevConfig();

                    if (currentDevConfig != null) {
                        Log.e("JerryZhu", "当前位置Str: " + location.getAddrStr() + "    描述：" + location.getLocationDescribe() + "  ==  " + currentDevConfig.getAlias() + "   MAC =   " + currentDevConfig.getMac());
                        mLocation.setMac(currentDevConfig.getMac());
                        mLocation.setName(currentDevConfig.getAlias());
                    } else {
                        Log.e("JerryZhu", "空！！！！！！！！当前位置Str: " + location.getAddrStr() + "    描述：" + location.getLocationDescribe());
                    }
                    mLocation.setTime(System.currentTimeMillis());
                    mLocation.setLatitude(location.getLatitude());
                    mLocation.setLongitude(location.getLongitude());
                    mLocation.setAddStr(location.getAddrStr());
                    mLocation.setLocationDescribe(location.getLocationDescribe());

//                    T.show(getActivity(), location.getLocationDescribe());
                    // 构建Marker图标
                    BitmapDescriptor bitmap = null;
                    if (isCal == 0) {
                        bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.icon_openmap_mark); // 非推算结果
                    } else {
                        bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.icon_openmap_focuse_mark); // 推算结果
                    }

                    // 构建MarkerOption，用于在地图上添加Marker
                    OverlayOptions option = new MarkerOptions().position(point).icon(bitmap);
                    // 在地图上添加Marker，并显示
                    mBaiduMap.addOverlay(option);
                    mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(point));
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    };
    BDAbstractLocationListener listener = new BDAbstractLocationListener() {
        //百度地图定位回调
        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub

            if (location != null && (location.getLocType() == 161 || location.getLocType() == 66)) {
                Message locMsg = locHandler.obtainMessage();
                Bundle locData;
                locData = Algorithm(location);
                if (locData != null) {
                    locData.putParcelable("loc", location);
                    locMsg.setData(locData);
                    locHandler.sendMessage(locMsg);
                }
            }
        }
    };

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            getActivity().setTitle("定位");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //SDKInitializer.initialize(getActivity().getApplication());
        View view = inflater.inflate(R.layout.fragment_three, null);
        getActivity().setTitle("定位");
        map = view.findViewById(R.id.map);
        view.findViewById(R.id.bt_lost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), LostHistory.class));
            }
        });
//        reset = view.findViewById(R.id.clear);
        mBaiduMap = this.map.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15));
        XFBluetooth.getInstance(getActivity()).addBleCallBack(gattCallback);
        locationService = ((LocationApplication) getActivity().getApplication()).locationService;
        locationService.registerListener(listener);
     /*   LocationClientOption mOption = locationService.getDefaultLocationClientOption();
        mOption.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        mOption.setCoorType("bd09ll");
        mOption.setScanSpan(10000);
        mOption.setIsNeedAddress(true);

        locationService.setLocationOption(mOption);
        locationService.registerListener(listener);
        Log.e("jerryzhu3", "onCreateView  定位开启: ");
        locationService.start();*/
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        map.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        map.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        locationService.unregisterListener(listener);
        Log.e("jerryzhu3", "onDestroy  定位开启: ");
        locationService.stop();
        map.onDestroy();
    }



}