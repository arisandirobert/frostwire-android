/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.activities;

import java.util.Formatter;
import java.util.Locale;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.services.NativeAndroidPlayer;
import com.frostwire.android.gui.util.MusicUtils;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractSwipeDetector;
import com.frostwire.android.gui.views.MediaPlayerControl;
import com.frostwire.android.util.StringUtils;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class MediaPlayer2Activity extends AbstractActivity implements MediaPlayerControl {

    private static final String TAG = "FW.MediaPlayerActivity";

    private MediaPlayer mediaPlayer;
    private FileDescriptor mediaFD;

    private BroadcastReceiver broadcastReceiver;

    private AdView adView;

    public MediaPlayer2Activity() {
        super(R.layout.activity_mediaplayer2, false, 0);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_MEDIA_PLAYER_STOPPED)) {
                    try {
                        finish();
                    } catch (Throwable e) {
                        // ignore
                    }
                } else if (intent.getAction().equals(Constants.ACTION_MEDIA_PLAYER_PLAY)) {
                    try {
                        initComponents();
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        };
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        sync();
    }

    public void pause() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (Throwable e) {
                Log.w(TAG, String.format("Review logic: %s", e.getMessage()));
            }
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.start();
            } catch (Throwable e) {
                Log.w(TAG, String.format("Review logic: %s", e.getMessage()));
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            finish();
            Engine.instance().getMediaPlayer().stop();
        }
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (Throwable e) {
                // ignore
                return 0;
            }
        } else {
            return 0;
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Throwable e) {
                // ignore
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void seekTo(int i) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(i);
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    public boolean isPlaying() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.isPlaying();
            } catch (Throwable e) {
                // ignore
                return false;
            }
        } else {
            return false;
        }
    }

    public int getBufferPercentage() {
        return 0;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canStop() {
        return true;
    }

    @Override
    protected void initComponents() {
        if (!(Engine.instance().getMediaPlayer() instanceof NativeAndroidPlayer)) {
            Log.e(TAG, "Only media player of type NativeAndroidPlayer is supported");
            return;
        }

        initGestures();

        mediaPlayer = ((NativeAndroidPlayer) Engine.instance().getMediaPlayer()).getMediaPlayer();
        mediaFD = Engine.instance().getMediaPlayer().getCurrentFD();

        if (mediaPlayer != null) {
            setMediaPlayer(this);
        }

        if (mediaFD != null) {
            TextView artist = findView(R.id.activity_mediaplayer_artist);
            if (!StringUtils.isNullOrEmpty(mediaFD.artist, true)) {
                artist.setText(mediaFD.artist);
            }
            TextView title = findView(R.id.activity_mediaplayer_title);
            if (!StringUtils.isNullOrEmpty(mediaFD.title, true)) {
                title.setText(mediaFD.title);
            }
            TextView album = findView(R.id.activity_mediaplayer_album);
            if (!StringUtils.isNullOrEmpty(mediaFD.album, true)) {
                album.setText(mediaFD.album);
            }

            ImageView image = findView(R.id.activity_mediaplayer_artwork);
            Bitmap coverArt = readArtWork();
            if (coverArt != null) {
                image.setImageBitmap(coverArt);
            }
        } else {
            Engine.instance().getMediaPlayer().stop();
        }

        // media player controls
        buttonPrevious = (ImageButton) findViewById(R.id.activity_mediaplayer_button_previous);
        if (buttonPrevious != null) {
            buttonPrevious.setOnClickListener(previousListener);
        }

        buttonPause = (ImageButton) findViewById(R.id.activity_mediaplayer_button_play);
        if (buttonPause != null) {
            buttonPause.requestFocus();
            buttonPause.setOnClickListener(pauseListener);
        }

        buttonNext = (ImageButton) findViewById(R.id.activity_mediaplayer_button_next);
        if (buttonNext != null) {
            buttonNext.setOnClickListener(nextListener);
        }

        progress = (SeekBar) findViewById(R.id.view_media_controller_progress);
        if (progress != null) {
            if (progress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) progress;
                seeker.setOnSeekBarChangeListener(seekListener);
            }
            progress.setMax(1000);
        }

        endTime = (TextView) findViewById(R.id.view_media_controller_time_end);
        currentTime = (TextView) findViewById(R.id.view_media_controller_time_current);
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        /*
        LinearLayout llayout = findView(R.id.activity_mediaplayer_adview_placeholder);
        adView = new AdView(this, AdSize.SMART_BANNER, Constants.ADMOB_PUBLISHER_ID);
        adView.setVisibility(View.GONE);
        llayout.addView(adView, 0);

        if (mediaFD != null) {
            UIUtils.supportFrostWire(adView, mediaFD.artist + " " + mediaFD.title + " " + mediaFD.album + " " + mediaFD.year);
        }
        */
    }

    private void initGestures() {
        LinearLayout lowestLayout = findView(R.id.activity_mediaplayer_layout);
        lowestLayout.setOnTouchListener(new AbstractSwipeDetector() {
            @Override
            public void onLeftToRightSwipe() {
                Engine.instance().getMediaPlayer().playPrevious();
            }

            @Override
            public void onRightToLeftSwipe() {
                Engine.instance().getMediaPlayer().playNext();
            }

            @Override
            public boolean onMultiTouchEvent(View v, MotionEvent event) {
                Engine.instance().getMediaPlayer().togglePause();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Constants.ACTION_MEDIA_PLAYER_STOPPED);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PLAY);
        registerReceiver(broadcastReceiver, filter);

        enableLock(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);
        enableLock(true);
    }

    private Bitmap readArtWork() {
        Bitmap artwork = null;

        try {
            artwork = MusicUtils.getArtwork(this, mediaFD.id, -1);
            artwork = applyEffect1(artwork);
        } catch (Throwable e) {
            Log.e(TAG, "Can't read the cover art for fd: " + mediaFD);
        }

        return artwork;
    }

    private void enableLock(boolean enable) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);

        if (enable) {
            lock.reenableKeyguard();
        } else {
            lock.disableKeyguard();
        }
    }

    private Bitmap applyEffect1(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        Camera cam = new Camera();
        cam.rotateX(-120);

        Matrix mat = new Matrix();
        cam.getMatrix(mat);

        int cX = width / 2;
        int cY = height / 2;

        mat.preTranslate(-cX, -cY);
        mat.postTranslate(cX, cY);

        return Bitmap.createBitmap(bmp, 0, 0, width, height, mat, false);
    }

    private Bitmap applyReflection(Bitmap bitmap) {

        //The gap we want between the reflection and the original image
        final int reflectionGap = 4;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        //This will not scale but will flip on the Y axis
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        //Create a Bitmap with the flip matix applied to it.
        //We only want the bottom half of the image
        Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height / 3, width, height / 3, matrix, false);

        //Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 3), Config.ARGB_8888);

        //Create a new Canvas with the bitmap that's big enough for
        //the image plus gap plus reflection
        Canvas canvas = new Canvas(bitmapWithReflection);
        //Draw in the original image
        canvas.drawBitmap(bitmap, 0, 0, null);
        //Draw in the gap
        Paint defaultPaint = new Paint();
        canvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);
        //Draw in the reflection
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

        //Create a shader that is a linear gradient that covers the reflection
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0, bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
        //Set the paint to use this shader (linear gradient)
        paint.setShader(shader);
        //Set the Transfer mode to be porter duff and destination in
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        //Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

        return bitmapWithReflection;
    }

    // media player controls

    private static final int SHOW_PROGRESS = 1;

    private MediaPlayerControl player;
    private ImageButton buttonPrevious;
    private ImageButton buttonPause;
    private ImageButton buttonNext;
    private ProgressBar progress;
    private TextView endTime;
    private TextView currentTime;

    private boolean dragging;

    private StringBuilder formatBuilder;
    private Formatter formatter;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
            case SHOW_PROGRESS:
                pos = setProgress();
                if (!dragging && player != null && player.isPlaying()) {
                    msg = obtainMessage(SHOW_PROGRESS);
                    sendMessageDelayed(msg, 1000 - (pos % 1000));
                }
                break;
            }
        }
    };

    private View.OnClickListener pauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            sync();
        }
    };

    private View.OnClickListener previousListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (player != null) {
                if (player.getCurrentPosition() < 5000) {
                    Engine.instance().getMediaPlayer().playPrevious();
                } else {
                    player.seekTo(0);
                }
            }
        }
    };

    private View.OnClickListener nextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (player != null) {
                Engine.instance().getMediaPlayer().playNext();
            }
        }
    };

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            sync();

            dragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            handler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            if (player != null) {
                long duration = player.getDuration();
                long newposition = (duration * progress) / 1000L;
                player.seekTo((int) newposition);
                if (currentTime != null)
                    currentTime.setText(stringForTime((int) newposition));
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            dragging = false;
            setProgress();
            updatePausePlay();
            sync();

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            handler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    public void setMediaPlayer(MediaPlayerControl player) {
        this.player = player;
        updatePausePlay();
    }

    public void sync() {
        setProgress();
        if (buttonPause != null) {
            buttonPause.requestFocus();
        }
        disableUnsupportedButtons();
        updatePausePlay();

        // cause the progress bar to be updated This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        handler.sendEmptyMessage(SHOW_PROGRESS);

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                sync();
                if (buttonPause != null) {
                    buttonPause.requestFocus();
                }
            }
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (uniqueDown && player.isPlaying()) {
                updatePausePlay();
                sync();
                player.stop();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        }

        sync();
        return super.dispatchKeyEvent(event);
    }

    /*
    @Override
    public void setEnabled(boolean enabled) {
        if (buttonPause != null) {
            buttonPause.setEnabled(enabled);
        }
        if (progress != null) {
            progress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }
    */

    private void disableUnsupportedButtons() {
        try {
            if (buttonPause != null && !player.canPause()) {
                buttonPause.setEnabled(false);
            }
        } catch (Throwable e) {
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (player == null || dragging) {
            return 0;
        }
        int position = player.getCurrentPosition();
        int duration = player.getDuration();
        if (progress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                progress.setProgress((int) pos);
            }
            int percent = player.getBufferPercentage();
            progress.setSecondaryProgress(percent * 10);
        }

        if (endTime != null) {
            endTime.setText(stringForTime(duration));
        }
        if (currentTime != null) {
            currentTime.setText(stringForTime(position));
        }

        return position;
    }

    private void updatePausePlay() {
        if (buttonPause == null || player == null) {
            return;
        }

        if (player.isPlaying()) {
            buttonPause.setImageResource(R.drawable.player_pause_icon);
        } else {
            buttonPause.setImageResource(R.drawable.player_play_icon);
        }
    }

    private void doPauseResume() {
        if (player == null) {
            return;
        }

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.resume();
        }

        updatePausePlay();
    }
}