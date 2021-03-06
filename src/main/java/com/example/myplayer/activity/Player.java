package com.example.myplayer.activity;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myplayer.R;
import com.example.myplayer.interfaces.IPlayerController;
import com.example.myplayer.interfaces.IPlayerViewController;
import com.example.myplayer.service.PlayService;
import com.example.myplayer.utils.DlCRUD;
import com.example.myplayer.utils.MenuList;
import com.example.myplayer.utils.LikeCRUD;


import java.io.File;

import static com.example.myplayer.interfaces.IPlayerController.PLAY_STATE_PAUSE;
import static com.example.myplayer.interfaces.IPlayerController.PLAY_STATE_START;
import static com.example.myplayer.interfaces.IPlayerController.PLAY_STATE_STOP;

public class Player extends AppCompatActivity {

    private static final int LAST_MUSIC = -1;
    private static final int NEXT_MUSIC = 1;
    private static final String TAG = "Player";
    private ImageView mPic;
    private ObjectAnimator mRotation;
    private IPlayerController mController;
    private PlayerConnection mPlayerConnection;
    private boolean isTouch = false;
    private SeekBar mSeek;
    private Button mSp;
    private static int mPosition;
    private static String mMusicId;;
    private com.example.myplayer.db.MusicListInfo mMusicInfo;
    private boolean isLike = false;
    private Button mLike;
    private Button mDownload;

    public static Handler sHandler;
    private Button mLast;
    private Button mNext;
    private int mMenuSize;

