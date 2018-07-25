package com.wr.qt.nmediademo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.tencent.bugly.crashreport.CrashReport;
import com.vigorchip.WrVideo.wr2.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;

import static com.vigorchip.WrVideo.wr2.R.id.height_seek;
import static com.vigorchip.WrVideo.wr2.R.id.seekbar_parent;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, View.OnClickListener, MediaPlayer.OnCompletionListener {
    private ListView lv;
    private Myadapter adapter;
    private List<MovieMo> movieList;
    private List<MovieMo> newList = new ArrayList<>();
    private VideoView videoView;
    //ll2,ll3主页面的播放进度条容器,全屏的进度条容器
    private LinearLayout ll2, ll3, botrl;
    private MediaPlayer mediaPlayer;
    private int durationTime, curTime, save;
    private TextView tvCurrent, tvDuration, bot_tvc, bot_tvDu;
    private int nextMovie = 0;
    //    /storage/emulated/0/BaiduNetdisk/The.Conjuring.2.2016.招魂2.720p.Chi_Eng.ZMZ-BD-MP4.mp4
    //    private String path="/storage/emulated/0/Download/1.mp4";
    private String path = "/storage/emulated/0/BaiduNetdisk/The.Conjuring.2.2016.招魂2.720p.Chi_Eng.ZMZ-BD-MP4.mp4";
    private SeekBar seekBar, botseekbar, verSeekbar, SoundseekBar, SoundseekBar3;
    private ContentObserver mVoiceObserver;
    private MyVolumeReceiver mVolumeReceiver;

    private ImageButton fullBtn;
    private ImageButton pre;
    private ImageButton next;
    private ImageButton playStop;
    private ImageButton botnext;
    private ImageButton botpre;
    private ImageButton botmini;
    private ImageButton botplay;
    private ImageButton big;
    private LinearLayout heightseek;
    private boolean Isplay = false, itemFirst = true;
    private long lastClickCur;
    private GestureDetector gestureDetector;
    private AudioManager audioManager;
    private int audio = -1;
    private int maxAudio;
    private LinearLayout lightLinearLayout;
    private Animation animation;
    private MyUsbBroad myUsb;
    private Cursor cursor;
    private LinearLayout topll;
    private TextView topName, emptyName;
    private RelativeLayout emptyView, emptyRl;
    private int i;
    private Handler handler = new Handler();
    private Handler handler3 = new Handler();
    private AlertDialog alertDialog1;
    private Handler handler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {   //覆盖handleMessage方法
            switch (msg.what) {   //根据收到的消息的what类型处理z
                case 1:
                    hideBottom();
                    break;
                case 6:
                    String scanMsg = getResources().getString(R.string.scan_start);
                    String scanSecond = getResources().getString(R.string.scan_second);
                    String scanEnd = getResources().getString(R.string.scan_end);
                    movieList.clear();
                    searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    adapter.notifyDataSetChanged();
                    emptyName.setText(scanMsg + i + scanSecond);
                    if (i > 120) {
                        emptyName.setText(scanEnd);
                        break;
                    }
                    i++;
                    handler2.sendEmptyMessageDelayed(6, 1000);
                    break;
                case 7:
                    movieList.clear();
                    searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    adapter.notifyDataSetChanged();
                    /*EXTERNAL_CONTENT_URI：
                    这个Uri代表要查询的数据库名称加上表的名称，这个Uri一般都直接从MediaStore里取得，
                    需要利用MediaStore.Vodeo.Media. EXTERNAL_CONTENT_URI这个Uri取得媒體信息
                    */
                    /*需要修改已生成的列表，通過notifyDataSetChanged()可以在修改適配器綁定的數組后，
                    直接通知Activity更新*/

                    freshUi();
                    break;
                case 8:
                    new Thread() {
                        @Override
                        public void run() {
                            traverseFolder2(pathString);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }.start();
                    break;
                case 10:
                    alertDialog1.dismiss();
                    movieList.clear();
                    searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    adapter.notifyDataSetChanged();
                    break;
                case 11:
                    if (Isplay) {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            //更新视频的播放进度
                            bot_tvc.setText(FormatHelper.formatDuration((int) mediaPlayer.getCurrentPosition()) + "");
                            botseekbar.setProgress((int) mediaPlayer.getCurrentPosition());
                            upDateTime2();
                        }
                    }
                    break;
            }
        }
    };

    private String pathString;
    private int propro;
    private MyButton myButton;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private static String getProcessName(int pid) {
        //通过getProcessName(int pid)来取得当前的进程名
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInsanceState) {
        super.onCreate(savedInsanceState);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
//      Vitamio.isInitialized(this);
        setContentView(R.layout.activity_main);
        /**
         * 配置bugly
         */
        String packageName = this.getPackageName();
        String processName = getProcessName(Process.myPid());
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        CrashReport.initCrashReport(this, "30f72ee0ba", false, strategy);

        initFindView();
//进来设置上下键不可以用
        next.setEnabled(false);
        pre.setEnabled(false);
        botnext.setEnabled(false);
        botpre.setEnabled(false);
        seekBar.setEnabled(false);
        initEvent();

        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        Log.e("onTouchEvent", "手势完成");
                        audio = -1;
                        handler2.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                lightLinearLayout.setVisibility(View.GONE);
                                verSeekbar.setBackgroundColor(Color.TRANSPARENT);
                            }
                        }, 1000);
                        break;
                }
                return false;
            }
        });
        traverseFolder2(Environment.getExternalStorageDirectory().getAbsolutePath());
        movieList.clear();
        searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        adapter.notifyDataSetChanged();

        for (int i = 0; i < movieList.size(); i++) {
            MediaScannerConnection.scanFile(this, new String[]{movieList.get(i).getPath()}, null, null);
        }
        Intent intent = new Intent("closesound");
        sendBroadcast(intent);

        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        /*CTION_CLOSE_SYSTEM_DIALOGS：
                会发出一个action为Intent.ACTION_CLOSE_SYSTEM_DIALOGS的广播，
                這是关闭系统Dialog的广播，可以通过注册它来监听Home按键消息。*/
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_HOME_KEY_LONG = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);  //通过获取Reason字段 来判断长按 还是单击Home键。
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    //表示按了home键,程序到了后台
                    finish();
                } else if (TextUtils.equals(reason, SYSTEM_HOME_KEY_LONG)) {
                    //表示长按home键,显示最近使用的程序列表
                }
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        //請求音頻焦點
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:  //AUDIOFOCUS_LOSS: 失去了音频焦点很长时间
                    playStop.setImageResource(R.mipmap.pause);
                    botplay.setImageResource(R.mipmap.pause);
                    big.setVisibility(View.GONE);
                    audioManager.abandonAudioFocus(mAudioFocusChange);
                    break;
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        audioManager.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (mediaPlayer != null) {
            videoView.seekTo(save);
            botplay.setImageResource(R.mipmap.play);
            playStop.setImageResource(R.mipmap.play);
            big.setVisibility(View.GONE);
        }
        registerUsb();
        i = 1;
        if (new File("/storage/usbhost1/LOST.DIR").exists()) {
            movieList.clear();
            searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            adapter.notifyDataSetChanged();
            handler2.sendEmptyMessageDelayed(6, 1000);
        }
        alwaysFresh();
    }

    private boolean ALWYASFRESH = true;

    private void alwaysFresh() {
//        Log.i(Thread.currentThread().getName(),"TAG");
        handler3.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ALWYASFRESH) {
                    movieList.clear();
//                    Log.i(Thread.currentThread().getName(),"TAGs");
                    searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    /**
                     * 这个Uri代表要查询的数据库名称加上表的名称。这个Uri一般都直接从MediaStore里取得，
                     * 例如我要取所有歌的信息，就必须利用MediaStore.Audio.Media. EXTERNAL_CONTENT_URI这个Uri。
                     */
                    adapter.notifyDataSetChanged();
                    alwaysFresh();
                }
            }
        }, 2000);
    }

    //加trycatch
    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                if ((nextMovie) < movieList.size()){
                    save = (int) mediaPlayer.getCurrentPosition();//獲取當前的播放時間
                }
            }
            Isplay = false;
            handler2.removeMessages(7);
            unregisterReceiver(myUsb);
            unregisterReceiver(myButton);
        }catch (Exception e){

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (movieList != null) {
            movieList.clear();
        }
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            handler2.removeMessages(6);
        }
        if (mVolumeReceiver!=null) {
            unregisterReceiver(mVolumeReceiver);
        }
        mediaPlayer = null;
    }

    private void initFindView() {
        movieList = new ArrayList<>();
//        movieList = FileUtils.getSpecificTypeOfFile(MainActivity.this, new String[]{".mp4", ".rmvb", ".wmv", ".3gp", ".mkv"});
        videoView = (VideoView) findViewById(R.id.vv);
        lv = (ListView) findViewById(R.id.lv);
        ll2 = (LinearLayout) findViewById(R.id.ll2);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        ll3 = (LinearLayout) findViewById(R.id.ll3);

//主视图按钮。。。
        //音量进度
        SoundseekBar=(SeekBar)findViewById(R.id.seekBar2);
        RelativeLayout soundseek = (RelativeLayout) findViewById(seekbar_parent);
        //音量进度触摸区域的放大
        soundseek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect seekRect = new Rect();
                SoundseekBar.getHitRect(seekRect);

                if ((event.getY() >= (seekRect.top - 500)) && (event.getY() <= (seekRect.bottom + 500))) {
                    float y = seekRect.top + seekRect.height() / 2;
                    //seekBar only accept relative x
                    float x = event.getX() - seekRect.left;
                    if (x < 0) {
                        x = 0;
                    } else if (x > seekRect.width()) {
                        x = seekRect.width();
                    }
                    MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                            event.getAction(), x, y, event.getMetaState());
                    return SoundseekBar.onTouchEvent(me);
                }
                return false;
            }
        });
        SoundseekBar3=(SeekBar)findViewById(R.id.seekBar3);
        RelativeLayout soundseek3 = (RelativeLayout) findViewById(R.id.seekbar_parent3);
        //音量进度触摸区域的放大
        soundseek3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect seekRect = new Rect();
                SoundseekBar3.getHitRect(seekRect);

                if ((event.getY() >= (seekRect.top - 500)) && (event.getY() <= (seekRect.bottom + 500))) {
                    float y = seekRect.top + seekRect.height() / 2;
                    //seekBar only accept relative x
                    float x = event.getX() - seekRect.left;
                    if (x < 0) {
                        x = 0;
                    } else if (x > seekRect.width()) {
                        x = seekRect.width();
                    }
                    MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                            event.getAction(), x, y, event.getMetaState());
                    return SoundseekBar3.onTouchEvent(me);
                }
                return false;
            }
        });
        tvCurrent = (TextView) findViewById(R.id.tvc);
        tvDuration = (TextView) findViewById(R.id.tvd);
        fullBtn = (ImageButton) findViewById(R.id.full);
        playStop = (ImageButton) findViewById(R.id.playStop);
        pre = (ImageButton) findViewById(R.id.previous);
        next = (ImageButton) findViewById(R.id.next);

        adapter = new Myadapter();
