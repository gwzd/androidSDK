package com.hkdemo.gwzd.ltprotectapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AlarmPic extends AppCompatActivity {

    private ShowAlarmPicTask showAlarmPicTask = null;      //获取报警图片
    private Bitmap AlarmMap = null;
    private int AlarmID;
    private View mProgressView = null;                     //进度条控件
    private ImageView ShowImageView = null;
    private String TAG = "获取报警图片";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_pic);

        Intent intent=getIntent();
        AlarmID = Integer.parseInt(intent.getStringExtra("alarmID"));
        Log.d(TAG,"获取到报警ID："+AlarmID);
        mProgressView = findViewById(R.id.AlarmPic_progress);
        ShowImageView = findViewById(R.id.imageView1);
        getPic();
    }
    /**
     * @author zxl
     * @func 获取图片
     */
    public void getPic(){
        showProgress(true);
        showAlarmPicTask = new ShowAlarmPicTask(AlarmID);
        showAlarmPicTask.execute((Void) null);
    }
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

            ShowImageView.setVisibility(show ? View.GONE : View.VISIBLE);
            ShowImageView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ShowImageView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            ShowImageView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    //创建一个线程来获取报警图片
    public class ShowAlarmPicTask extends AsyncTask<Void, Void, Boolean> {

        private final int AlarmID;

        ShowAlarmPicTask(int alarmID) {
            AlarmID = alarmID;
        }
        //通过webservice检查信息
        private String GetAlarmPic(int i_alarmID){
            String WSDL_URI = "http://112.54.80.211:5042/LTProtectService/ServletPort?wsdl";
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
            showProgress(false);

            if (success) {
                //ui操作将图片显示到窗口上
                Log.d("报警图片","获取报警图片成功，显示报警图片!");
                ShowImageView.setImageBitmap(AlarmMap);
            } else {
                //获取报警图片失败
                Log.d("报警图片","获取报警图片失败!");
            }
        }

        @Override
        protected void onCancelled() {
            showAlarmPicTask = null;
            showProgress(false);
        }
    }
}
