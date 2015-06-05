package com.jason.house;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;

public class MainActivity extends Activity {

    private MapView sMapView = null;
    private BaiduMap sBdMap = null;
    private LocationClient sLocationClient = null;
    private BDLocationListener sLocationListener = null;

    private int sLocType = -1;
    private double sLongitude = 0.0;
    private double sLatitude = 0.0;
    private float sRadius = 0.0f;
    private String sStrAddr = null;
    private String sStrProvince = null;// 省份信息
    private String sStrCity = null;// 城市信息
    private String sStrDistrict = null;// 区县信息
    private float sDirection = 0.0f;// 手机方向信息


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestFeature() must be called before adding content
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //request no title conflicts with ActionBar
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.main);

        initActionBar();
        initBaiduMap();
        requestLocation();
    }
    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab tab = actionBar.newTab();
        tab.setText("new house");
        tab.setTabListener(new TabListener<NewHouseFragment>(this, "new house", NewHouseFragment.class));
        actionBar.addTab(tab);
        tab = actionBar.newTab();
        tab.setText("second-hand house");
        tab.setTabListener(new TabListener<SecondHandHouseFragment>(this, "new house", SecondHandHouseFragment.class));
        actionBar.addTab(tab);

        actionBar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                Toast.makeText(this, "Menu Item refresh selected",
                        Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestLocation() {
        if (sLocationClient != null && sLocationClient.isStarted()) {
            sLocationClient.requestLocation();
        }
        else {
            Log.d("jason", "location Client is null or not started");
        }
    }

    private void initBaiduMap() {
        sMapView = (MapView)findViewById(R.id.bmapview);
        sMapView.showScaleControl(false);
        sMapView.showZoomControls(false);
        sBdMap = sMapView.getMap();
        sBdMap.setMyLocationEnabled(true);
        sLocationClient = new LocationClient(getApplicationContext());
        sLocationListener = new MyLocationListener();
        sLocationClient.registerLocationListener(sLocationListener);

        LocationClientOption locOption = new LocationClientOption();
        locOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        locOption.setCoorType("bd09ll");// 设置定位结果类型
        //< 1000ms, once mode
        //>= 1000ms, auto mode
        locOption.setScanSpan(500);// 设置发起定位请求的间隔时间,ms
        locOption.setIsNeedAddress(true);// 返回的定位结果包含地址信息
        locOption.setNeedDeviceDirect(true);// 设置返回结果包含手机的方向
        sLocationClient.setLocOption(locOption);
        sLocationClient.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        sMapView.onPause();
    }
    @Override
    protected void onDestroy() {
        sMapView.onDestroy();
        sMapView = null;
        sLocationClient.stop();
        sLocationClient.unRegisterLocationListener(sLocationListener);
        super.onDestroy();
    }

    class MyLocationListener implements BDLocationListener {
        // 异步返回的定位结果
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }
            sLocType = location.getLocType();
            Toast.makeText(MainActivity.this, "当前定位的返回值是：" + sLocType, Toast.LENGTH_SHORT).show();
            sLongitude = location.getLongitude();
            sLatitude = location.getLatitude();
            if (location.hasRadius()) {// 判断是否有定位精度半径
                sRadius = location.getRadius();
            }
            if (sLocType == BDLocation.TypeGpsLocation) {//
                Toast.makeText(
                        MainActivity.this,
                        "当前速度是：" + location.getSpeed() + "~~定位使用卫星数量："
                                + location.getSatelliteNumber(),
                        Toast.LENGTH_SHORT).show();
            } else if (sLocType == BDLocation.TypeNetWorkLocation) {
                sStrAddr = location.getAddrStr();// 获取反地理编码(文字描述的地址)
                Toast.makeText(MainActivity.this, sStrAddr,
                        Toast.LENGTH_SHORT).show();
            }
            sDirection = location.getDirection();// 获取手机方向，【0~360°】,手机上面正面朝北为0°
            sStrProvince = location.getProvince();// 省份
            sStrCity = location.getCity();// 城市
            sStrDistrict = location.getDistrict();// 区县
            Toast.makeText(MainActivity.this,
                    sStrProvince + "~" + sStrCity + "~" + sStrDistrict, Toast.LENGTH_SHORT)
                    .show();
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(sRadius)//
                    .direction(sDirection)// 方向
                    .latitude(sLatitude)//
                    .longitude(sLongitude)//
                    .build();
            // 设置定位数据
            sBdMap.setMyLocationData(locData);
            LatLng ll = new LatLng(sLatitude, sLongitude);
            MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(ll);
            sBdMap.animateMapStatus(msu);
        }
    }

    public class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