//设置空视图
        emptyRl = (RelativeLayout) findViewById(R.id.rlnull);
        emptyView = (RelativeLayout) findViewById(R.id.rlnull2);
        emptyName = (TextView) findViewById(R.id.tvfresh);
        lv.setEmptyView(emptyView);
        lv.setAdapter(adapter);
//全屏
        topll = (LinearLayout) findViewById(R.id.top_ll);
        topName = (TextView) findViewById(R.id.top_text);
        botrl = (LinearLayout) findViewById(R.id.bot_rl);
        big = (ImageButton) findViewById(R.id.bigstart);
        bot_tvc = (TextView) findViewById(R.id.bot_tvc);
        bot_tvDu = (TextView) findViewById(R.id.bot_tvd);
        heightseek = (LinearLayout) findViewById(height_seek);
        botseekbar = (SeekBar) findViewById(R.id.bot_seekbar);
        heightseek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect seekRect = new Rect();
                botseekbar.getHitRect(seekRect);

                if ((event.getY() >= (seekRect.top - 500)) && (event.getY() <= (seekRect.bottom + 500))) {
                    float y = seekRect.top + seekRect.height() / 2;
                    //seekBar only accept relative x
                    float x = event.getX() - seekRect.left;
                    if (x < 0) {
                        x = 0;
                    } else if (x > seekRect.width()) {
                        x = seekRect.width();
                    }
                    MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                            event.getAction(), x, y, event.getMetaState());
                    return botseekbar.onTouchEvent(me);
                }
                return false;
            }
        });
        botnext = (ImageButton) findViewById(R.id.bot_next);
        botpre = (ImageButton) findViewById(R.id.bot_pre);
        botplay = (ImageButton) findViewById(R.id.bot_play);
        botmini = (ImageButton) findViewById(R.id.bot_mini);

        verSeekbar = (SeekBar) findViewById(R.id.verticalSeekbar);
        gestureDetector = new GestureDetector(this, new MyGes());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);//获取音量服务
        maxAudio = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//获取系统音量最大值
        SoundseekBar.setMax(maxAudio);//音量控制Bar的最大值设置为系统音量最大值
        SoundseekBar3.setMax(maxAudio);
