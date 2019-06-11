package com.hkdemo.gwzd.ltprotectapp;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActionBar;
import android.app.FragmentTransaction;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AlarmPicAndVideo extends Activity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ShowAlarmPicTask showAlarmPicTask = null;      //获取报警图片
    private Bitmap AlarmMap = null;
    private int AlarmID;
    private View mProgressView = null;                     //进度条控件
    private ImageView ShowImageView = null;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_pic_and_video);
        //设置横向屏幕
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);                  //容器页面
        mViewPager.setAdapter(mSectionsPagerAdapter);

        //获取页面跳转参数
        Intent intent=getIntent();
        AlarmID = Integer.parseInt(intent.getStringExtra("alarmID"));
        //Log.d("报警图片","OnCreate 结尾!");
        //setTitle("报警图像视频");
        //获取报警照片
        getPic();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_alarm_pic_and_video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * @author zxl
     * @func 获取图片
     */
    public void getPic(){
        //showProgress(true);
        showAlarmPicTask = new ShowAlarmPicTask(AlarmID);
        showAlarmPicTask.execute((Void) null);
    }
    /**
     * A placeholder fragment containing a simple view.
     * Fragment:碎片
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();//创建一个自己（碎片）
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);        //绑定参数，这是第几个页面
            fragment.setArguments(args);
            return fragment;                                         //返回这个碎片
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            int i_TabIndex = getArguments().getInt(ARG_SECTION_NUMBER);
            if(i_TabIndex == 1){
                //图像
                rootView = inflater.inflate(R.layout.fragment_alarm_pic_and_video, container, false);
                TextView textView = (TextView) rootView.findViewById(R.id.section_label);
                Log.d("报警图片","创建图片显示页面!");
            }
            else{
                //视频
                //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
                rootView = inflater.inflate(R.layout.alarm_videoshow_new, container, false);
                //TextView textView = (TextView) rootView.findViewById(R.id.AlarmVideotextView);
                //textView.setText("视频");
                Log.d("报警图片","创建报警视频页面!");
            }
            return rootView;
        }
    }


    /**
     * @func 显示进度条
     * @param show
     */
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        mProgressView = findViewById(R.id.AlarmPic_progress);
        ShowImageView = findViewById(R.id.imageView1);
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
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages. tab 适配器
     * tab页面适配器需要至少需要重载三个函数 getItem   getCount  getPageTitle
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment pFragment = PlaceholderFragment.newInstance(position + 1);
            //Log.d("报警图片","创建tab页");
            return pFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            //Log.d("报警图片","获取tab页数");
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //Log.d("报警图片","获取tab标题");
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
            }
            return null;
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("报警图片","页面退出");
    }
}
