package com.example.zhengshang.amaptest;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements PoiSearch.OnPoiSearchListener {
    public static final String TAG = "MainActivity";
    private TextView textView;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;

    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    private PoiSearch.Query query;
    private double longitude;
    private double latitude;
    private String cityCode;
    private ListView mList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> locationDataList = new ArrayList<>();

    private Spinner mSpinner;
    private String deepType;
    private ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.location_text);
        mList = (ListView) findViewById(R.id.list);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mProgressBar = (ProgressBar) findViewById(R.id.load);

        View emptyView = View.inflate(getApplicationContext(), R.layout.empty_view, null);
        ((ViewGroup) mList.getParent()).addView(emptyView);
        mList.setEmptyView(emptyView);


        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.list_item, locationDataList);
        mList.setAdapter(adapter);


        TextView refresh = (TextView) findViewById(R.id.refresh);

        if (refresh != null) {
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doSearch();
                }
            });
        }

        initSpinner();
        initAMap();

        MainActivityPermissionsDispatcher.startLocationWithCheck(this);

    }

    private void setTextWithColor(String text, int color) {
        textView.setText(text);
        textView.setTextColor(color);
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION)
    void showDeniedForLocation() {
        setTextWithColor("只有打开定位权限才能正常使用此应用", Color.RED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void initSpinner() {
        final String[] str = new String[]{"SDK默认的deepType", "汽车服务", "汽车销售", "汽车维修",
                "摩托车服务", "餐饮服务", "购物服务", "生活服务", "体育休闲服务", "医疗保健服务",
                "住宿服务", "风景名胜", "商务住宅", "政府机构及社会团体", "科教文化服务", "交通设施服务",
                "金融保险服务", "公司企业", "道路附属设施", "地名地址信息", "公共设施"};

        mSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, str));
        mSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            deepType = str[5] + "|" + str[7] + "|" + str[12];
                        } else {
                            deepType = (String) mSpinner.getSelectedItem();
                        }
                        doSearch();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void loading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
    }

    private void loadComplete() {
        mProgressBar.setVisibility(View.GONE);
        mList.setVisibility(View.VISIBLE);
    }

    private void initAMap() {
        mLocationClient = new AMapLocationClient(getApplicationContext());

        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();

        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

        //获取一次定位结果：
        //该方法默认为false。
        //mLocationOption.setOnceLocation(true);
        mLocationOption.setInterval(20 * 1000);

        mLocationOption.setLocationCacheEnable(false);

        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);

        //设置定位回调监听
        mLocationClient.setLocationListener(new AMapLocationListener() {

            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //解析定位结果
                        Log.i(TAG, amapLocation.getPoiName());
                        latitude = amapLocation.getLatitude();
                        longitude = amapLocation.getLongitude();
                        setTextWithColor("latitude = " + latitude + "\nlongitude = " + longitude, Color.BLACK);
                        cityCode = amapLocation.getCity();
                        doSearch();
                        mLocationClient.stopLocation();
                    } else if (amapLocation.getErrorCode() == 12) {
                        setTextWithColor(amapLocation.getErrorInfo(), Color.RED);
                        Toast.makeText(getApplicationContext(), "权限不足，请在设置中授予相应权限", Toast.LENGTH_SHORT).show();
                    } else {
                        setTextWithColor(amapLocation.getErrorInfo(), Color.RED);
                        Log.e(TAG, "error code = " + amapLocation.getErrorCode());
                    }
                }
            }
        });

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);

    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void startLocation() {
        //启动定位
        mLocationClient.startLocation();
    }

    private void doSearch() {
        loading();
        query = new PoiSearch.Query("", deepType, cityCode);
        PoiSearch poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(this);
        query.setPageSize(20);// 设置每页最多返回多少条数据
        query.setPageNum(0);//设置查询页码

        poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude,
                longitude), 2000));//设置周边搜索的中心点以及半径
        //keyWord表示搜索字符串，
        //第二个参数表示POI搜索类型，二者选填其一，
        //POI搜索类型共分为以下20种：汽车服务|汽车销售|
        //汽车维修|摩托车服务|餐饮服务|购物服务|生活服务|体育休闲服务|医疗保健服务|
        //住宿服务|风景名胜|商务住宅|政府机构及社会团体|科教文化服务|交通设施服务|
        //金融保险服务|公司企业|道路附属设施|地名地址信息|公共设施
        //cityCode表示POI搜索区域，可以是城市编码也可以是城市名称，也可以传空字符串，空字符串代表全国在全国范围内进行搜索

        poiSearch.searchPOIAsyn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.stopLocation();//停止定位后，本地定位服务并不会被销毁
            mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
        }
    }

    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        locationDataList.clear();
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    PoiResult poiResult = result;
                    ArrayList<PoiItem> poiItems = poiResult.getPois();

                    if (poiItems != null && poiItems.size() > 0) {
                        for (PoiItem p : poiItems) {
                            Log.i(TAG, "getTitle = " + p.getTitle());
                            locationDataList.add(p.getTitle());
                        }

                    }
                } else {
                    Log.e(TAG, "无结果");
                }
            }

        } else if (rCode == 27) {
            Log.e(TAG, "error_network");
            Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT).show();
        } else if (rCode == 32) {
            setTextWithColor("error key", Color.RED);
            Log.e(TAG, "error_key");
        } else {
            setTextWithColor("poi error code = " + rCode, Color.RED);
            Log.e(TAG, "error_other：" + rCode);
        }
        loadComplete();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }
}