//        verSeekbar.setMax(maxAudio);
//        int currentSount=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);//获取当前音量
        SoundseekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));// 当前的媒体音量//音量控制Bar的当前值设置为系统音量当前值
//        SoundseekBar3.setProgress(audio Manager.getStreamVolume(AudioManager.STREAM_MUSIC));// 当前的媒体音量//音量控制Bar的当前值设置为系统音量当前值
//        verSeekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));// 当前的媒体音量//音量控制Bar的当前值设置为系统音量当前值
        lightLinearLayout = (LinearLayout) findViewById(R.id.lightLinearLayout);
        myRegisterReceiver();//注册同步更新的广播

        animation = AnimationUtils.loadAnimation(this, R.anim.big);
    }

    private void myRegisterReceiver() {
        mVolumeReceiver = new MyVolumeReceiver() ;
        IntentFilter filter = new IntentFilter() ;
        filter.addAction("android.media.VOLUME_CHANGED_ACTION") ;
        registerReceiver(mVolumeReceiver, filter) ;
    }

    private class MyVolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //如果音量发生变化则更改seekbar的位置
            if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ;// 当前的媒体音量
                SoundseekBar.setProgress(currVolume) ;
//                SoundseekBar3.setProgress(currVolume);
            }
        }
    }


    private void initEvent() {
        fullBtn.setOnClickListener(this);
        pre.setOnClickListener(this);
        playStop.setOnClickListener(this);
        next.setOnClickListener(this);
        botnext.setOnClickListener(this);
        botplay.setOnClickListener(this);
        big.setOnClickListener(this);
        botpre.setOnClickListener(this);
        botmini.setOnClickListener(this);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                nextMovie = position;
                fullBtn.setEnabled(true);
                seekBar.setEnabled(true);
                playStop.setEnabled(true);
                next.setEnabled(true);
                pre.setEnabled(true);
                botnext.setEnabled(true);
                botpre.setEnabled(true);

                clickItem = false;
                if (itemFirst) {
                    lastClickCur = System.currentTimeMillis();
                    videoView.setVideoPath(movieList.get(nextMovie).getPath());
                    videoView.setBackgroundColor(Color.TRANSPARENT);
                    playStop.setImageResource(R.mipmap.play);
                    botplay.setImageResource(R.mipmap.play);
                    big.setVisibility(View.GONE);
                    itemFirst = false;
                }
                if ((System.currentTimeMillis() - lastClickCur) > 800) {
                    lastClickCur = System.currentTimeMillis();
                    videoView.setBackgroundColor(Color.TRANSPARENT);//透明的
                    videoView.setVideoPath(movieList.get(nextMovie).getPath());
                    playStop.setImageResource(R.mipmap.play);
                    botplay.setImageResource(R.mipmap.play);
                    big.setVisibility(View.GONE);
                }
                adapter.setSelectItem(nextMovie);
                adapter.notifyDataSetInvalidated();
                if (emptyRl.getVisibility() == View.VISIBLE) {
                    emptyRl.setVisibility(View.GONE);
                    ll2.setVisibility(View.GONE);
                    ll3.setVisibility(View.GONE);
                }
            }
        });

        ll3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect seekRect = new Rect();
                seekBar.getHitRect(seekRect);

                if ((event.getY() >= (seekRect.top - 1000)) && (event.getY() <= (seekRect.bottom + 1000))) {
                    float y = seekRect.top + seekRect.height() / 2;
                    //seekBar only accept relative x
                    float x = event.getX() - seekRect.left;
                    if (x < 0) {
                        x = 0;
                    } else if (x > seekRect.width()) {
                        x = seekRect.width();
                    }
                    MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                            event.getAction(), x, y, event.getMetaState());
                    return seekBar.onTouchEvent(me);
                }
                return false;
            }
        });

