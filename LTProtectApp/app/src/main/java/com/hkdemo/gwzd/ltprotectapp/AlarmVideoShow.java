package com.hkdemo.gwzd.ltprotectapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.INT_PTR;
import com.hikvision.netsdk.NET_DVR_CLIENTINFO;
import com.hikvision.netsdk.NET_DVR_COMPRESSIONCFG_V30;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.PTZCommand;
import com.hikvision.netsdk.PlaybackCallBack;
import com.hikvision.netsdk.RealPlayCallBack;

import org.MediaPlayer.PlayM4.Player;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AlarmVideoShow extends Activity implements SurfaceHolder.Callback {

    private final String WSDL_URI = "http://112.54.80.211:82/LTProtectService/ServletPort?wsdl";
    private ShowAlarmPicTask showAlarmPicTask = null;      //获取报警图片
    private Bitmap AlarmMap = null;
    private Button m_oBtnShowPic = null;                  //显示图片按钮
    private Button m_oBtnShowLittleVideo = null;        //显示小视频
    private Button m_oPreviewBtn = null;                 //播放按钮
    //private Button m_oPlaybackBtn = null;                //回放按钮
    private Button m_oCaptureBtn = null;                 //抓图按钮
    private Button m_oRecordBtn = null;                  //录像按钮
    private Button m_oTalkBtn = null;                    //喊话按钮
    private Button m_oOtherBtn = null;                   //其他按钮
    private SurfaceView m_osurfaceView = null;          //播放窗口
    private Button m_oPTZBtn_Up_Left = null;            //左上
    private Button m_oPTZBtn_Up = null;                  //上
    private Button m_oPTZBtn_Up_Right = null;           //右上
    private Button m_oPTZBtn_Left = null;               //左
    private Button m_oPTZBtn_Right = null;              //右
    private Button m_oPTZBtn_Left_Down = null;         //左下
    private Button m_oPTZBtn_Down = null;               //下
    private Button m_oPTZBtn_Right_Down = null;         //右下
    private Button m_oPTZBtn_JJ = null;         //焦距远
    private Button m_oPTZBtn_JJ_2 = null;       //焦距近
    //private Button m_oPTZBtn_JD = null;         //焦点
    //private Button m_oPTZBtn_GQ = null;         //光圈
    private SeekBar m_SeekBar = null;                    //云台速度
    private boolean InNet = false;               //移动设备与前端设备处于同一内网
    private GetCamInfoByAlarmIDTask CamInfotask = null;//通过报警ID获取摄像机信息
    //private ImageView m_PtzCtrlimage = null;

    private NET_DVR_DEVICEINFO_V30 m_oNetDvrDeviceInfoV30 = null;

    private int m_iLogID = -1; // return by NET_DVR_Login_v30
    private int m_iPlayID = -1; // return by NET_DVR_RealPlay_V30
    private int m_iPlaybackID = -1; // return by NET_DVR_PlayBackByTime

    private int m_iPort = -1; // play port
    private int m_iStartChan = 0; // start channel no
    private int m_iChanNum = 0; // channel number
    private static PlaySurfaceView[] playView = new PlaySurfaceView[4];

    private final String TAG = "获取报警视频";
    private boolean b_ptzbtn_jj_mark = true;        //焦点控制标记

    private boolean m_bTalkOn = false;
    private boolean m_bMultiPlay = false;

    private boolean m_bNeedDecode = true;
    private boolean m_bSaveRealData = false;            //录像标记
    private boolean m_bStopPlayback = false;
    private boolean m_bLightOpen = false;
    private int AlarmID;
    //摄像机的登陆信息
    String strIP = "";
    int nPort = 0;
    String strUser = "";
    String strPsd = "";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //CrashUtil crashUtil = CrashUtil.getInstance();
        //crashUtil.init(this);
        setContentView(R.layout.alarm_videoshow_new);

        if (!initeSdk()) {
            this.finish();
            return;
        }
        Intent intent=getIntent();
        //得到alarmid，然后根据alarmid获取到该点的摄像机信息
        AlarmID = intent.getIntExtra("alarmID",-1);
        strIP = intent.getStringExtra("InIPV4");
        nPort = intent.getIntExtra("InPort",-1);
        strUser = intent.getStringExtra("LoginName");
        strPsd = intent.getStringExtra("PassWord");
        Log.i(TAG,"报警ID：" + AlarmID);
        Log.i(TAG,"报警ID：" + strIP);
        Log.i(TAG,"报警ID：" + nPort);
        if(AlarmID != -1) {
            GetCameraInfoByAlarmID();
        }
        else if(nPort != -1)
        {
            DoLogin();
        }
        else{
            Toast.makeText(getApplicationContext(),"获取摄像机信息失败!",Toast.LENGTH_SHORT);
            finish();
        }
        //布置界面
        if (!initeActivity()) {
            this.finish();
            return;
        }
    }

    /**
     * @fun 根据报警ID获取摄像机的信息
     */
    public void GetCameraInfoByAlarmID(){
        CamInfotask = new GetCamInfoByAlarmIDTask();
        CamInfotask.execute((Void) null);
    }
    //创建一个线程 通过报警ID获取摄像机的信息
    public class GetCamInfoByAlarmIDTask extends AsyncTask<Void, Void, Boolean> {

        GetCamInfoByAlarmIDTask() {

        }
        //通过webservice检查信息
        private String GetCamInfo(){
            String NAME_SPACE = "http://zxl.ltprotect.com/";
            String methodName = "GetCamInfoByAlarmID";
            String str_Ret = "";
            //（1）创建HttpTransportSE对象，该对象用于调用WebService操作
            HttpTransportSE httpTransportSE = new HttpTransportSE(WSDL_URI,90*1000);
            //（2）创建SoapSerializationEnvelope对象
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            //（3）创建SoapObject对象，创建该对象时需要传入所要调用的Web Service的命名空间和WebService方法名
            SoapObject request = new SoapObject(NAME_SPACE, methodName);
            //(4) //填入需要传入的参数
            request.addProperty("arg0",AlarmID);
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
            String str_JsonCamInfo = GetCamInfo();
            Log.d(TAG,str_JsonCamInfo);
            if(str_JsonCamInfo.isEmpty() || str_JsonCamInfo.compareTo("anyType{}") == 0){
                return false;
            }
            else{
                //解析json格式的回复到listMap中
                try{
                    JSONObject camObj = new JSONObject(str_JsonCamInfo);
                    if(InNet){
                        strIP = camObj.getString("InIPV4");
                        nPort = camObj.getInt("InPort");
                        strUser = camObj.getString("LoginName");
                        strPsd = camObj.getString("PassWord");
                    }
                    else{
                        strIP = camObj.getString("OutIPV4");
                        nPort = camObj.getInt("outPort");
                        strUser = camObj.getString("LoginName");
                        strPsd = camObj.getString("PassWord");
                    }
                    Log.i(TAG,"摄像机IP"+ strIP);
                    Log.i(TAG,"摄像机Port"+ nPort);
                    Log.i(TAG,"摄像机User"+ strUser);
                    Log.i(TAG,"摄像机PassWord"+ strPsd);
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
            CamInfotask = null;
            if (success) {
                //获取成功
                //此处在进程执行了，可以操作界面.本次执行摄像机登陆操作
                if(!strIP.isEmpty() && nPort != 0 && !strUser.isEmpty()) {
                    DoLogin();
                }
                else{
                    Toast.makeText(getApplicationContext(), "获取摄像机信息失败!",Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                //获取报警信息失败,退出页面
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            CamInfotask = null;
        }
    }
    //创建一个线程来获取报警图片
    public class ShowAlarmPicTask extends AsyncTask<Void, Void, Boolean> {

        private final int AlarmID;
        //private SurfaceHolder  sfh;

        ShowAlarmPicTask(int alarmID) {
            AlarmID = alarmID;
        }
        //通过webservice检查信息
        private String GetAlarmPic(int i_alarmID){

            String NAME_SPACE = "http://zxl.ltprotect.com/";
            String methodName = "GetAlarmPic";
            String str_Ret = "";
            //（1）创建HttpTransportSE对象，该对象用于调用WebService操作
            HttpTransportSE httpTransportSE = new HttpTransportSE(WSDL_URI,90*1000);
            //（2）创建SoapSerializationEnvelope对象
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            //（3）创建SoapObject对象，创建该对象时需要传入所要调用的Web Service的命名空间和WebService方法名
            SoapObject request = new SoapObject(NAME_SPACE, methodName);
            //(4) //填入需要传入的参数
            request.addProperty("arg0",i_alarmID);
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
                    Log.d("报警图片","response is null!");
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
            String AlarmTableInfo = GetAlarmPic(AlarmID);
            //Log.d("报警图片",AlarmTableInfo);
            if(AlarmTableInfo.isEmpty() || AlarmTableInfo.compareTo("anyType{}") == 0){
                return false;
            }
            else{
                //解析json格式的回复到listMap中
                try{
                    //H264解码,当前仍然在线程中
                    byte[] decodeBytes = Base64.decode(AlarmTableInfo,Base64.DEFAULT);
                    AlarmMap = BitmapFactory.decodeByteArray(decodeBytes,0,decodeBytes.length);
                    //sfh = m_osurfaceView.getHolder();
                    //sfh.addCallback(AlarmVideoShow.this);

                    Log.d(TAG,"显示报警图片!");
                    Rect rect = new Rect(0,0,m_osurfaceView.getWidth(),m_osurfaceView.getHeight());
                    //m_osurfaceView.getDrawingRect(rect);
                    Canvas canvas = m_osurfaceView.getHolder().lockCanvas();//获取画布
                    canvas.drawColor(Color.BLACK);
                    canvas.drawBitmap(AlarmMap, null,rect,null);
                    m_osurfaceView.getHolder().unlockCanvasAndPost(canvas);
                    //m_osurfaceView.getHolder().
                    //m_osurfaceView.setVisibility(View.VISIBLE);

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
            showAlarmPicTask = null;

            if (success) {
                //ui操作将图片显示到窗口上
                Log.d("报警图片","获取报警图片成功，显示报警图片!");
                //ShowImageView.setImageBitmap(AlarmMap);
                //m_osurfaceView
                //恢复客户与界面的交互
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            } else {
                //获取报警图片失败
                Log.d("报警图片","获取报警图片失败!");
                Toast.makeText(getApplicationContext(), "该设备没有上传报警图片!",Toast.LENGTH_SHORT).show();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        }

        @Override
        protected void onCancelled() {
            showAlarmPicTask = null;
            //恢复客户与界面的交互
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    /**
     * @func surfaceView 创建时触发
     * @param holder
     */
    // @Override
    public void surfaceCreated(SurfaceHolder holder) {
        m_osurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Log.i(TAG, "surface创建成功，抓取端口" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (true == surface.isValid()) {
            if (false == Player.getInstance().setVideoWindow(m_iPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    /**
     * @func surfaceView改变时触发
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    // @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        Log.i(TAG,"surfaceChanged函数触发!");
        //m_osurfaceView.setVisibility(View.VISIBLE);
    }

    /**
     * @func surfaceView销毁时触发
     * @param holder
     */
    // @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "播放器设置播放窗口，释放端口!" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        if (true == holder.getSurface().isValid()) {
            if (false == Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
                Log.e(TAG, "播放器释放播放窗口失败!");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("m_iPort", m_iPort);
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        m_iPort = savedInstanceState.getInt("m_iPort");
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState");
    }

    /**
     * @fn initeSdk
     * @author zhuzhenlei
     * @brief SDK init
     * @param
     * @param
     * @return true - success;false - fail
     */
    private boolean initeSdk() {
        // init net sdk
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            Log.e(TAG, "HCNetSDK init is failed!");
            return false;
        }
        else {
            //HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, "/mnt/sdcard/sdklog/",true);
            Log.d(TAG,"海康动态库加载成功!");
            return true;
        }
    }

    // GUI init
    private boolean initeActivity() {
        findViews();
        m_osurfaceView.getHolder().addCallback(this);
        setListeners();
        return true;
    }

    // get controller instance
    private void findViews() {
        m_oPreviewBtn = (Button) findViewById(R.id.btn_Preview_new);
        //m_oPlaybackBtn = (Button) findViewById(R.id.btn_Playback_new);
        m_oCaptureBtn = (Button) findViewById(R.id.btn_Capture_new);
        m_oRecordBtn = (Button) findViewById(R.id.btn_Record_new);
        m_oTalkBtn = (Button) findViewById(R.id.btn_Talk_new);
        m_oOtherBtn = (Button) findViewById(R.id.btn_OTHER_new);
        m_osurfaceView = (SurfaceView) findViewById(R.id.Sur_Player_new);
        //m_PtzCtrlimage = findViewById(R.id.PTZCtrlImage);
        m_oPTZBtn_Up_Left   = (Button) findViewById(R.id.PTZBtn_Left_Up);
        m_oPTZBtn_Up        = (Button) findViewById(R.id.PTZBtn_Up);
        m_oPTZBtn_Up_Right  = (Button) findViewById(R.id.PTZBtn_Right_Up);
        m_oPTZBtn_Left      = (Button) findViewById(R.id.PTZBtn_Left);
        m_oPTZBtn_Right     = (Button) findViewById(R.id.PTZBtn_Right);
        m_oPTZBtn_Left_Down = (Button) findViewById(R.id.PTZBtn_Left_Down);
        m_oPTZBtn_Down       = (Button) findViewById(R.id.PTZBtn_Down);
        m_oPTZBtn_Right_Down = (Button) findViewById(R.id.PTZBtn_Right_Down);
        m_SeekBar = (SeekBar)findViewById(R.id.seekBar_PTZ_Speed);
        m_oPTZBtn_JJ = (Button)findViewById(R.id.PTZBtn_JJ);
        m_oPTZBtn_JJ_2 = (Button)findViewById(R.id.PTZBtn_JJ_2);
        //m_oPTZBtn_JD = (Button)findViewById(R.id.PTZBtn_JD);
        //m_oPTZBtn_GQ = (Button)findViewById(R.id.PTZBtn_GQ);
        m_oBtnShowPic = (Button)findViewById(R.id.btn_ShowImage);
        m_oBtnShowLittleVideo = (Button)findViewById(R.id.btn_ShowLittleVideo);
    }

    // 监听设置
    private void setListeners() {
        m_oPreviewBtn.setOnClickListener(Preview_Listener);
        //m_oPlaybackBtn.setOnClickListener(Playback_Listener);
        m_oCaptureBtn.setOnClickListener(Capture_Listener);
        m_oRecordBtn.setOnClickListener(Record_Listener);
        m_oTalkBtn.setOnClickListener(Talk_Listener);
        m_oOtherBtn.setOnClickListener(PTZ_Light_Ctrl);
        //m_PtzCtrlimage.setOnTouchListener(PTZ_ImageListener);
        m_oPTZBtn_Up_Left.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Up.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Up_Right.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Left.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Right.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Left_Down.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Down.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_Right_Down.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_JJ.setOnTouchListener(PTZ_Listener_New);
        m_oPTZBtn_JJ_2.setOnTouchListener(PTZ_Listener_New);
        //m_oPTZBtn_JD.setOnTouchListener(PTZ_Listener_JJ_JD_GQ_Record);
        //m_oPTZBtn_GQ.setOnTouchListener(PTZ_Listener_JJ_JD_GQ_Record);
        m_oBtnShowPic.setOnClickListener(ShowPic_Listener);
    }

    /**
     * @author zxl
     * @func 八方位云台控制
     */
    private Button.OnTouchListener PTZ_Listener_New = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            try{
                //m_SeekBar
                if (m_iLogID < 0) {
                    Log.e(TAG, "please login on a device first");
                    return false;
                }
                //获取云台控制速度
                int i_thisValue = m_SeekBar.getProgress();
                //Log.i(TAG,"按钮事件:" + v.getId() + "----" + R.id.PTZBtn_Left_Down);
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    //按钮按下
                    switch(v.getId()){
                        case R.id.PTZBtn_Left_Up:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.UP_LEFT, 0,i_thisValue)) {
                                Log.e(TAG,"左上方云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "左上方云台控制成功!");
                                m_oPTZBtn_Up_Left.setBackgroundResource(R.drawable.btn7a);
                            }
                            break;
                        case R.id.PTZBtn_Up:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.TILT_UP, 0,i_thisValue)) {
                                Log.e(TAG,"上方云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "上方云台控制成功!");
                                m_oPTZBtn_Up.setBackgroundResource(R.drawable.btn3a);
                            }
                            break;
                        case R.id.PTZBtn_Right_Up:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.UP_RIGHT, 0,i_thisValue)) {
                                Log.e(TAG,"右上方云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "右上方云台控制成功!");
                                m_oPTZBtn_Up_Right.setBackgroundResource(R.drawable.btn8a);
                            }
                            break;
                        case R.id.PTZBtn_Left:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.PAN_LEFT, 0,i_thisValue)) {
                                Log.e(TAG,"左侧云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "左侧云台控制成功!");
                                m_oPTZBtn_Left.setBackgroundResource(R.drawable.btn2a);
                            }
                            break;
                        case R.id.PTZBtn_Right:
                            Log.i(TAG, "云台右转控制!");
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.PAN_RIGHT, 0,i_thisValue)) {
                                Log.e(TAG,"右侧云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "右侧云台控制成功!");
                                m_oPTZBtn_Right.setBackgroundResource(R.drawable.btn1a);
                            }
                            break;
                        case R.id.PTZBtn_Left_Down:
                            Log.i(TAG, "云台左下控制!");
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.DOWN_LEFT, 0,i_thisValue)) {
                                Log.e(TAG,"左下方云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "左下方云台控制成功!");
                                m_oPTZBtn_Left_Down.setBackgroundResource(R.drawable.btn6a);
                            }
                            break;
                        case R.id.PTZBtn_Down:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.TILT_DOWN, 0,i_thisValue)) {
                                Log.e(TAG,"下方云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "下方云台控制成功!");
                                m_oPTZBtn_Down.setBackgroundResource(R.drawable.btn4a);
                            }
                            break;
                        case R.id.PTZBtn_Right_Down:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.DOWN_RIGHT, 0,i_thisValue)) {
                                Log.e(TAG,"右下方云台控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "右下方云台控制成功!");
                                m_oPTZBtn_Right_Down.setBackgroundResource(R.drawable.btn5a);
                            }
                            break;
                        case R.id.PTZBtn_JJ:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.ZOOM_IN, 0,i_thisValue)) {
                                Log.e(TAG,"焦距变小控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "焦距变小控制成功!");
                                m_oPTZBtn_JJ.setBackgroundResource(R.drawable.btn23a);
                            }
                            break;
                        case R.id.PTZBtn_JJ_2:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.ZOOM_OUT, 0,i_thisValue)) {
                                Log.e(TAG,"焦距变小控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "焦距变小控制成功!");
                                m_oPTZBtn_JJ_2.setBackgroundResource(R.drawable.btn12a);
                            }
                            break;

                        default:
                            ;
                    }
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    //按钮抬起
                    if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID,m_iStartChan, PTZCommand.PAN_RIGHT, 1,0)) {
                        Log.e(TAG,"停止云台控制失败: "+ HCNetSDK.getInstance().NET_DVR_GetLastError());
                    } else {
                        Log.i(TAG, "停止云台控制成功!");
                    }
                    switch(v.getId()){
                        case R.id.PTZBtn_Left_Up:
                            m_oPTZBtn_Up_Left.setBackgroundResource(R.drawable.btn7);
                            break;
                        case R.id.PTZBtn_Up:
                            m_oPTZBtn_Up.setBackgroundResource(R.drawable.btn3);
                            break;
                        case R.id.PTZBtn_Right_Up:
                            m_oPTZBtn_Up_Right.setBackgroundResource(R.drawable.btn8);
                            break;
                        case R.id.PTZBtn_Left:
                            m_oPTZBtn_Left.setBackgroundResource(R.drawable.btn2);
                            break;
                        case R.id.PTZBtn_Right:
                            m_oPTZBtn_Right.setBackgroundResource(R.drawable.btn1);
                            break;
                        case R.id.PTZBtn_Left_Down:
                            m_oPTZBtn_Left_Down.setBackgroundResource(R.drawable.btn6);
                            break;
                        case R.id.PTZBtn_Down:
                            m_oPTZBtn_Down.setBackgroundResource(R.drawable.btn4);
                            break;
                        case R.id.PTZBtn_Right_Down:
                            m_oPTZBtn_Right_Down.setBackgroundResource(R.drawable.btn5);
                            break;
                        case R.id.PTZBtn_JJ:
                            m_oPTZBtn_JJ.setBackgroundResource(R.drawable.btn23);
                            break;
                        case R.id.PTZBtn_JJ_2:
                            m_oPTZBtn_JJ_2.setBackgroundResource(R.drawable.btn12);
                            break;
                        case R.id.btn_OTHER_new:
                            if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.AUX_PWRON1, 1,i_thisValue)) {
                                Log.e(TAG,"灯光控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                            } else {
                                Log.i(TAG, "灯光控制成功!");
                                m_oOtherBtn.setBackgroundResource(R.drawable.btn11);
                            }
                            break;
                        default:
                            ;
                    }
                }
                else{}
                return true;
            }catch(Exception err){
                Log.e(TAG, "error: " + err.toString());
                return false;
            }
        }
    };
    /**
     * @author zxl
     * @time 2019.03.14
     * @func 显示报警图片按钮触发函数
     */
    private Button.OnClickListener ShowPic_Listener = new Button.OnClickListener() {

        public void onClick(View v) {
            if(-1 == m_iPort) {
                //阻止客户与界面的交互
                if(AlarmID != -1) {
                    Toast.makeText(getApplicationContext(), "正在获取报警图片，请稍后......", Toast.LENGTH_SHORT).show();
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    showAlarmPicTask = new ShowAlarmPicTask(AlarmID);
                    showAlarmPicTask.execute((Void) null);
                }
                else{
                    Log.e(TAG,"非报警接口进入，无法查看报警照片!");
                    Toast.makeText(getApplicationContext(),"非报警接口进入，无法查看报警照片!",Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                Log.e(TAG,"正在播放视频，无法获取报警图片!");
                Toast.makeText(getApplicationContext(), "播放视频期间无法申请查看报警图片，请先关闭视频!",Toast.LENGTH_SHORT).show();
            }
        }
    };
    // 对讲按钮触发函数
    private Button.OnClickListener Talk_Listener = new Button.OnClickListener() {

        public void onClick(View v) {
            //Log.d(TAG,"Talk_Listener按钮触发函数被屏蔽");

            try {
                if (m_bTalkOn == false) {
                    if (VoiceTalk.startVoiceTalk(m_iLogID) >= 0) {
                        m_bTalkOn = true;
                        //m_oTalkBtn.setText("挂机");
                        m_oTalkBtn.setBackgroundResource(R.drawable.button_play_talk_close);
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"打开语音通道失败:" +  HCNetSDK.getInstance().NET_DVR_GetLastError(),Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (VoiceTalk.stopVoiceTalk()) {
                        m_bTalkOn = false;
                        //m_oTalkBtn.setText("对讲");
                        m_oTalkBtn.setBackgroundResource(R.drawable.button_play_talk);
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"关闭语音通道失败:" +  HCNetSDK.getInstance().NET_DVR_GetLastError(),Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception err) {
                Log.e(TAG, "error: " + err.toString());
            }
        }
    };
    //开关灯控制
    private Button.OnClickListener PTZ_Light_Ctrl =  new Button.OnClickListener() {
        public void onClick(View v) {
            if (m_iLogID < 0) {
                Log.e(TAG, "please login on a device first");
                return;
            }
            if(!m_bLightOpen){
                //开灯
                //!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.LIGHT_PWRON, 0,0)
                //!HCNetSDK.getInstance().NET_DVR_SetAlarmOut(m_iLogID,1,1)
                if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.LIGHT_PWRON, 0,0)) {
                    Log.e(TAG,"灯光打开控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                } else {
                    Log.i(TAG, "灯光打开控制成功!");
                    m_oOtherBtn.setBackgroundResource(R.drawable.button_play_lightctrl_close);
                }
            }
            else{
                //关灯
                //!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.LIGHT_PWRON, 1,0)
                //!HCNetSDK.getInstance().NET_DVR_SetAlarmOut(m_iLogID,1,0)
                if (!HCNetSDK.getInstance().NET_DVR_PTZControlWithSpeed_Other(m_iLogID, m_iStartChan, PTZCommand.LIGHT_PWRON, 1,0)) {
                    Log.e(TAG,"灯光关闭控制失败，错误码: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                } else {
                    Log.i(TAG, "灯光关闭控制成功!");
                    m_oOtherBtn.setBackgroundResource(R.drawable.button_play_lightctrl);
                }
            }
        }
    };
    // 录像按钮触发函数
    private Button.OnClickListener Record_Listener = new Button.OnClickListener() {
        public void onClick(View v) {
            if (!m_bSaveRealData) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                Date curDate =  new Date(System.currentTimeMillis());
                String str   =   formatter.format(curDate);
                Log.i(TAG,"录像文件名:" + str);
                if (!HCNetSDK.getInstance().NET_DVR_SaveRealData(m_iPlayID, "/sdcard/"+str+".mp4")) {
                    Log.e(TAG,"录像失败! error: " + HCNetSDK.getInstance().NET_DVR_GetLastError());
                    Toast.makeText(getApplicationContext(),"录像失败" + HCNetSDK.getInstance().NET_DVR_GetLastError(),Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Log.i(TAG,"录像成功!");
                    Toast.makeText(getApplicationContext(),"录像申请成功，录像中!",Toast.LENGTH_SHORT).show();
                    m_oRecordBtn.setBackgroundResource(R.drawable.button_play_stoprecord);
                    m_bSaveRealData = true;
                }
            } else {
                if (!HCNetSDK.getInstance().NET_DVR_StopSaveRealData(m_iPlayID)) {
                    Log.e(TAG,"停止录像失败! error: "+ HCNetSDK.getInstance().NET_DVR_GetLastError());
                    Toast.makeText(getApplicationContext(),"停止录像失败:"+ HCNetSDK.getInstance().NET_DVR_GetLastError(),Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG,"停止录像成功!");
                    //m_oRecordBtn.setText("录像");
                    Toast.makeText(getApplicationContext(),"停止录像成功!",Toast.LENGTH_SHORT).show();
                    m_oRecordBtn.setBackgroundResource(R.drawable.button_play_pic_record);
                    m_bSaveRealData = false;
                }
            }
        }
    };
    // capture listener
    private Button.OnClickListener Capture_Listener = new Button.OnClickListener() {
        public void onClick(View v) {
            try {
                if (m_iPort < 0) {
                    Log.e(TAG, "请先开启视频播放!");
                    return;
                }
                Player.MPInteger stWidth = new Player.MPInteger();
                Player.MPInteger stHeight = new Player.MPInteger();
                if (!Player.getInstance().getPictureSize(m_iPort, stWidth,stHeight)) {
                    Log.e(TAG, "获取图片大小失败，错误码:" + Player.getInstance().getLastError(m_iPort));
                    return;
                }
                else{
                    Log.i(TAG,"获取图像大小成功!");
                }
                int nSize = 5 * stWidth.value * stHeight.value;
                byte[] picBuf = new byte[nSize];
                Player.MPInteger stSize = new Player.MPInteger();
                if (!Player.getInstance().getBMP(m_iPort, picBuf, nSize, stSize)) {
                    Log.e(TAG, "获取BMP照片失败，错误码:"+ Player.getInstance().getLastError(m_iPort));
                    return;
                }
                else{
                    Log.i(TAG,"获取bmp图片成功!");
                    Toast.makeText(getApplicationContext(), "抓图成功，图片存放于\"内部存储\"!",Toast.LENGTH_SHORT).show();
                }

                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
                String date = sDateFormat.format(new java.util.Date());
                FileOutputStream file = new FileOutputStream("/mnt/sdcard/" + date + ".bmp");
                file.write(picBuf, 0, stSize.value);
                file.close();
            } catch (Exception err) {
                Log.e(TAG, "抓图异常，错误编码: " + err.toString());
            }
        }
    };
    /**
     * @fun 摄像机登陆的执行函数
     */
    private void DoLogin(){
        try {
            if (m_iLogID < 0) {
                // login on the device
                m_iLogID = loginDevice();
                if (m_iLogID < 0) {
                    Log.e(TAG, "设备登陆失败!");
                    return;
                } else {
                    System.out.println("m_iLogID=" + m_iLogID);
                }
                // get instance of exception callback and set
                ExceptionCallBack oexceptionCbf = getExceptiongCbf();
                if (oexceptionCbf == null) {
                    Log.e(TAG, "异常回调函数创建失败!");
                    return;
                }

                if (!HCNetSDK.getInstance().NET_DVR_SetExceptionCallBack(oexceptionCbf)) {
                    Log.e(TAG, "异常回调函数设置失败!");
                    return;
                }
                Log.i(TAG,"设备登陆成功 ****************************1***************************");
            } else {
                // whether we have logout
                if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID)) {
                    Log.e(TAG, " 设备登陆失败!");
                    Toast.makeText(getApplicationContext(), "设备登陆失败!",Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                m_iLogID = -1;
            }
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
        }
    }
    // 播放实时视频按钮触发函数
    private Button.OnClickListener Preview_Listener = new Button.OnClickListener() {
        public void onClick(View v) {
            try {
                //隐藏输入法键盘
                //((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(AlarmVideoShow.this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                if (m_iLogID < 0) {
                    Log.e(TAG, "请先登陆设备!");
                    Toast.makeText(getApplicationContext(), "摄像机登陆失败，请返回重试!",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (m_bNeedDecode) {
                    //没有下面的两句会出现：如果先查看报警图片，然后再播放视频，这时候视频无法播放。
                    //下面的两句话就是例用surfaceview的双缓冲机制，重新创建surfaceview的holder。
                    m_osurfaceView.setVisibility(View.GONE);
                    m_osurfaceView.setVisibility(View.VISIBLE);

                    if (m_iChanNum > 1)// preview more than a channel
                    {
                        if (!m_bMultiPlay) {
                            startMultiPreview();
                            m_bMultiPlay = true;
                        } else {
                            stopMultiPreview();
                            m_bMultiPlay = false;
                        }
                    } else // preivew a channel
                    {
                        if (m_iPlayID < 0) {
                            //该摄像机没有正在播放
                            startSinglePreview();
                        } else {
                            //该摄像机已经正在播放
                            stopSinglePreview();
                        }
                    }
                } else {

                }
            } catch (Exception err) {
                Log.e(TAG, "error: " + err.toString());
            }
        }
    };

    /**
     * 单通道摄像机开启视频播放
     */
    private void startSinglePreview() {
        if (m_iPlaybackID >= 0) {
            Log.i(TAG, "请先停止回放操作!");
            return;
        }
        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
        if (fRealDataCallBack == null) {
            Log.e(TAG, "实时视频回调函数设置失败!");
            return;
        }
        Log.i(TAG, "m_iStartChan:" + m_iStartChan);
        /*
        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = m_iStartChan;
        previewInfo.dwStreamType = 0; // substream  子码流
        previewInfo.bBlocked = 1;
        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(m_iLogID,previewInfo, fRealDataCallBack);
        */
        NET_DVR_CLIENTINFO previewInfo = new NET_DVR_CLIENTINFO();
        previewInfo.lChannel = m_iStartChan;
        previewInfo.lLinkMode    = 0x80000000;   // 子码流  TCP
        previewInfo.sMultiCastIP = null;        //无多播
        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V30(m_iLogID,previewInfo, fRealDataCallBack,true);
        if (m_iPlayID < 0) {
            Log.e(TAG, "NET_DVR_RealPlay is failed!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            Toast.makeText(getApplicationContext(), "视频播放失败,请重试!",Toast.LENGTH_SHORT).show();
            return;
        }
        else{
            m_oPreviewBtn.setBackgroundResource(R.drawable.button_play_play_stop);
        }
    }

    /**
     * 多通道ipc 启动视频播放
     */
    private void startMultiPreview() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int i = 0;
        for (i = 0; i < 4; i++) {
            if (playView[i] == null) {
                playView[i] = new PlaySurfaceView(this);
                playView[i].setParam(metric.widthPixels);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = playView[i].getCurHeight() - (i / 2)
                        * playView[i].getCurHeight();
                params.leftMargin = (i % 2) * playView[i].getCurWidth();
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                addContentView(playView[i], params);
            }
            playView[i].startPreview(m_iLogID, m_iStartChan + i);
        }
        m_iPlayID = playView[0].m_iPreviewHandle;
    }

    /**
     * 当该摄像机拥有多个播放通道时，调用该函数停止视频播放，操作跟停止单通道IPC播放一样，先调用动态库停止播放
     * 然后调用android系统播放器停止播放视频，并且释放端口
     */
    private void stopMultiPreview() {
        int i = 0;
        for (i = 0; i < 4; i++) {
            playView[i].stopPreview();
        }
        m_iPlayID = -1;
    }

    /**
     * @fn stopSinglePreview 单通道摄像机停止视频播放
     * @author zhuzhenlei
     * @brief stop preview
     * @param
     * @param
     * @return NULL
     */
    private void stopSinglePreview() {
        if (m_iPlayID < 0) {
            Log.e(TAG, "m_iPlayID < 0");
            return;
        }

        // net sdk stop preview
        if (!HCNetSDK.getInstance().NET_DVR_StopRealPlay(m_iPlayID)) {
            Log.e(TAG, "StopRealPlay is failed!Err:"+ HCNetSDK.getInstance().NET_DVR_GetLastError());
            return;
        }
        else{
            m_oPreviewBtn.setBackgroundResource(R.drawable.button_play_play);
        }

        m_iPlayID = -1;
        stopSinglePlayer();
    }

    /**
     * 单通道摄像机停止视频播放
     * 释放android系统控件
     */
    private void stopSinglePlayer() {
        Player.getInstance().stopSound();
        // player stop play
        if (!Player.getInstance().stop(m_iPort)) {
            Log.e(TAG, "播放器停止视频播放失败!");
            return;
        }

        if (!Player.getInstance().closeStream(m_iPort)) {
            Log.e(TAG, "关闭视频流失败!");
            return;
        }
        if (!Player.getInstance().freePort(m_iPort)) {
            Log.e(TAG, "释放播放端口失败!" + m_iPort);
            return;
        }
        m_iPort = -1;
    }

    /**
     * @fn loginNormalDevice   普通IPC设备登陆函数
     * @author zhuzhenlei
     * @brief login on device
     * @param
     * @param
     * @return login ID
     */
    private int loginNormalDevice() {
        // get instance
        m_oNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == m_oNetDvrDeviceInfoV30) {
            Log.e(TAG, "HKNetDvrDeviceInfoV30 new is failed!");
            return -1;
        }

        // call NET_DVR_Login_v30 to login on, port 8000 as default
        Log.d(TAG,strIP + ":"  + nPort + "@" + strUser + "/" + strPsd);
        int iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(strIP, nPort,
                strUser, strPsd, m_oNetDvrDeviceInfoV30);
        if (iLogID < 0) {
            Log.e(TAG, "NET_DVR_Login is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        if (m_oNetDvrDeviceInfoV30.byChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum;
        } else if (m_oNetDvrDeviceInfoV30.byIPChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartDChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum
                    + m_oNetDvrDeviceInfoV30.byHighDChanNum * 256;
        }
        Log.i(TAG, "NET_DVR_Login is Successful!");

        return iLogID;
    }

    public static void Test_XMLAbility(int iUserID) {
        byte[] arrayOutBuf = new byte[64 * 1024];
        INT_PTR intPtr = new INT_PTR();
        String strInput = new String("<AlarmHostAbility version=\"2.0\"></AlarmHostAbility>");
        byte[] arrayInBuf = new byte[8 * 1024];
        arrayInBuf = strInput.getBytes();
        if (!HCNetSDK.getInstance().NET_DVR_GetXMLAbility(iUserID,
                HCNetSDK.DEVICE_ABILITY_INFO, arrayInBuf, strInput.length(),
                arrayOutBuf, 64 * 1024, intPtr)) {
            System.out.println("get DEVICE_ABILITY_INFO faild!" + " err: "
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
        } else {
            System.out.println("get DEVICE_ABILITY_INFO succ!");
        }
    }

    /**
     * @fn loginEzvizDevice
     * @author liuyu6
     * @brief login on ezviz device
     * @param
     * @param
     * @return login ID
     */
    private int loginEzvizDevice() {
        return -1;
    }

    /**
     * @fn loginDevice
     * @author zhangqing
     * @brief login on device
     * @param
     * @param
     * @return login ID
     */
    private int loginDevice() {
        int iLogID = -1;

        iLogID = loginNormalDevice();
        return iLogID;
    }

    /**
     * @fn paramCfg   参数配置 函数
     * @author zhuzhenlei
     * @brief configuration
     * @param iUserID
     *            - login ID [in]   用户登陆ID
     * @param
     * @return NULL
     */
    private void paramCfg(final int iUserID) {
        // whether have logined on
        if (iUserID < 0) {
            Log.e(TAG, "iUserID < 0");
            return;
        }

        NET_DVR_COMPRESSIONCFG_V30 struCompress = new NET_DVR_COMPRESSIONCFG_V30();
        if (!HCNetSDK.getInstance().NET_DVR_GetDVRConfig(iUserID,
                HCNetSDK.NET_DVR_GET_COMPRESSCFG_V30, m_iStartChan,
                struCompress)) {
            Log.e(TAG, "NET_DVR_GET_COMPRESSCFG_V30 failed with error code:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
        } else {
            Log.i(TAG, "NET_DVR_GET_COMPRESSCFG_V30 succ");
        }
        // set substream resolution to cif
        struCompress.struNetPara.byResolution = 1;
        if (!HCNetSDK.getInstance().NET_DVR_SetDVRConfig(iUserID,
                HCNetSDK.NET_DVR_SET_COMPRESSCFG_V30, m_iStartChan,
                struCompress)) {
            Log.e(TAG, "NET_DVR_SET_COMPRESSCFG_V30 failed with error code:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
        } else {
            Log.i(TAG, "NET_DVR_SET_COMPRESSCFG_V30 succ");
        }
    }

    /**
     * @fn getExceptiongCbf  触发异常时的回调函数
     * @author zhuzhenlei
     * @brief process exception
     * @param
     * @param
     * @return exception instance
     */
    private ExceptionCallBack getExceptiongCbf() {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                //打印错误编号
                System.out.println("recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    /**
     * @fn getRealPlayerCbf 实时视频播放时    海康动态库的流媒体播放回调函数，里面可以获取到裸码流以及码流类型等信息
     * @author zhuzhenlei
     * @brief get realplay callback instance
     * @param
     * @param
     * @return callback instance
     */
    private RealPlayCallBack getRealPlayerCbf() {
        RealPlayCallBack cbf = new RealPlayCallBack() {
            public void fRealDataCallBack(int iRealHandle, int iDataType,byte[] pDataBuffer, int iDataSize) {
                // player channel 1
                AlarmVideoShow.this.processRealData(1, iDataType, pDataBuffer,
                        iDataSize, Player.STREAM_REALTIME);
            }
        };
        return cbf;
    }

    /**
     * @fn getPlayerbackPlayerCbf  录像回放    海康动态库的流媒体播放回调函数，里面可以获取到裸码流以及码流类型等信息
     * @author zhuzhenlei
     * @brief get Playback instance
     * @param
     * @param
     * @return callback instance
     */
    private PlaybackCallBack getPlayerbackPlayerCbf() {
        PlaybackCallBack cbf = new PlaybackCallBack() {
            @Override
            public void fPlayDataCallBack(int iPlaybackHandle, int iDataType,
                                          byte[] pDataBuffer, int iDataSize) {
                // player channel 1
                AlarmVideoShow.this.processRealData(1, iDataType, pDataBuffer,
                        iDataSize, Player.STREAM_FILE);
            }
        };
        return cbf;
    }

    /**
     * @fn processRealData 海康动态库的流媒体播放回调函数，里面可以获取到裸码流以及码流类型等信息.
     * 此函数用于将画面显示到SurfaceView中
     * @author zhuzhenlei
     * @brief process real data
     * @param iPlayViewNo
     *            - player channel [in]
     * @param iDataType
     *            - data type [in]
     * @param pDataBuffer
     *            - data buffer [in]
     * @param iDataSize
     *            - data size [in]
     * @param iStreamMode
     *            - stream mode [in]
     * @param
     * @return NULL
     */
    public void processRealData(int iPlayViewNo, int iDataType,byte[] pDataBuffer, int iDataSize, int iStreamMode) {
        //Log.d(TAG,"进入processRealData函数!");
        if (!m_bNeedDecode) {
            Log.i(TAG, "iPlayViewNo:" + iPlayViewNo + ",iDataType:" + iDataType + ",iDataSize:" + iDataSize);
        } else {
            if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
                if (m_iPort >= 0) {
                    return;
                }
                m_iPort = Player.getInstance().getPort();
                if (m_iPort == -1) {
                    Log.e(TAG, "获取播放端口号失败，错误编号: "+ Player.getInstance().getLastError(m_iPort));
                    return;
                }
                Log.i(TAG, "获取播放端口号成功，端口号为: " + m_iPort);
                if (iDataSize > 0) {
                    if (!Player.getInstance().setStreamOpenMode(m_iPort,iStreamMode)) // set stream mode
                    {
                        Log.e(TAG, "设置流媒体播放模式失败!");
                        return;
                    }
                    if (!Player.getInstance().openStream(m_iPort, pDataBuffer,iDataSize, 2 * 1024 * 1024)) // open stream
                    {
                        Log.e(TAG, "打开流媒体失败!");
                        return;
                    }
                    if (!Player.getInstance().play(m_iPort,m_osurfaceView.getHolder())) {
                        Log.e(TAG, "播放器，播放流媒体失败!");
                        return;
                    }
                    if (!Player.getInstance().playSound(m_iPort)) {
                        Log.e(TAG, "播放音频失败，错误编号:"+ Player.getInstance().getLastError(m_iPort));
                        return;
                    }
                }
            } else {
                if (!Player.getInstance().inputData(m_iPort, pDataBuffer,iDataSize))
                {
                    // Log.e(TAG, "inputData failed with: " +
                    // Player.getInstance().getLastError(m_iPort));
                    for (int i = 0; i < 4000 && m_iPlaybackID >= 0
                            && !m_bStopPlayback; i++) {
                        if (Player.getInstance().inputData(m_iPort,
                                pDataBuffer, iDataSize)) {
                            break;

                        }

                        if (i % 100 == 0) {
                            Log.e(TAG, "inputData failed with: "
                                    + Player.getInstance()
                                    .getLastError(m_iPort) + ", i:" + i);
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();

                        }
                    }
                }
            }
        }

    }

    /**
     * @fn Cleanup  退出摄像机登陆并且卸载sdk
     * @author zhuzhenlei
     * @brief cleanup
     * @param
     * @param
     * @return NULL
     */
    public void Cleanup() {
        // release player resource

        HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID);
        Player.getInstance().freePort(m_iPort);
        m_iPort = -1;

        // release net SDK resource
        HCNetSDK.getInstance().NET_DVR_Cleanup();
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG,"视频播放界面退出，释放海康驱动!");
        Cleanup();
        super.onDestroy();

    }
}