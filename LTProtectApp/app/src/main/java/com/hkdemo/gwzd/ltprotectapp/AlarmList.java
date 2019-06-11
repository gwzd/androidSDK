package com.hkdemo.gwzd.ltprotectapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlarmList extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private static final int MENU_PIC_ID = Menu.FIRST + 1;//使用Menu.FIRST常量而不使用其他常量的原因：程序封装的变量 用起来不占内存 程序读的快 不容易出错
    private static final int MENU_VIDEO_ID=Menu.FIRST + 2;
    private final String WSDL_URI = "http://112.54.80.211:82/LTProtectService/ServletPort?wsdl";
    private String TAG = "获取报警视频";

    private String RealName;
    private String LoginUserName;
    private ListView listview;
    private SimpleAdapter simpleAdapter;//列表适配器
    private SimpleAdapter DeviceTreeAdapter;//列表适配器
    private boolean flag;               //滚动条滚动判断
    private List listMap;               //listview中的内容
    private List DeviceTreeList;            //设备树list
    private String AlarmTableInfo;     //报警列表信息
    private AlarmInfoTask alarmInfotask = null;//获取报警信息线程
    private GetCameraListTask GetCamListTask = null;

    private View mProgressView = null;                     //进度条控件
    private int i_date = 0;                                 //下拉到底，刷新更早的报警，该值记住到之前的多少天，每次加1
    private TextToSpeech tts = null;                        //文字转语音
    private Handler handler = null;
    private ListView DeviceTree = null;                     //设备树
    private ActionBarDrawerToggle mDrawerToggle = null;     //抽屉控件得适配器
    private DrawerLayout mDrawerLayout = null;              //抽屉布局
    private Toolbar toolbar = null;

    private long exitTime = 0; //实现两次返回 退出
    //private ProgressDialog progressDialog = null;//进度条
    //private String[] presidents={"北京","深圳","济南","广州","海南","香港","澳门"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_list);

        Intent intent=getIntent();
        RealName = intent.getStringExtra("realName");
        LoginUserName = intent.getStringExtra("Username");
        //Log.d("页面跳转",RealName);
        //setTitle("操作员:"+RealName);
        //初始化tts
        tts = new TextToSpeech(this,this);
        //初始化定时器
        handler = new Handler();
        /******************************************************************************************************************************************/
        //初始化设备树信息
        DeviceTreeList = new ArrayList();
        DeviceTree = findViewById(R.id.DeviceTreeListview);
        DeviceTreeAdapter = new SimpleAdapter(AlarmList.this,DeviceTreeList, R.layout.devicetreeadapter,
                new String[]{"Image","CameraName","CameraID"},new int[]{R.id.DevTreeimage,R.id.DevTreetitle,R.id.DevTreetitle_ID});
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.devicetreeadapter,str);
        DeviceTree.setAdapter(DeviceTreeAdapter);
        //DeviceTree.setOnItemClickListener(AppCompatActivity.this);
        //抽屉控件绑定
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("操作员:"+RealName);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,toolbar,R.string.drawer_open,R.string.drawer_close);
        mDrawerToggle.syncState();
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        DeviceTree.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Log.i("刷新报警信息", "ClickPosition: ------" + position);
                if(position >= 0) {
                    Map map = (Map) DeviceTreeList.get(position);
                    //Log.i("刷新报警信息", "onItemClick: ------" + map.get("id"));
                    //int i_AlarmID = Integer.parseInt(map.get("id").toString());
                    //String strIP = map.get("InIPV4").toString();
                    //2019.5.8 张现利修改，外网访问摄像机
                    String strIP = map.get("OutIPV4").toString();
                    int i_port = Integer.parseInt(map.get("outPort").toString());
                    String strUser = map.get("LoginName").toString();
                    String strPsd = map.get("PassWord").toString();
                    Intent intent = new Intent();
                    intent.putExtra("InIPV4", strIP);
                    intent.putExtra("InPort", i_port);
                    intent.putExtra("LoginName", strUser);
                    intent.putExtra("PassWord", strPsd);
                    //2019.3.14 张现利修改，由于可以在视频播放界面查看报警图片因此原来设定的单击报警条目查看报警图片的页面取消。直接进入视频界面
                    //intent.setClass(AlarmList.this,AlarmPic.class);
                    intent.setClass(AlarmList.this,AlarmVideoShow.class);
                    startActivity(intent);
                }
            }
        });
        /******************************************************************************************************************************************/
        //初始化报警列表
        listview = findViewById(R.id.alarmListview);
        //添加一个TextView作为表头
        TextView tvHeader=new TextView(AlarmList.this);
        tvHeader.setText("报警列表");
        listview.addHeaderView(tvHeader);
        /*
        //添加一个TextView作为表尾
        TextView tvFooter=new TextView(AlarmList.this);
        tvFooter.setText("报警列表尾");
        listview.addFooterView(tvFooter);
        */
        //listview.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_2,presidents));
        //捕获listview的滚动条位置，来实现上划刷新最新，下拉加载更多历史
        listview.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    // 当不滚动时
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        // 判断滚动到底部
                        if (listview.getLastVisiblePosition() == (listview.getCount() - 1)) {
                            //Log.d("调试打印","滚动到了最底部");
                            RefrushAlarm(i_date);
                        }
                        // 判断滚动到顶部
                        else if(listview.getFirstVisiblePosition() == 0){
                            //Log.d("调试打印","滚动到了最顶部");
                            i_date ++;
                            RefrushAlarm(i_date);
                        }
                        else{
                            MyPlaySount("系统错误，无法刷新数据!");
                        }
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                if (firstVisibleItem + visibleItemCount == totalItemCount && !flag) {
                    flag = true;
                } else
                    flag = false;
            }
        });


        /**2019.1.3张现利修改，点击查看报警图片，长按查看报警点的实时视频
         * 为报警列表绑定事件--点击item时触发
         */
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Log.i("刷新报警信息", "ClickPosition: ------" + position);
                if(position > 0) {
                    Map map = (Map) listMap.get(position - 1);
                    //Log.i("刷新报警信息", "onItemClick: ------" + map.get("id"));
                    //int i_AlarmID = Integer.parseInt(map.get("id").toString());
                    int str_AlarmID = Integer.parseInt(map.get("id").toString());
                    Log.i(TAG,"传入的报警ID:" + str_AlarmID);
                    Intent intent = new Intent();
                    intent.putExtra("alarmID", str_AlarmID);
                    //2019.3.14 张现利修改，由于可以在视频播放界面查看报警图片因此原来设定的单击报警条目查看报警图片的页面取消。直接进入视频界面
                    //intent.setClass(AlarmList.this,AlarmPic.class);
                    intent.setClass(AlarmList.this,AlarmVideoShow.class);
                    startActivity(intent);
                }
            }
        });
        //为listview添加长按弹出菜单事件
        listMap = new ArrayList();
        simpleAdapter = new SimpleAdapter(AlarmList.this,listMap, R.layout.alarmadapter,
                new String[]{"addr","Alarm","id"},new int[]{R.id.AlarmLineTower,R.id.AlarmInfo,R.id.AlarmID});
        listview.setAdapter(simpleAdapter);
        //进度条
        mProgressView = findViewById(R.id.AlarmpbNormal);
        RefrushAlarm(i_date);
        handler.postDelayed(runnable, 15000); // 15S一次刷新
        //获取设备列表
        GetCameraList();
    }

    /**
     * @author zxl
     * @time 2019.3.19
     * @func 获取该管理员名下的摄像机列表
     */
    private void GetCameraList(){
        GetCamListTask = new GetCameraListTask(LoginUserName);
        GetCamListTask.execute((Void)null);
    }
    /**
     * @author zxl
     * @func 定时器
     */
    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            RefrushAlarm(i_date);
            handler.postDelayed(runnable, 300000); // 5分钟一次刷新
        }
    };
    /**
     * @func 显示进度条
     * @param show
     */
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            listview.setVisibility(show ? View.GONE : View.VISIBLE);
            listview.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    listview.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            listview.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    /**
     * @func 获取报警信息
     */
    public void RefrushAlarm(int i_date){
        Log.d("刷新报警信息","获取：" + i_date + "天之内的信息");
        //MyPlaySount("语音播放测试");
        //mProgressView.setVisibility(View.VISIBLE);
        showProgress(true);
        LocalDateTime time=LocalDateTime.now().minusDays(1);
        LocalDateTime time2= time.minusDays(1 + i_date);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        alarmInfotask = new AlarmInfoTask(LoginUserName,df.format(time2),df.format(time));
        alarmInfotask.execute((Void) null);
    }

    @Override
    public void onInit(int status) {
        //判断tts回调是否成功
        if (status == TextToSpeech.SUCCESS) {
            int result1 = tts.setLanguage(Locale.US);
            int result2 = tts.setLanguage(Locale.CHINESE);
            if (result1 == TextToSpeech.LANG_MISSING_DATA || result1 == TextToSpeech.LANG_NOT_SUPPORTED
                    || result2 == TextToSpeech.LANG_MISSING_DATA || result2 == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @author zxl
     * @param str_TextSound         需要播放的语音内容
     * @func                          报警信息播放语音
     */
    public void MyPlaySount(String str_TextSound){
        if(!TextUtils.isEmpty(str_TextSound)) {
            // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
            tts.setPitch(1.0f);
            // 设置语速
            tts.setSpeechRate(1.0f);
            //播放语音
            tts.speak(str_TextSound, TextToSpeech.QUEUE_ADD, null,null);
        }
    }

    //创建一个线程，获取该管理员名下的摄像机列表
    public class GetCameraListTask extends AsyncTask<Void, Void, Boolean> {
        private final String UserName;
        private String StrDeviceTreeList;

        GetCameraListTask(String str_username){
            UserName = str_username;
        }
        private String GetCameraList(){
            String NAME_SPACE = "http://zxl.ltprotect.com/";
            String methodName = "GetCameraList";
            String str_Ret = "";
            //（1）创建HttpTransportSE对象，该对象用于调用WebService操作
            HttpTransportSE httpTransportSE = new HttpTransportSE(WSDL_URI,90*1000);
            //（2）创建SoapSerializationEnvelope对象
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            //（3）创建SoapObject对象，创建该对象时需要传入所要调用的Web Service的命名空间和WebService方法名
            SoapObject request = new SoapObject(NAME_SPACE, methodName);
            //(4) //填入需要传入的参数
            request.addProperty("arg0",UserName);
            //（5）调用SoapSerializationEnvelope的setOutputSoapObject()方法，或者直接对bodyOut属性赋值，
            //将前两步创建的SoapObject对象设为SoapSerializationEnvelope的传出SOAP消息体
            envelope.setOutputSoapObject(request);
            envelope.dotNet = false;//告知系统 连接的jax-ws

            SoapObject result = null;
            Object response = null;
            try{
                Log.d(TAG,"开始调用webservice" + WSDL_URI);
                String SOAP_ACTION = NAME_SPACE + methodName;
                httpTransportSE.call(SOAP_ACTION, envelope);//调用
                response = envelope.getResponse();
                if (response != null) {
                    str_Ret = response.toString();
                }
                else{
                    Log.d("打印webservice调用的结果","response is null!");
                }
                return str_Ret;
            }catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return str_Ret;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            StrDeviceTreeList = GetCameraList();
            Log.i(TAG,"获取摄像机列表:" + StrDeviceTreeList);
            if(StrDeviceTreeList.isEmpty() || StrDeviceTreeList.compareTo("anyType{}") == 0){
                return false;
            }
            else {
                //解析json格式的回复到listMap中
                try {
                    DeviceTreeList.clear();
                    JSONArray jsonArr = new JSONArray(StrDeviceTreeList);
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject obj = jsonArr.getJSONObject(i);
                        String str_Name = obj.getString("NodeName");
                        String str_NodeID = obj.getString("CamID");
                        //Log.d(TAG,"摄像机名:" + str_Name);
                        //Log.d(TAG,"摄像机ID:" + str_NodeID);
                        Map map = new HashMap();
                        map.put("Image",R.drawable.video_camera);
                        map.put("CameraName", str_Name);
                        map.put("CameraID", str_NodeID);
                        map.put("Factory", obj.getString("Factory"));
                        map.put("LoginName", obj.getString("LoginName"));
                        map.put("PassWord", obj.getString("PassWord"));
                        map.put("OutIPV4", obj.getString("OutIPV4"));
                        map.put("outIpV6", obj.getString("outIpV6"));
                        map.put("outPort", obj.getString("outPort"));
                        map.put("InIPV4", obj.getString("InIPV4"));
                        map.put("InIPV6", obj.getString("InIPV6"));
                        map.put("InPort", obj.getString("InPort"));
                        DeviceTreeList.add(map);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            GetCamListTask = null;
            if (success) {
                //获取成功
                Log.i(TAG,"获取设备列表成功，布置设备树!");
                DeviceTreeAdapter.notifyDataSetChanged();//listmap内容已经改变，如果在线程中调用该方法，界面是不会刷新的，只能在ui线程中调用
            } else {
                //获取报警信息失败
                Log.e(TAG,"获取摄像机列表失败!");
            }
        }
        @Override
        protected void onCancelled() {
            GetCamListTask = null;
        }
    };
    //创建一个线程来获取报警信息
    public class AlarmInfoTask extends AsyncTask<Void, Void, Boolean> {

        private final String UserName;
        private final String Kssj;
        private final String Jssj;

        AlarmInfoTask(String userName, String kssj,String jssj) {
            UserName = userName;
            Kssj = kssj;
            Jssj = jssj;
        }
        //通过webservice检查信息
        private String GetAlarmTable(String userName,String kssj,String jssj){
            //String WSDL_URI = "http://112.54.80.211:5042/LTProtectService/ServletPort?wsdl";
            String NAME_SPACE = "http://zxl.ltprotect.com/";
            String methodName = "GetAlarmTable";
            String str_Ret = "";
            //（1）创建HttpTransportSE对象，该对象用于调用WebService操作
            HttpTransportSE httpTransportSE = new HttpTransportSE(WSDL_URI,90*1000);
            //（2）创建SoapSerializationEnvelope对象
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            //（3）创建SoapObject对象，创建该对象时需要传入所要调用的Web Service的命名空间和WebService方法名
            SoapObject request = new SoapObject(NAME_SPACE, methodName);
            //(4) //填入需要传入的参数
            request.addProperty("arg0",userName);
            request.addProperty("arg1",kssj);
            request.addProperty("arg2",jssj);
            //（5）调用SoapSerializationEnvelope的setOutputSoapObject()方法，或者直接对bodyOut属性赋值，
            //将前两步创建的SoapObject对象设为SoapSerializationEnvelope的传出SOAP消息体
            envelope.setOutputSoapObject(request);
            envelope.dotNet = false;//告知系统 连接的jax-ws

            SoapObject result = null;
            Object response = null;
            try{
                //Log.d("调试打印","开始调用webservice");
                String SOAP_ACTION = NAME_SPACE + methodName;
                httpTransportSE.call(SOAP_ACTION, envelope);//调用
                response = envelope.getResponse();
                if (response != null) {
                    str_Ret = response.toString();
                }
                else{
                    Log.d("打印webservice调用的结果","response is null!");
                }
                return str_Ret;
            }catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return str_Ret;
        }
        //下面的函数在线程中执行
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }
            // TODO: register the new account here.
            AlarmTableInfo = GetAlarmTable(UserName,Kssj,Jssj);
            Log.d("刷新报警信息",AlarmTableInfo);
            if(AlarmTableInfo.isEmpty() || AlarmTableInfo.compareTo("anyType{}") == 0){
                return false;
            }
            else{
                //解析json格式的回复到listMap中
                try{
                    listMap.clear();
                    JSONArray jsonArr = new JSONArray(AlarmTableInfo);
                    for(int i=0 ; i<jsonArr.length() ; i++){
                        JSONObject obj = jsonArr.getJSONObject(i);
                        int i_id = obj.getInt("ID");
                        String str_Name = obj.getString("Name");
                        String str_AlarmType = obj.getString("AlarmType");
                        String str_Time = obj.getString("Time");
                        Map map=new HashMap();
                        map.put("addr",str_Name);
                        map.put("Alarm","报警类型:"+str_AlarmType+" 最新时间:"+str_Time);
                        map.put("id",i_id);
                        listMap.add(map);

                    }

                }catch(Exception e){
                    e.printStackTrace();
                }
                return true;
            }
        }
        //doInBackground 执行完成后将结果发送给onPostExecute
        @Override
        protected void onPostExecute(final Boolean success) {
            //Log.d("调试打印",success?"true":"fasle");
            alarmInfotask = null;
            showProgress(false);
            if (success) {
                //获取成功
                simpleAdapter.notifyDataSetChanged();//listmap内容已经改变，如果在线程中调用该方法，界面是不会刷新的，只能在ui线程中调用
                for(int i=0 ; i<listMap.size() && i<5; i++){
                    HashMap theMap = (HashMap) listMap.get(i);
                    String str_addr = theMap.get("addr").toString();
                    if(str_addr.contains("-")) {
                        str_addr = str_addr.split("-")[0] + "杠" + str_addr.split("-")[1];
                    }
                    String str_AlarmInfo = theMap.get("Alarm").toString().split(" 最新时间")[0].split(":")[1];
                    //语音提示
                    String str_Sound = str_addr + "发生" + str_AlarmInfo + "报警!";
                    MyPlaySount(str_Sound);
                }

            } else {
                //获取报警信息失败
            }
        }

        @Override
        protected void onCancelled() {
            alarmInfotask = null;
            //showProgress(false);
            //mProgressView.setVisibility(View.GONE);
            showProgress(false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * @func 实现两次点击退出
     */
    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //mDrawerToggle.syncState();//该方法会自动和actionBar关联, 将开关的图片显示在了action上，如果不设置，也可以有抽屉的效果，不过是默认的图标
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