//音量进度
        SoundseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                Log.v("lyj_ring", "mVoiceSeekBar max progress = "+arg1);
                //系统音量和媒体音量同时更新
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, arg1, 0);
                audioManager.setStreamVolume(3, arg1, 3);//  3 代表  AudioManager.STREAM_MUSIC
                SoundseekBar3.setProgress(arg1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }
        });

//        SoundseekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
//
//            @Override
//            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
//                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                Log.v("lyj_ring", "mVoiceSeekBar max progress = "+arg1);
//                //系统音量和媒体音量同时更新
//                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, arg1, 0);
//                audioManager.setStreamVolume(3, arg1, 3);//  3 代表  AudioManager.STREAM_MUSIC
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar arg0) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar arg0) {
//
//            }
//        });

        mVoiceObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                SoundseekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
//                SoundseekBar3.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
                verSeekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
                //或者你也可以用媒体音量来监听改变，效果都是一样的。
                //mVoiceSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        };





        videoView.setOnPreparedListener(MainActivity.this);
        videoView.setOnCompletionListener(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                videoView.setBackgroundColor(Color.TRANSPARENT);
                if (fromUser) {
                    propro = progress;
                    Log.v("progress", "progress = "+progress);
                    tvCurrent.setText(FormatHelper.formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Isplay = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Isplay = true;
                upDateTime();
                upDateTime2();
                videoView.seekTo(propro);
                playStop.setImageResource(R.mipmap.play);
                botplay.setImageResource(R.mipmap.play);
                big.setVisibility(View.GONE);
                tvCurrent.setText(FormatHelper.formatDuration(propro));
            }
        });

        botseekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                videoView.setBackgroundColor(Color.TRANSPARENT);
                if (fromUser) {
                    propro = progress;
                    bot_tvc.setText(FormatHelper.formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Isplay = false;
                handler2.removeMessages(1);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Isplay = true;
                upDateTime();
                upDateTime2();
                handler2.sendEmptyMessageDelayed(1, 4000);
                videoView.seekTo(propro);
                playStop.setImageResource(R.mipmap.play);
                botplay.setImageResource(R.mipmap.play);
                big.setVisibility(View.GONE);
                tvCurrent.setText(FormatHelper.formatDuration(propro));
            }
        });
    }

    private void registerUsb() {
        myUsb = new MyUsbBroad();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addDataScheme("file");
        registerReceiver(myUsb, intentFilter);

        myButton = new MyButton();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(myButton, filter);
    }

    private void searchMovie(Uri uri) {
        String str[] = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION
        };
        cursor = getContentResolver().query(uri, str, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MovieMo movie = new MovieMo();
                movie.size = cursor.getString(0);
                movie.name = cursor.getString(1);
//                int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
//                int duration = cursor.getInt(durationCol);
//                movie.setDuration(duration);

                movie.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                movie.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

                double fileOrFilesSize = FileSizeUtil.getFileOrFilesSize(movie.path, 3);
                if (fileOrFilesSize > 10) {

                    movieList.add(movie);
                }
            }
            cursor.close();
        }
    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (gestureDetector.onTouchEvent(event)) {
//            return true;
//        }
//        switch (event.getAction()) {
//
//            case MotionEvent.ACTION_UP:
//                Log.e("onTouchEvent", "手势完成");
//                audio = -1;
//                handler2.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        lightLinearLayout.setVisibility(View.GONE);
//                        verSeekbar.setBackgroundColor(Color.TRANSPARENT);
//                    }
//                }, 1000);
//                break;
//
//        }
//        return false;
//    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private class MyGes extends GestureDetector.SimpleOnGestureListener {
        private boolean touchFirst = false;
        private boolean isLand = false;
        private boolean isLight = false;

        @Override
        public boolean onDown(MotionEvent e) {
            touchFirst = true;
            if (!isShow(emptyRl)) {
                Log.e("print", "onDown");
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (!isShow(emptyRl)) {
                Log.e("print", "onSingleTapUp");
                if (isShow(botrl)) {
                    handler2.removeMessages(1);
                    handler2.sendEmptyMessageDelayed(1, 0);
                } else {
                    showBottom(4000);
                }
            }
            return super.onSingleTapUp(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.e("print", "onScroll");
            float bx = e1.getX();
            float by = e1.getY();

            float ex = e2.getX();
            float ey = e2.getY();

            float mx = ex - bx;
            float my = by - ey;

            if (touchFirst) {
                if (Math.abs(distanceX) <= Math.abs(distanceY)) {
                    isLand = false;
                    if (bx < 0) {
                        isLight = true;

                    } else {
                        isLight = false;
                    }
                } else {
                    isLand = true;
                }
                touchFirst = false;
            }
            if (isLand) {
                //控制播放进度；
//                    float p=mx/videoView.getWidth();
//                    if (mx>0){
////                        (long)(p*videoView.getDuration())
//                        videoView.seekTo(videoView.getCurrentPosition()+30000);
//                    }if (mx<0){
//                        if(videoView.getCurrentPosition()<=30000){
//                            videoView.seekTo(0);
//                        }else {
//                            videoView.seekTo(videoView.getCurrentPosition()-30000);
//                        }
//                    }
            } else {
                float p = my / videoView.getHeight();
                if (isLight) {
                    //控制亮度
                    setLight(p);
                } else {
                    //控制声音
//                    if (itemFirst == false) {
//                        setSound(p);
//                    }
                    setSound(p);
                }
            }
//            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }
    }

    /**
     * TODO 设置亮度
     *
     * @param p
     */
    private void setLight(float p) {
//        if (light== -1){
//            light=  getWindow().getAttributes().screenBrightness;
//        }
//        float newLight=p*1+light;
//        if (newLight > 1) {
//            newLight=1;
//        }else if (newLight<0.01){
//            newLight=0.01f;
//        }
//        //设置亮度
//        WindowManager.LayoutParams attributes = getWindow().getAttributes();
//        attributes.screenBrightness=newLight;
//        getWindow().setAttributes(attributes);
//        lightLinearLayout.setVisibility(View.VISIBLE);
//        lightText.setText((int)(newLight*100)+"%");
////        //控制文本 (int)(newlight*100)+"%";
    }

    /**
     * TODO 设置音量
     *
     * @param p
     */
    private void setSound(float p) {
        if (audio == -1) {
            audio = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        int newAudio = (int) (p * maxAudio + audio);
        if (newAudio > maxAudio) {
            newAudio = maxAudio;
        } else if (newAudio < 0) {
            newAudio = 0;
        }

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newAudio, 0);
//显示中间的控件 中间空间设置；
        verSeekbar.setMax(100);
//        SoundseekBar.setMax(100);
//        SoundseekBar3.setMax(100);
        String s = (int) (((float) newAudio / maxAudio) * 100) + "%";
//        verSeekbar.setBackgroundColor(Color.YELLOW);
        verSeekbar.setProgress((int) (((float) newAudio / maxAudio) * 100));
//        SoundseekBar.setProgress((int) (((float) newAudio / maxAudio) * 100));//音量控制Bar的当前值设置为系统音量当前值
//        SoundseekBar3.setProgress((int) (((float) newAudio / maxAudio) * 100));//音量控制Bar的当前值设置为系统音量当前值
        lightLinearLayout.setVisibility(View.VISIBLE);
        Log.e("print", "sound");
//        lightText.setTextColor(Color.GREEN);
//        lightText.setText(s);
    }

    private void showBottom() {
        showBottom(0);
    }

    private void showBottom(long timer) {
        if (botrl.getVisibility() == View.GONE) {
            botrl.setVisibility(View.VISIBLE);
            topll.setVisibility(View.VISIBLE);
            if ((nextMovie) >= movieList.size()) {

            } else {
                topName.setText(movieList.get(nextMovie).getName());
            }

            botrl.setBackgroundColor(Color.argb(0xa0, 0xff, 0xff, 0xff));
            topll.setBackgroundColor(Color.argb(0xa0, 0xff, 0xff, 0xff));
            if (timer > 0) {
                handler2.sendEmptyMessageDelayed(1, timer);
            }
        }
    }

    private void hideBottom() {
        botrl.setVisibility(View.GONE);
        topll.setVisibility(View.GONE);

    }

    private boolean isShow(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e("print", "onPrepared-------");
        Isplay = true;
        videoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
//        videoView.setHardwareDecoder(true);

        mediaPlayer = mp;
        curTime = (int) mp.getCurrentPosition();
        durationTime = (int) mp.getDuration();
        tvCurrent.setText(FormatHelper.formatDuration(curTime) + "");//设置当前进度
        tvDuration.setText(FormatHelper.formatDuration(durationTime) + "");//设置总的时长
        seekBar.setMax(durationTime);
        bot_tvc.setText(FormatHelper.formatDuration(curTime) + "");//设置当前进度
        bot_tvDu.setText(FormatHelper.formatDuration(durationTime) + "");//设置总的时长
        botseekbar.setMax(durationTime);

        upDateTime2();
        upDateTime();
    }

    //1加trycatch
    //更新进度条2
    private void upDateTime2() {
        try{
            handler3.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handler3.sendEmptyMessage(11);
                }
            }, 500);
        }catch (Exception e){

        }
    }

    //更新进度条1
    private void upDateTime() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Isplay) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        //更新视频的播放进度
                        tvCurrent.setText(FormatHelper.formatDuration((int) mediaPlayer.getCurrentPosition()) + "");
                        seekBar.setProgress((int) mediaPlayer.getCurrentPosition());
                        upDateTime();
                    }
                }
            }
        }, 500);
    }

    private boolean clickPaly = true, clickItem = true;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.full:
