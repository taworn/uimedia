package diy.uimedia;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

/**
 * A media player fragment.
 */
public class MediaPlayerFragment extends Fragment {

    private static final String TAG = MediaPlayerFragment.class.getSimpleName();

    private SeekBar seekBar;
    private TextView textTimeCurrent;
    private TextView textTime;
    private ImageButton buttonPlay;
    private ImageButton buttonStop;
    private ImageButton buttonFastRewind;
    private ImageButton buttonFastForward;
    private ImageButton buttonToStart;
    private ImageButton buttonToEnd;
    private CheckBox checkLoop;
    private SurfaceView surfaceView;

    // open/close flag with path name
    private String path;
    private boolean opened;

    // media player and properties
    private MediaPlayer player;
    private boolean paused;
    private int duration;
    private int position;
    private int deltaPosition;
    private boolean looping;

    // handler to control seekbar and time
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                int p = !paused ? player.getCurrentPosition() : position;
                if (!paused) {
                    textTimeCurrent.setText(timeToText(p));
                    seekBar.setProgress(p);
                }
                else {
                    int q = seekBar.getProgress();
                    if (p != q) {
                        textTimeCurrent.setText(timeToText(p));
                        seekBar.setProgress(p);
                    }
                }
                handler.postDelayed(runnable, 500);
            }
        }
    };

    public MediaPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_media_player, container, false);
        Log.d(TAG, "onCreateView()");

        seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        textTimeCurrent = (TextView) view.findViewById(R.id.text_time_current);
        textTime = (TextView) view.findViewById(R.id.text_time);
        buttonPlay = (ImageButton) view.findViewById(R.id.button_play);
        buttonStop = (ImageButton) view.findViewById(R.id.button_stop);
        buttonFastRewind = (ImageButton) view.findViewById(R.id.button_fast_rewind);
        buttonFastForward = (ImageButton) view.findViewById(R.id.button_fast_forward);
        buttonToStart = (ImageButton) view.findViewById(R.id.button_to_start);
        buttonToEnd = (ImageButton) view.findViewById(R.id.button_to_end);
        checkLoop = (CheckBox) view.findViewById(R.id.check_loop);
        surfaceView = null;

        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlayClick(view);
            }
        });
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopClick(view);
            }
        });
        buttonFastRewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFastRewindClick(view);
            }
        });
        buttonFastForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFastForwardClick(view);
            }
        });
        buttonToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onToStartClick(view);
            }
        });
        buttonToEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onToEndClick(view);
            }
        });
        checkLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean b = checkLoop.isChecked();
                setLooping(b);
            }
        });

        path = null;
        opened = false;
        player = null;
        paused = false;
        duration = 0;
        position = 0;
        deltaPosition = 15000;
        looping = false;
        return view;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        handler.removeCallbacks(runnable);
        if (player != null) {
            player.stop();
            player.release();
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState()");
        savedInstanceState.putString("path", path);
        savedInstanceState.putBoolean("opened", opened);
        savedInstanceState.putBoolean("paused", paused);
        savedInstanceState.putInt("duration", opened ? player.getDuration() : 0);
        if (opened)
            savedInstanceState.putInt("position", !paused ? player.getCurrentPosition() : position);
        savedInstanceState.putInt("deltaPosition", deltaPosition);
        savedInstanceState.putBoolean("looping", looping);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated(), bundle == " + (savedInstanceState != null ? "ok" : "null"));
        if (savedInstanceState != null) {
            path = savedInstanceState.getString("path");
            boolean opened = savedInstanceState.getBoolean("opened");
            paused = savedInstanceState.getBoolean("paused");
            duration = savedInstanceState.getInt("duration");
            position = savedInstanceState.getInt("position", 0);
            deltaPosition = savedInstanceState.getInt("deltaPosition");
            looping = savedInstanceState.getBoolean("looping");
            if (opened) {
                if (start(path)) {
                    if (paused)
                        player.pause();
                    player.setLooping(looping);
                    textTimeCurrent.setText(timeToText(position));
                }
            }
        }
        setPlayEnabled();
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean open(String path) {
        boolean result = start(path);
        setPlayEnabled();
        Log.d(TAG, "open(), return " + result);
        return result;
    }

    public void close() {
        if (opened) {
            opened = false;
            player.stop();
            player.release();
            player = null;
            paused = false;
            duration = 0;
            position = 0;
            seekBar.setProgress(0);
            textTimeCurrent.setText(timeToText(0));
            setPlayEnabled();
            Log.d(TAG, "close()");
        }
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean b) {
        looping = b;
        if (player != null)
            player.setLooping(looping);
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(SurfaceView view) {
        surfaceView = view;
    }

    private void onPlayClick(View view) {
        if (opened) {
            if (!paused) {
                Log.d(TAG, "Play clicked, switch !paused to paused");
                player.pause();
                paused = true;
                position = player.getCurrentPosition();
            }
            else {
                Log.d(TAG, "Play clicked, switch paused to !paused");
                player.seekTo(position);
                player.start();
                paused = false;
            }
            seekBar.setProgress(position);
            textTimeCurrent.setText(timeToText(position));
            setPlayEnabled();
        }
    }

    private void onStopClick(View view) {
        if (opened) {
            Log.d(TAG, "Stop clicked");
            player.pause();
            paused = true;
            position = 0;
            seekBar.setProgress(0);
            textTimeCurrent.setText(timeToText(0));
            setPlayEnabled();
        }
    }

    private void onFastRewindClick(View view) {
        if (opened) {
            Log.d(TAG, "Fast Rewind clicked");
            int p = !paused ? player.getCurrentPosition() : position;
            p -= deltaPosition;
            if (p < 0)
                p = 0;
            position = p;
            if (!paused)
                player.seekTo(p);
            seekBar.setProgress(p);
            textTimeCurrent.setText(timeToText(p));
        }
    }

    private void onFastForwardClick(View view) {
        if (opened) {
            Log.d(TAG, "Fast Forward clicked");
            int p = !paused ? player.getCurrentPosition() : position;
            p += deltaPosition;
            if (p >= duration)
                p = duration - 1;
            position = p;
            if (!paused)
                player.seekTo(p);
            seekBar.setProgress(p);
            textTimeCurrent.setText(timeToText(p));
        }
    }

    private void onToStartClick(View view) {
        if (opened) {
            Log.d(TAG, "To Start clicked");
            position = 0;
            if (!paused)
                player.seekTo(0);
            seekBar.setProgress(0);
            textTimeCurrent.setText(timeToText(0));
        }
    }

    private void onToEndClick(View view) {
        if (opened) {
            Log.d(TAG, "To End clicked");
            position = duration - 1;
            if (!paused)
                player.seekTo(duration - 1);
            seekBar.setProgress(duration - 1);
            textTimeCurrent.setText(timeToText(duration - 1));
        }
    }

    private void setPlayEnabled() {
        if (!opened) {
            seekBar.setEnabled(false);
            textTimeCurrent.setEnabled(false);
            textTime.setEnabled(false);
            textTime.setText("");
            buttonPlay.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            buttonStop.setEnabled(false);
            buttonFastRewind.setEnabled(false);
            buttonFastForward.setEnabled(false);
            buttonToStart.setEnabled(false);
            buttonToEnd.setEnabled(false);
        }
        else {
            seekBar.setEnabled(true);
            textTimeCurrent.setEnabled(true);
            textTime.setEnabled(true);
            textTime.setText(timeToText(duration));
            if (!paused)
                buttonPlay.setImageResource(R.drawable.ic_pause_black_24dp);
            else
                buttonPlay.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            buttonStop.setEnabled(true);
            buttonFastRewind.setEnabled(true);
            buttonFastForward.setEnabled(true);
            buttonToStart.setEnabled(true);
            buttonToEnd.setEnabled(true);
        }
    }

    private boolean start(final String path) {
        try {
            Log.d(TAG, "initializes media player");
            player = new MediaPlayer();
            player.setLooping(looping);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer player) {
                    Log.d(TAG, "MediaPlayer.onCompletion()");
                    if (!looping) {
                        player.pause();
                        paused = true;
                        setPlayEnabled();
                    }
                }
            });
            player.setDataSource(path);
            player.prepare();
            duration = player.getDuration();
            if (surfaceView != null) {
                SurfaceHolder holder = surfaceView.getHolder();
                player.setDisplay(null);
                player.setDisplay(holder);
            }

            Log.d(TAG, "initializes SeekBar");
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (player != null && b) {
                        if (!paused)
                            player.seekTo(i);
                        else
                            position = i;
                        textTimeCurrent.setText(timeToText(i));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            seekBar.setMax(duration);
            seekBar.setProgress(0);

            Log.d(TAG, "start playing");
            player.seekTo(position);
            player.start();
            handler.postDelayed(runnable, 0);
            this.path = path;
            this.opened = true;
            return true;
        }
        catch (IOException e) {
            Log.d(TAG, "error, reset opened to false");
            opened = false;
            player = null;
            paused = false;
            duration = 0;
            position = 0;
            return false;
        }
    }

    private static String timeToText(int t) {
        int s = t / 1000;
        int m = s / 60;
        s %= 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

}
