/*
 * Copyright (c) 2005, 2017, EVECOM Technology Co.,Ltd. All rights reserved.
 * EVECOM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.document;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.DownLoadListener.INoticeListener;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.DownLoadListener.OnDownload;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.DownState;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.bean.TaskInfo;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.downlaod.dao.TaskInfoManager;
import com.wzgiceman.rxretrofitlibrary.retrofit_rx.utils.Tools;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 下载服务
 * Created by thinkpad on 2017/4/18.
 */
public class DownloadService extends Service {
    private String tag = "DownloadService";
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_FINISHED = "finished";
    public static final String ACTION_NOTICE_BRIEF = "notice_brief";
    public static final String EXTRA_FILE_NAME = "extra_file_name";
    public static final String EXTRA_DOWNLOAD_URL = "extra_url";
    public static final String EXTRA_DOWNLOAD_PROGRESS = "extra_download_progress";
    private static final String DOWNLOAD_FILE = "Download";
    /**
     * cpu核心数默认值
     */
    private static final int DEFAULT_CPU_CORES = 2;

    private final int MSG_INIT = 0x01;
    private final int MSG_DOWNLOAD_FINISHED = 0x02;
    private final int MSG_NOTICE_BRIEF = 0x03;
    private Handler mHandler;
    /**服务绑定**/
    private Binder mBinder;
    /***线程池**/
    private ExecutorService mThreadPool ;
    /***下载任务列表**/
    private LinkedList<DownloadTask> mListTask;
    /***广播接收者**/
    private BroadcastReceiver mReceiver;


    private TaskInfo info;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(tag, "onCreate");
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    //start 下载任务初始化成功
                    case MSG_INIT:
                        Log.i(tag, "msg.object  = " + msg.obj);
                        info = (TaskInfo) msg.obj;
                        DownloadTask task = new DownloadTask(DownloadService.this,info,mThreadPool,3, mNoticeListener); //建立下载任务
                        mListTask.add(task);
                        task.downlaod();     //下载
                        break;
                    //end 下载任务初始化成功
                    //start 下载完成删除相应的任务
                    case MSG_DOWNLOAD_FINISHED:
                        Map<String, String> map1 = (Map<String, String>) msg.obj;
                        String url = map1.get("url");
                        for(DownloadTask task1 : mListTask) {
                            if(task1.getUrl().equals(url)) {
                                mListTask.remove(task1);
                                break;
                            }
                        }
                        // 更新本地文件信息
                        if (info != null){
                            info.setState(DownState.FINISH);
                            info.setFinished(100);
                            TaskInfoManager.getInstance().update(info);
                        }

                        /*if (mNoticeListener != null){
                            String fileName = map1.get(EXTRA_FILE_NAME);
                            String progress = map1.get(EXTRA_DOWNLOAD_PROGRESS);
                            mNoticeListener.noticeBrief(fileName, progress);
                        }*/