//                int i1=10/0;
                if (mediaPlayer != null) {
                    if (emptyRl.getVisibility() == View.VISIBLE) {
                        emptyRl.setVisibility(View.GONE);
                        ll2.setVisibility(View.GONE);
                        ll3.setVisibility(View.GONE);
                    }
                }
                break;
            case R.id.playStop:
                if (mediaPlayer != null) {
                    playStop.startAnimation(animation);
                }
                if (clickItem) {
                    if (movieList.size() == 0) {

                    } else if (clickPaly) {
                        seekBar.setEnabled(true);
                        videoView.setVideoPath(movieList.get(0).getPath());
                        videoView.setBackgroundColor(Color.TRANSPARENT);
                        playStop.setImageResource(R.mipmap.play);
                        botplay.setImageResource(R.mipmap.play);
                        clickPaly = false;
                        adapter.setSelectItem(0);
                        next.setEnabled(true);
                        pre.setEnabled(true);
                        botnext.setEnabled(true);
                        botpre.setEnabled(true);
                    }
                }
                playAndStop();
                break;
            case R.id.next:
                if (mediaPlayer != null) {
                    next.startAnimation(animation);
                }
                seekBar.setEnabled(true);
                fullBtn.setEnabled(true);
                playStop.setEnabled(true);
                nextPlay();
                break;
            case R.id.previous:
                if (mediaPlayer != null) {
                    pre.startAnimation(animation);
                }
                seekBar.setEnabled(true);
                fullBtn.setEnabled(true);
                playStop.setEnabled(true);
                prePlay();
                break;
            case R.id.bot_mini:
                emptyRl.setVisibility(View.VISIBLE);
                ll2.setVisibility(View.VISIBLE);
                ll3.setVisibility(View.VISIBLE);
                botrl.setVisibility(View.GONE);
                topll.setVisibility(View.GONE);
                break;
            case R.id.bot_next:
                botnext.startAnimation(animation);
                handler2.removeMessages(1);
                handler2.sendEmptyMessageDelayed(1, 4000);
                nextPlay();
                break;
            case R.id.bot_pre:
                botpre.startAnimation(animation);
                handler2.removeMessages(1);
                handler2.sendEmptyMessageDelayed(1, 4000);
                prePlay();
                break;
            case R.id.bot_play:
                botplay.startAnimation(animation);
                handler2.removeMessages(1);
                handler2.sendEmptyMessageDelayed(1, 4000);
                playAndStop();
                break;
            case R.id.bigstart:
                big.startAnimation(animation);
                handler2.removeMessages(1);
                handler2.sendEmptyMessageDelayed(1, 4000);
                playAndStop();
                break;
        }
    }

    private void prePlay() {
        adapter.notifyDataSetInvalidated();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            playStop.setImageResource(R.mipmap.play);
            botplay.setImageResource(R.mipmap.play);
            big.setVisibility(View.GONE);
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (movieList.size() == 0) {
                videoView.setVideoPath(movieList.get(0).getPath());
            }
            if (movieList.size() > 0) {
                nextMovie--;
                if (nextMovie >= 0) {
                    Log.e("pre", "1" + nextMovie);
                    videoView.setVideoPath(movieList.get(nextMovie).getPath());
                    adapter.setSelectItem(nextMovie);
                    playStop.setImageResource(R.mipmap.play);
                    botplay.setImageResource(R.mipmap.play);
                    big.setVisibility(View.GONE);
                }
                if (nextMovie < 0) {
                    Log.e("pre", "2" + nextMovie);
                    videoView.setVideoPath(movieList.get(movieList.size() - 1).getPath());
                    adapter.setSelectItem(movieList.size() - 1);
                    nextMovie = movieList.size() - 1;
                }
            }
        }
    }


    private void nextPlay() {
        adapter.notifyDataSetInvalidated();

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            playStop.setImageResource(R.mipmap.play);
            botplay.setImageResource(R.mipmap.play);
            big.setVisibility(View.GONE);
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (movieList.size() == 0) {
                videoView.setVideoPath(movieList.get(0).getPath());
            }
            if (movieList.size() > 0) {
                nextMovie++;
                if (nextMovie < movieList.size()) {
                    Log.e("next", "1" + nextMovie);
                    videoView.setVideoPath(movieList.get(nextMovie).getPath());
                    adapter.setSelectItem(nextMovie);
                    playStop.setImageResource(R.mipmap.play);
                    botplay.setImageResource(R.mipmap.play);
                    big.setVisibility(View.GONE);
                    lastlast = true;
                }
                if ((nextMovie) == movieList.size()) {
                    Log.e("next", "2" + nextMovie);
                    videoView.setVideoPath(movieList.get(0).getPath());
                    adapter.setSelectItem(0);
                    nextMovie = 0;
                }
            }
        }
    }

    boolean lastlast = true;

    private void playAndStop() {
        if (mediaPlayer != null) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                playStop.setImageResource(R.mipmap.pause);
                botplay.setImageResource(R.mipmap.pause);
                big.setVisibility(View.VISIBLE);
            } else {
                upDateTime();
                upDateTime2();
                mediaPlayer.start();
                playStop.setImageResource(R.mipmap.play);
                botplay.setImageResource(R.mipmap.play);
                big.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        ll2.setVisibility(View.VISIBLE);
        ll3.setVisibility(View.VISIBLE);
        emptyRl.setVisibility(View.VISIBLE);
        botrl.setVisibility(View.GONE);
        videoView.setMediaController(null);
        topll.setVisibility(View.GONE);
        if (movieList.size() < 0) {
            Log.e("last", movieList.size() + "<0");
        } else {
            Log.e("last", movieList.size() + ">0");
            if (nextMovie + 1 <= movieList.size() - 1) {
                videoView.setVideoPath(movieList.get(nextMovie + 1).getPath());
                nextMovie = nextMovie + 1;
                adapter.setSelectItem(nextMovie);
                adapter.notifyDataSetInvalidated();
            }
//            else {
//
//                seekBar.setEnabled(false);
//                seekBar.setProgress(0);
//                playStop.setEnabled(false);
//                playStop.setImageResource(R.mipmap.pause);
////                big.setImageResource(R.mipmap.bigstop);
//                fullBtn.setEnabled(false);
////                seekBar.setProgress(0);
////                tvDuration.setText("00:00:00");
////                tvCurrent.setText("00:00:00");
//                adapter.setSelectItem(-1);
//                adapter.notifyDataSetInvalidated();
//                fullBtn.setEnabled(false);
//                videoView.setBackgroundResource(R.mipmap.magic);
//            }
        }
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            playStop.setImageResource(R.mipmap.play);
            botplay.setImageResource(R.mipmap.play);
            big.setVisibility(View.GONE);
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (movieList.size() == 0) {
                videoView.setVideoPath(movieList.get(0).getPath());
            }
            if (movieList.size() > 0) {
                nextMovie++;
                if (nextMovie < movieList.size()) {
                    Log.e("next", "1" + nextMovie);
                    videoView.setVideoPath(movieList.get(nextMovie).getPath());
                    adapter.setSelectItem(nextMovie);
                    playStop.setImageResource(R.mipmap.play);
                    botplay.setImageResource(R.mipmap.play);
                    big.setVisibility(View.GONE);
                    lastlast = true;
                }
                if ((nextMovie) == movieList.size()) {
                    Log.e("next", "2" + nextMovie);
                    videoView.setVideoPath(movieList.get(0).getPath());
                    adapter.setSelectItem(0);
                    nextMovie = 0;
                }
            }
        }
    }

    private class Myadapter extends BaseAdapter {
        @Override
        public int getCount() {
            return movieList.size();
        }

        @Override
        public Object getItem(int position) {
            return movieList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(getApplicationContext(), R.layout.item, null);
                holder.tvname = (TextView) convertView.findViewById(R.id.tvname);
                holder.sort = (TextView) convertView.findViewById(R.id.  rank);
                holder.tvsize = (TextView) convertView.findViewById(R.id.tcsize);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.sort.setText(String.valueOf(position+1)+"");
            holder.tvname.setText(movieList.get(position).getName());
            //选中的高亮
            if (selectItem == position) {
                holder.tvname.setTextColor(Color.rgb(177,10,31));
                holder.sort.setTextColor(Color.rgb(177,10,31));
            } else {
                holder.tvname.setTextColor(Color.WHITE);
                holder.sort.setTextColor(Color.WHITE);
                convertView.setBackgroundColor(Color.rgb(0x1B, 0x1F, 0x2F));
            }
            showPlayStateIcon(convertView,position);
            return convertView;
        }

        private void showPlayStateIcon(View convertView, int position) {
            ImageView playerImage = (ImageView) convertView.findViewById(R.id.play_view);
            TextView pduration = (TextView) convertView.findViewById(R.id.tv_item_duration);
            playerImage.setImageResource(R.drawable.list_pause_indicator);
            pduration.setText(FormatHelper.formatDuration((int) movieList.get(position).getDuration()));

            if (position != nextMovie){
                playerImage.setVisibility(View.GONE);
                pduration.setVisibility(View.VISIBLE);
            }else{//正在播放
                playerImage.setVisibility(View.VISIBLE);
                pduration.setVisibility(View.GONE);
            }
        }

        private int selectItem = -1;

        public void setSelectItem(int selectPosition) {
            this.selectItem = selectPosition;
        }

        class ViewHolder {
            TextView tvsize, tvname, sort;
        }
    }

    long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("back-key","tag1");
        if (keyCode == KeyEvent.KEYCODE_BACK && ll2.getVisibility() == View.GONE) {
            Log.i("back-key","tag2");
            ll2.setVisibility(View.VISIBLE);
            ll3.setVisibility(View.VISIBLE);
            emptyRl.setVisibility(View.VISIBLE);
            botrl.setVisibility(View.GONE);
            videoView.setMediaController(null);
            topll.setVisibility(View.GONE);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.i("back-key","tag3");
            if (System.currentTimeMillis() - exitTime > 2000) {
                String exitMsg = getResources().getString(R.string.app_exit);
                Toast.makeText(this, exitMsg, Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                if (movieList != null) {
                    movieList.clear();
                }
                finish();
                System.exit(0);
            }
            return true;
        }
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_VOLUME_UP:
//                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
//                return true;
//            case KeyEvent.KEYCODE_VOLUME_DOWN:
//                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
//                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
//                return true;
//            default:
//                break;
//        }
        Log.i("back-key","tag4");
        return super.onKeyDown(keyCode, event);
    }
