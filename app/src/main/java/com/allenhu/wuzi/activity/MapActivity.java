package com.allenhu.wuzi.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.allenhu.wuzi.R;
import com.allenhu.wuzi.bean.Station;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends BaseActivity implements OnGetGeoCoderResultListener {
    private MapView mapView;
    private LocationClient mLocation;
    public MyLocationListenner myListener = new MyLocationListenner();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    boolean isFirstLoc = true; // 是否首次定位
    private BaiduMap mBaiduMap;
    private List<Station> dataList = null;

    private BitmapDescriptor selectorDescriptor = null;
    private BitmapDescriptor normalDescriptor = null;
    private Marker lastMarker;
    private GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initData();
        initView();
    }

    private void initData() {
        dataList = new ArrayList<>();
        float a = 23.149529f;
        float b = 113.310217f;
        for (int i = 0; i < 10; i++) {
            double latitude = a + i * 0.002;
            double longitude = b + i * 0.002;
            Station station = new Station();
            station.setId(i);
            station.setLatitude(latitude);
            station.setLongitude(longitude);
            station.setName("你好啊，真的好啊" + i);
            station.setType(1);
            dataList.add(station);
        }
    }

    private void initView() {
        mapView = (MapView) findViewById(R.id.mapView);
        mBaiduMap = mapView.getMap();
//        mapView.showScaleControl(false);
        mapView.removeViewAt(1);
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;

        selectorDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.icon_marka);
        normalDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding);

        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(mapStatusUpdate);

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);

        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        mLocation = new LocationClient(getApplicationContext());
        mLocation.registerLocationListener(myListener);

        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");   // 设置坐标类型
        option.setScanSpan(500);       // 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);
        mLocation.setLocOption(option);
        mLocation.start();

        mBaiduMap.setOnMarkerClickListener(new myMapClickListener());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        addViewAtMap();
    }

    private void addViewAtMap() {
//        BitmapDescriptor desc = BitmapDescriptorFactory.fromResource(R.mipmap.icon_gcoding);
        List<Marker> markers = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            LatLng point = new LatLng(dataList.get(i).getLatitude(), dataList.get(i).getLongitude());
            OverlayOptions overlayOptions = new MarkerOptions().position(point).icon(normalDescriptor);
            Marker marker = (Marker) mBaiduMap.addOverlay(overlayOptions);
            Bundle bundle = new Bundle();
            bundle.putSerializable("station", dataList.get(i));
            marker.setExtraInfo(bundle);
            markers.add(marker);
        }

        //文字覆盖物
        LatLng textLng = new LatLng(23.128826, 113.321024);
        OverlayOptions textOverlay = new TextOptions().position(textLng).text("公司").bgColor(0xFFFF00FF).fontColor(0xAAFFFF00).fontSize(20);
        mBaiduMap.addOverlay(textOverlay);
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocation.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        super.onDestroy();
        mapView.onDestroy();
        selectorDescriptor.recycle();
        normalDescriptor.recycle();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MapActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
//        mBaiduMap.clear();
//        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation()).icon(selectorDescriptor));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result.getLocation()));


        Button button = new Button(getApplicationContext());
        button.setBackgroundResource(R.mipmap.popup);
        
        button.setText(result.getAddress());
        button.setTextColor(getResources().getColor(R.color.brown));
        //定义用于显示该InfoWindow的坐标点
//            LatLng pt = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
        LatLng pt = result.getLocation();
        //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
        InfoWindow mInfoWindow = new InfoWindow(button, pt, -47);
        //显示InfoWindow
        mBaiduMap.showInfoWindow(mInfoWindow);

//        Toast.makeText(MapActivity.this, result.getAddress(), Toast.LENGTH_LONG).show();
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                            // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {

        }
    }

    private class myMapClickListener implements BaiduMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (lastMarker != null) {
                lastMarker.setIcon(normalDescriptor);
            }
            marker.setIcon(selectorDescriptor);
            mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(marker.getPosition()));
            lastMarker = marker;
            return true;
        }
    }
}
