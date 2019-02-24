package com.example.aa.myapplication;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Rational;
import android.view.View;
import android.widget.ScrollView;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.O)
public class TestActivity extends AppCompatActivity {

    /** Intent action for media controls from Picture-in-Picture mode.
     * 从图片-图片模式中对媒体控制的意图行动。
     * */
    private static final String ACTION_MEDIA_CONTROL = "media_control";

    /** Intent extra for media controls from Picture-in-Picture mode.
     *  从图片图片模式中 为媒体控制提供额外的功能。
     * */
    private static final String EXTRA_CONTROL_TYPE = "control_type";

    /** The request code for play action PendingIntent. */
    private static final int REQUEST_PLAY = 1;

    /** The request code for pause action PendingIntent. */
    private static final int REQUEST_PAUSE = 2;

    /** The request code for info action PendingIntent. */
    private static final int REQUEST_INFO = 3;

    /** The intent extra value for play action. */
    private static final int CONTROL_TYPE_PLAY = 1;

    /** The intent extra value for pause action. */
    private static final int CONTROL_TYPE_PAUSE = 2;

    /** The arguments to be used for Picture-in-Picture mode. */
    private final PictureInPictureParams.Builder mPictureInPictureParamsBuilder =
            new PictureInPictureParams.Builder();

    /** This shows the video. */
    private MovieView mMovieView;

    /** The bottom half of the screen; hidden on landscape */
    private ScrollView mScrollView;

    /** A {@link BroadcastReceiver} to receive action item events from Picture-in-Picture mode. */
    private BroadcastReceiver mReceiver;

    private String mPlay;
    private String mPause;

    private final View.OnClickListener mOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.pip://进入画中画模式
                            minimize();
                            break;
                    }
                }
            };

    /** Callbacks from the {@link MovieView} showing the video playback. 显示视频回放*/
    private MovieView.MovieListener mMovieListener =
            new MovieView.MovieListener() {

                @Override
                public void onMovieStarted() {
                    // We are playing the video now. In PiP mode, we want to show an action item to
                    // pause the video.
                    updatePictureInPictureActions(
                            R.drawable.ic_pause_24dp, mPause, CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
                }

                @Override
                public void onMovieStopped() {
                    // The video stopped or reached its end. In PiP mode, we want to show an action
                    // item to play the video.
                    updatePictureInPictureActions(
                            R.drawable.ic_play_arrow_24dp, mPlay, CONTROL_TYPE_PLAY, REQUEST_PLAY);
                }

                @Override
                public void onMovieMinimized() {
                    // The MovieView wants us to minimize 最小化 it. We enter Picture-in-Picture mode now.
                    minimize();
                }
            };

    /**
     * Update the state of pause/resume action item in Picture-in-Picture mode.
     *
     * @param iconId The icon to be used.
     * @param title The title text.
     * @param controlType The type of the action. either {@link #CONTROL_TYPE_PLAY} or {@link
     *     #CONTROL_TYPE_PAUSE}.
     * @param requestCode The request code for the {@link PendingIntent}.
     */
    void updatePictureInPictureActions(
            @DrawableRes int iconId, String title, int controlType, int requestCode) {
        final ArrayList<RemoteAction> actions = new ArrayList<>();

        // This is the PendingIntent that is invoked when a user clicks on the action item.
        // You need to use distinct request codes for play and pause, or the PendingIntent won't
        // be properly updated.
        final PendingIntent intent =
                PendingIntent.getBroadcast(TestActivity.this, requestCode,
                        new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType), 0);
        final Icon icon = Icon.createWithResource(TestActivity.this, iconId);
        actions.add(new RemoteAction(icon, title, title, intent));

        // Another action item. This is a fixed action.
        actions.add(
                new RemoteAction(
                        Icon.createWithResource(TestActivity.this, R.drawable.ic_info_24dp),
                        getString(R.string.info),
                        getString(R.string.info_description),
                        PendingIntent.getActivity(
                                TestActivity.this,
                                REQUEST_INFO,
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(getString(R.string.info_uri))),
                                0)));

        mPictureInPictureParamsBuilder.setActions(actions);

        // This is how you can update action items (or aspect ratio) for Picture-in-Picture mode.
        // Note this call can happen even when the app is not in PiP mode. In that case, the
        // arguments will be used for at the next call of #enterPictureInPictureMode.
        setPictureInPictureParams(mPictureInPictureParamsBuilder.build());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        // Prepare string resources for Picture-in-Picture actions.
        mPlay = getString(R.string.play);
        mPause = getString(R.string.pause);

        // View references
        mMovieView = findViewById(R.id.movie);
        mScrollView = findViewById(R.id.scroll);


        // Set up the video; it automatically starts.
        //设置视频自动开始
        mMovieView.setMovieListener(mMovieListener);

        findViewById(R.id.pip).setOnClickListener(mOnClickListener);

    }

    @Override
    protected void onStop() {
        // On entering Picture-in-Picture mode, onPause is called, but not onStop.
        // For this reason, this is the place where we should pause the video playback.
        //在进入画中画模式的时候，调用了暂停，没调用stop方法时
        //出于这个原因，我们应该暂停视频回放。
        mMovieView.pause();
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!isInPictureInPictureMode()) {
            // Show the video controls so the video can be easily resumed.
            //显示视频控制，这样视频就可以很容易地恢复。
            mMovieView.showControls();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustFullScreen(newConfig);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            adjustFullScreen(getResources().getConfiguration());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode, Configuration configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, configuration);
        if (isInPictureInPictureMode) {
            // Starts receiving events from action items in PiP mode.
            //开始接收画中画模式的操作
            mReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent == null
                                    || !ACTION_MEDIA_CONTROL.equals(intent.getAction())) {
                                return;
                            }
                            // This is where we are called back from Picture-in-Picture action items.
                            //这就是我们从画中画模式的操作回调的地方
                            final int controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0);
                            switch (controlType) {
                                case CONTROL_TYPE_PLAY:
                                    mMovieView.play();
                                    break;
                                case CONTROL_TYPE_PAUSE:
                                    mMovieView.pause();
                                    break;
                            }
                        }
                    };
            registerReceiver(mReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
        } else {
            // We are out of PiP mode. We can stop receiving events from it.
            // 当我们不在画中画模式时，停止接收广播
            unregisterReceiver(mReceiver);
            mReceiver = null;
            // Show the video controls if the video is not playing
            //当视频不在播放时，显示控制条
            if (mMovieView != null && !mMovieView.isPlaying()) {
                mMovieView.showControls();
            }
        }
    }

    /** Enters Picture-in-Picture mode. 进入画中画模式*/
    void minimize() {
        if (mMovieView == null) {
            return;
        }
        // Hide the controls in picture-in-picture mode. 在画中画模式中 隐藏控制条
        mMovieView.hideControls();
        // Calculate the aspect ratio of the PiP screen. 计算屏幕的纵横比
        Rational aspectRatio = new Rational(mMovieView.getWidth(), mMovieView.getHeight());
        mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build();
        enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());
    }

    /**
     * Adjusts immersive full-screen flags depending on the screen orientation.
     *根据屏幕的方向调整沉浸式全屏幕的标志。
     * @param config The current {@link Configuration}.
     */
    private void adjustFullScreen(Configuration config) {
        final View decorView = getWindow().getDecorView();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            mScrollView.setVisibility(View.GONE);
            mMovieView.setAdjustViewBounds(false);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            mScrollView.setVisibility(View.VISIBLE);
            mMovieView.setAdjustViewBounds(true);
        }
    }
}