    private Button mMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_player);
        //???????????????
        initData();
        //???????????????
        initView();
        //??????????????????????????????
        initEvent();
        initService();
        initBindService();
    }

    private void initData() {
        /**
         * ?????????????????????????????????
         */
        Intent intent = getIntent();
        int newPosition = intent.getIntExtra("position",-1);
        String newMusicId = MenuList.sMusicListInfo.get(newPosition).getId();
        mMenuSize = MenuList.sMusicListInfo.size();
        if (mMusicId!=null&&mMusicId.equals(newMusicId)){
            isLike = true;
        }
        mPosition = newPosition;
    }

    /**
     * ??????????????????????????????
     */
    private void initView() {
        mMusicId = MenuList.sMusicListInfo.get(mPosition).getId();;
        mMusicInfo = MenuList.sMusicListInfo.get(mPosition);
        ((TextView)findViewById(R.id.player_title)).setText(mMusicInfo.getName());
        ((TextView)findViewById(R.id.player_alis)).setText(mMusicInfo.getAlia());
        ((TextView)findViewById(R.id.player_artists)).setText(mMusicInfo.getArtistsName());
        mLike = findViewById(R.id.player_like);
        mDownload = findViewById(R.id.player_download);
        mLast = findViewById(R.id.player_last);
        mNext = findViewById(R.id.player_next);
        mMenu = findViewById(R.id.menu);

        mPic = findViewById(R.id.player_pic);
        //??????????????????
        Glide.with(mPic.getContext()).load(mMusicInfo.getPicUrl()).into(mPic);
        //???????????????????????????????????????????????????????????????
        sHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                String showInfo = "";
                if(msg.what == 301){
                    showInfo = "????????????";
                }else if(msg.what == 302){
                    showInfo="????????????";
                }else if(msg.what == 303){
                    showInfo = "???????????????????????????";
                }
                Toast.makeText(Player.this, showInfo, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        if (mRotation != null) {
            mRotation.start();
            return;
        }


        //????????????????????????????????????????????????360???
        mRotation = ObjectAnimator
                .ofFloat(mPic, "rotation", 0,360)
                .setDuration(15000);
        mRotation.setRepeatCount(Animation.INFINITE);
        mRotation.setInterpolator(new LinearInterpolator());
        mRotation.start();
    }


    /**
     * ???????????????????????????????????????????????????
     */

    private void initEvent() {
        mSeek = findViewById(R.id.player_seek);
        mSp = findViewById(R.id.player_sp);
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //??????????????????????????????
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //????????????????????????
                isTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //?????????????????????
                if (mController != null) {
                    mController.seekTo(seekBar.getProgress());
                    isTouch=false;
                }
            }
        });

        //????????????????????????????????????????????????
        mSp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mController != null) {
                    mController.pauseOrResume();
                }
            }
        });

        //????????????????????????????????????
        mLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LikeCRUD().likeAdd(getApplicationContext(),mPosition);
            }
        });

        //?????????????????????????????????
        mDownload.setOnClickListener(new View.OnClickListener() {
            // TODO: 2020/11/10 ???????????????????????? 
            @Override
            public void onClick(View v) {
                File path = getFilesDir();
                File file = new File(path,"/music/"+mMusicInfo.getName()+".mp3");
                Toast.makeText(Player.this, "????????????", Toast.LENGTH_SHORT).show();
                new DlCRUD().downLoadMusic(mPosition,file);
            }
        });




        //???????????????????????????
        mLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMusic(LAST_MUSIC);
            }
        });

        //?????????????????????
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMusic(NEXT_MUSIC);
            }
        });
    }

    /**
     * ????????????
     * @param mode ???????????????????????????mode=-1???/????????????model=1??????
     */
    public void changeMusic(int mode){
        isLike = false;
        int newPosition;
        int i = mode;
        do {
            if (i!=mode){
                Toast.makeText(this, "????????????????????????????????????", Toast.LENGTH_SHORT).show();
            }
            newPosition = (mPosition+mode+mMenuSize)%mMenuSize;
            i = i+mode;
        }while (MenuList.sMusicListInfo.get(newPosition).getUrl()==null);
        mPosition = newPosition;
        initView();
        startPlay();
    }

    /**
     * ?????????????????????
     */
    private void initService() {
        Log.i(TAG,"->initService");
        startService(new Intent(this, PlayService.class));
    }

    /**
     * ??????????????????
     */
    private void initBindService() {
        Log.i(TAG,"->initBindService");
        Intent intent = new Intent(this, PlayService.class);
        if (mPlayerConnection == null) {
            Log.i(TAG,"->mPlayerConnection");
            mPlayerConnection = new PlayerConnection();
        }
        bindService(intent,mPlayerConnection,BIND_AUTO_CREATE);
    }

    private class PlayerConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG,"->onServiceConnected");
            mController = (IPlayerController)service;
            //????????????????????????UI????????????????????????
            mController.registerIPlayViewController(mPlayerViewController);
            startPlay();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"->onServiceDisconnected");
            mController = null;
        }
    }

    private IPlayerViewController mPlayerViewController = new IPlayerViewController() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onPlayStateChange(int state) {
            switch (state){
                case PLAY_STATE_START:
                    mRotation.resume();
                    mSp.setBackgroundResource(R.drawable.pause);
                    break;
                case PLAY_STATE_PAUSE:
                    mRotation.pause();
                    mSp.setBackgroundResource(R.drawable.start);
                    //????????????
                case PLAY_STATE_STOP:
                    break;
            }
        }

        @Override
        public void onSeekChange(int seek) {

            /**
             * ????????????????????????
             */
            if (mSeek != null&&isTouch==false) {
                mSeek.setProgress(seek);
                if(seek == 100){
                    mController.seekTo(0);
                }
            }
        }
    };

    /**
     * ????????????
     */
    private void startPlay() {
        //??????????????????
        if (mController != null) {
            mController.start(mMusicInfo.getUrl(), isLike);
        }
    }

    /**
     * ???????????????????????????
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerConnection != null) {
            unbindService(mPlayerConnection);
            mPlayerViewController = null;
        }
    }
    public void like(View view){
        if(new LikeCRUD().likeSelete(getApplicationContext(),10)){
            Intent intent = new Intent(Player.this,Result.class);
            intent.putExtra("Tag","????????????");
            intent.putExtra("keyword","");
            startActivity(intent);
        }else {
            Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
        }
    }
}