//    @Override
//    public void onBackPressed() {
////        super.onBackPressed();
//        Log.i("back-key","tag1");
//        if (ll2.getVisibility() == View.GONE) {
//            Log.i("back-key","tag2");
//            ll2.setVisibility(View.VISIBLE);
//            ll3.setVisibility(View.VISIBLE);
//            emptyRl.setVisibility(View.VISIBLE);
//            botrl.setVisibility(View.GONE);
//            videoView.setMediaController(null);
//            topll.setVisibility(View.GONE);
//        }else {
//            Log.i("back-key","tag3");
//            if (System.currentTimeMillis() - exitTime > 2000) {
//                String exitMsg = getResources().getString(R.string.app_exit);
//                Toast.makeText(this, exitMsg, Toast.LENGTH_SHORT).show();
//                exitTime = System.currentTimeMillis();
//            } else {
//                if (movieList != null) {
//                    movieList.clear();
//                }
//                finish();
//                System.exit(0);
//            }
//        }
    //遍历指定路径的视频
    public void traverseFolder2(String path) {

        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                if (files.length == 0) {
                    System.out.println("文件夹是空的!");
                    return;
                } else {
                    for (File file2 : files) {
                        if (file2.isDirectory()) {
                            System.out.println("文件夹:" + file2.getAbsolutePath());
                            traverseFolder2(file2.getAbsolutePath());
                        } else {
                            if (file2.toString().endsWith(".mp4") || file2.toString().endsWith(".mkv") || file2.toString().endsWith(".rmvb") || file2.toString().endsWith(".wmv") || file2.toString().endsWith(".MOV")
                                    || file2.toString().endsWith(".flv") || file2.toString().endsWith(".avi")
                                    || file2.toString().endsWith(".3gp") || file2.toString().endsWith(".rm")) {
                                MovieMo movie = new MovieMo();
                                movie.name = file2.getName();
                                movie.path = file2.getPath();
                                double fileOrFilesSize = FileSizeUtil.getFileOrFilesSize(movie.path, 3);
                                if (fileOrFilesSize > 10) {
                                    newList.add(movie);
                                    movieList.add(movie);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    private void freshUi() {
        handler2.sendEmptyMessageDelayed(7, 500);
    }

    private class MyUsbBroad extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((Intent.ACTION_MEDIA_MOUNTED).equals(intent.getAction())) {
                alertDialog1 = new AlertDialog.Builder(MainActivity.this).setView(getLayoutInflater().inflate(R.layout.usb_out, null)).setCancelable(false).show();
                ALWYASFRESH = false;
                handler2.removeMessages(6);//暫停播放，則停止handler刷新
                i = 1;
                handler2.sendEmptyMessageDelayed(6, 1000);
                handler2.sendEmptyMessageDelayed(8, 1000);
                Toast.makeText(MainActivity.this, "U盘插入", Toast.LENGTH_LONG).show();
                String path = intent.getDataString();
                pathString = path.split("file://")[1];
//                handler2.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        //添加后才显示item总时长
////                        alertDialog1.dismiss();
////                        movieList.clear();
////                        searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
////                        adapter.notifyDataSetChanged();
//                        handler2.sendEmptyMessage(10);
//                    }
//                }, 5000);
//            }
            new Thread() {
                @Override
                public void run() {
                    //添加后才显示item总时长
//                        alertDialog1.dismiss();
//                        movieList.clear();
//                        searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//                        adapter.notifyDataSetChanged();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler2.sendEmptyMessage(10);
                }
            }.start();
        }
            if ((Intent.ACTION_MEDIA_EJECT).equals(intent.getAction())) {
                handler2.removeMessages(8);
                next.setEnabled(false);
                pre.setEnabled(false);
                botnext.setEnabled(false);
                botpre.setEnabled(false);
                big.setVisibility(View.GONE);
                playStop.setEnabled(false);
                seekBar.setEnabled(false);
                seekBar.setProgress(0);
                botseekbar.setProgress(0);
                botseekbar.setEnabled(false);
                fullBtn.setEnabled(false);
                emptyRl.setVisibility(View.VISIBLE);
                ll2.setVisibility(View.VISIBLE);
                ll3.setVisibility(View.VISIBLE);
                botrl.setVisibility(View.GONE);
                topll.setVisibility(View.GONE);
                String usb_noVideo = getResources().getString(R.string.usb_noVideo);
                Toast.makeText(MainActivity.this, "U盘拔出", Toast.LENGTH_LONG).show();
                traverseFolder2(Environment.getExternalStorageDirectory().getAbsolutePath());
                for (int i = 0; i < movieList.size(); i++) {
                    MediaScannerConnection.scanFile(MainActivity.this, new String[]{
                            movieList.get(i).getPath()}, null, null);
                }
                String usb_tvc = getResources().getString(R.string.usb_tvc);
                String usb_tvd = getResources().getString(R.string.usb_tvd);
                tvCurrent.setText(usb_tvc);
                tvDuration.setText(usb_tvd);
                handler2.removeMessages(6);
                emptyName.setText(usb_noVideo);
                newList.clear();
                System.exit(0);
//                movieList.clear();
//                searchMovie(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//                adapter.notifyDataSetChanged();
                ALWYASFRESH = true;
                alwaysFresh();
                i = 1;

            }
        }
    }

    private class MyButton extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        if (mediaPlayer != null) {
                            pre.startAnimation(animation);
                        }
                        seekBar.setEnabled(true);
                        fullBtn.setEnabled(true);
                        playStop.setEnabled(true);
                        prePlay();
                        Log.e("back", "-1");
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        if (mediaPlayer != null) {
                            next.startAnimation(animation);
                        }
                        seekBar.setEnabled(true);
                        fullBtn.setEnabled(true);
                        playStop.setEnabled(true);
                        nextPlay();
                        Log.e("back", "+1");
                    }
                }
            }
        }
    }
}