                        break;
                    //end 下载完成删除相应的任务
                }
            }
        };
        mThreadPool = Executors.newFixedThreadPool(getNumberOfCPUCores());  //初始化线程
        mListTask = new LinkedList<DownloadTask>();                         //初始化任务列表
        registerReceiver();                                                   //注册广播

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(tag, "onBind");
        if(null == mBinder){
            mBinder = new DownloadBinder();
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(tag, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(tag, "onDestroy");
        super.onDestroy();
        //start 停止所有在执行的任务
        for(DownloadTask task1 : mListTask) {
            task1.pause();
        }
        mThreadPool.shutdown(); //关闭线程池
        //end 停止所有在执行的任务
        unregisterReceiver(mReceiver);  //注销广播

    }

    @Override
    public void onLowMemory() {
        //start 停止所有在执行的任务
        for(DownloadTask task1 : mListTask) {
            task1.pause();
        }
        //end 停止所有在执行的任务
        super.onLowMemory();
    }

    /**
     * 更新通知栏的接口
     */
    private INoticeListener mNoticeListener;
    /**
     * 绑定服务类
     */
    public class DownloadBinder extends Binder{
        public  DownloadService getService (INoticeListener noticeListener) {
            mNoticeListener = noticeListener;
            return DownloadService.this;
        }
    }

    /**
     * 注册广播
     */
    private void registerReceiver() {
        mReceiver = new DownloadFinishedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    /**
     * 下载
     * @param url
     * @param download
     */
    public void download(String url,OnDownload download) {
//        TaskInfo info = new TaskInfo(url,null,null,0,download);
        download(url, null, null, download);

    }

    public void download(String url,String filePath,OnDownload download) {
        TaskInfo info = new TaskInfo(Tools.getId(),url,filePath,null,0,0,0);
        info.setDownload(download);
        download(url, filePath, null, download);
    }
    public void download(String url,String filePath,String fileName,OnDownload download) {
        TaskInfo info = new TaskInfo(Tools.getId(),url,filePath,fileName,0,0,0);
        info.setDownload(download);
        download(info);

    }

    /**
     * 下载
     * 首先查找任务列表中对应的任务，如果没有找到则初始化新的下载任务
     * @param info 下载任务信息
     */
    public void download(TaskInfo info) {
        //start 查找任务列表中对应的任务
        for(DownloadTask task1 : mListTask) {
            if(task1.getUrl().equals(info.getUrl())) {
                if(task1.isPaused()) {
                    task1.restart();   //任务已经停止，重新开始
                }
                return;
            }
        }
        //end 查找任务列表中对应的任务

        //初始化新的下载任务
        InitThread thread = new InitThread(info);
        if(!mThreadPool.isShutdown()) {
            mThreadPool.execute(thread);
        }

    }


    /**
     * 停止任务
     * @param url 下载链接
     */
    public void pause(String url) {
        //start 查找下载任务，停止响应的下载任务
        for(DownloadTask task1 : mListTask) {
            if(task1.getUrl().equals(url)) {
                task1.pause();
                return;
            }
        }
        //end 查找下载任务，停止响应的下载任务
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    class InitThread extends Thread {
        //        FileInfo fileInfo;
        TaskInfo taskInfo;
        public InitThread (TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
        }
        @Override
        public void run() {
            super.run();
            Log.i(tag,"InitThread");
            try {
                URL url = new URL(taskInfo.getUrl());
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(5000);
                // 解决len=1的问题
                con.setRequestProperty("Accept-Encoding","identity");
                if(con.getResponseCode() ==  HttpURLConnection.HTTP_OK) {
                    int len = con.getContentLength(); //文件的总长度
                    taskInfo.setLenght(len);
                    if(len <= 0) {
                        return;
                    }
                    String downloadUrl = taskInfo.getUrl();
                    //start 设置存放文件的路径
                    String filePath = taskInfo.getFilePath();
                    if(TextUtils.isEmpty(filePath)) {  //没有设置路径
                        filePath = getFilePath();      //获取默认的路径
                    }else {
                        File file = new File(filePath);
                        if(!file.exists()) {      //确保存在相应的文件夹
                            file.mkdirs();
                        }
                        filePath = file.getAbsolutePath();
                    }
                    taskInfo.setFilePath(filePath);
                    //end 设置存放文件的路径

                    //start 设置文件名
                    String fileName = taskInfo.getFileName();
                    if(TextUtils.isEmpty(fileName)) {                                       //没有设置文件名
                        fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1); //下载链接后缀名为文件名
                    }
                    fileName.trim();
                    if(TextUtils.isEmpty(fileName)) {
                        fileName = getFileName(con);   //根据http参数获取文件名
                    }
                    taskInfo.setFileName(fileName);
                    //end 设置文件名

                    //start 设置下载文件
                    RandomAccessFile accessFile = new RandomAccessFile(new File(taskInfo.getFilePath(),taskInfo.getFileName()),"rwd");
                    accessFile.setLength(len); //设置文件长度
                    accessFile.close();
                    //end 设置下载文件
                    con.disconnect();

                    // 更新本地信息
                    TaskInfoManager.getInstance().update(taskInfo);
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_INIT,taskInfo));

                    Log.i(tag,"len  = "+len);

                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取下载目录
     * @return
     */
    private  String getFilePath() {
        String path = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
//            path = getExternalCacheDir().getPath();
            path = getExternalFilesDir(null).getAbsolutePath();
            Log.i(tag,"path = "+path);

        } else {
            path = getFilesDir().getAbsolutePath();
        }
        File downloadFile = new File(path,DOWNLOAD_FILE);
        if(!downloadFile.exists()) {
            downloadFile.mkdirs();
        }
        path  = downloadFile.getAbsolutePath();

        return path;

    }

    /**
     * 获取cup核心数
     * @return
     */
    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;
        }
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException e) {
//            cores = DEVICEINFO_UNKNOWN;
            cores = DEFAULT_CPU_CORES;
        } catch (NullPointerException e) {
//            cores = DEVICEINFO_UNKNOWN;
            cores = DEFAULT_CPU_CORES;
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    /**
     * 获取文件名
     * @param conn
     * @return
     */
    private String getFileName(HttpURLConnection conn) {
        String filename = null;

        if(filename==null || "".equals(filename.trim())){//如果获取不到文件名称
            for (int i = 0;; i++) {
                String mine = conn.getHeaderField(i);

                if (mine == null) break;

                if("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
                    if(m.find()) return m.group(1);
                }
            }

            filename = UUID.randomUUID()+ ".tmp";//默认取一个文件名
        }

        return filename;
    }

    /**
     * 接收下载完成的广播
     */
    class DownloadFinishedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(null != action){
                if(action.equals(ACTION_FINISHED)) {
             /*       String name = intent.getStringExtra(EXTRA_FILE_NAME);
                    String path = intent.getStringExtra(EXTRA_FILE_PATH);*/
                    String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
//                    TaskInfo info = new TaskInfo(url,path,name,0,null);
                    Map<String, String> map = new HashMap<>();
                    map.put(EXTRA_FILE_NAME, intent.getStringExtra(EXTRA_FILE_NAME));
                    map.put(EXTRA_DOWNLOAD_PROGRESS, intent.getStringExtra(EXTRA_DOWNLOAD_PROGRESS));
                    map.put("url", url);
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOAD_FINISHED,map));
                    Log.i(tag,"download suc url = "+url);
                }
                /*if(action.equals(ACTION_NOTICE_BRIEF)){// 更新通知栏
                    Map<String, String> map = new HashMap<>();
                    map.put(EXTRA_FILE_NAME, intent.getStringExtra(EXTRA_FILE_NAME));
                    map.put(EXTRA_DOWNLOAD_PROGRESS, intent.getStringExtra(EXTRA_DOWNLOAD_PROGRESS));
                    Message message = mHandler.obtainMessage();
                    message.what = MSG_NOTICE_BRIEF;
                    message.obj = map;
                    mHandler.sendMessage(message);

                }*/
            }
        }
    }


}

