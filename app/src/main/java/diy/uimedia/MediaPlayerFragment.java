package diy.uimedia;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;

import java.io.IOException;

/**
 * A media player fragment.
 */
public class MediaPlayerFragment extends Fragment {

    private static final String TAG = MediaPlayerFragment.class.getSimpleName();

    private SeekBar seekBar;
    private ImageButton buttonPlay;
    private ImageButton buttonStop;
    private ImageButton buttonFastRewind;
    private ImageButton buttonFastForward;
    private ImageButton buttonToStart;
    private ImageButton buttonToEnd;

    private String path;
    private boolean opened;

    private MediaPlayer player = null;
    private boolean paused = false;
    private int duration = 0;
    private int position = 0;
    private int deltaPosition = 10000;
    private boolean looping = false;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                int p = player.getCurrentPosition();
                if (!paused)
                    seekBar.setProgress(p);
                handler.postDelayed(runnable, 1000);
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
        buttonPlay = (ImageButton) view.findViewById(R.id.button_play);
        buttonStop = (ImageButton) view.findViewById(R.id.button_stop);
        buttonFastRewind = (ImageButton) view.findViewById(R.id.button_fast_rewind);
        buttonFastForward = (ImageButton) view.findViewById(R.id.button_fast_forward);
        buttonToStart = (ImageButton) view.findViewById(R.id.button_to_start);
        buttonToEnd = (ImageButton) view.findViewById(R.id.button_to_end);

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

        path = null;
        opened = false;
        player = null;
        paused = false;
        duration = 0;
        position = 0;
        deltaPosition = 10000;
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
            setPlayEnabled();
        }
    }

    public void onPlayClick(View view) {
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
            setPlayEnabled();
        }
    }

    public void onStopClick(View view) {
        if (opened) {
            Log.d(TAG, "Stop clicked");
            player.stop();
            player.start();
            player.pause();
            paused = true;
            position = 0;
            seekBar.setProgress(0);
            setPlayEnabled();
        }
    }

    public void onFastRewindClick(View view) {
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
        }
    }

    public void onFastForwardClick(View view) {
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
        }
    }

    public void onToStartClick(View view) {
        if (opened) {
            Log.d(TAG, "To Start clicked");
            position = 0;
            if (!paused)
                player.seekTo(0);
            seekBar.setProgress(0);
        }
    }

    public void onToEndClick(View view) {
        if (opened) {
            Log.d(TAG, "To End clicked");
            position = duration - 1;
            if (!paused)
                player.seekTo(duration - 1);
            seekBar.setProgress(duration - 1);
        }
    }

    private void setPlayEnabled() {
        if (!opened) {
            seekBar.setEnabled(false);
            buttonPlay.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            buttonStop.setEnabled(false);
            buttonFastRewind.setEnabled(false);
            buttonFastForward.setEnabled(false);
            buttonToStart.setEnabled(false);
            buttonToEnd.setEnabled(false);
        }
        else {
            seekBar.setEnabled(true);
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
            player = new MediaPlayer();
            player.setLooping(looping);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer player) {
                    Log.d(TAG, "MediaPlayer.onCompletion()");
                }
            });
            player.setDataSource(path);
            player.prepare();

            duration = player.getDuration();
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (player != null && b) {
                        if (!paused)
                            player.seekTo(i);
                        else
                            position = i;
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

            player.seekTo(position);
            player.start();
            handler.postDelayed(runnable, 0);
            this.path = path;
            this.opened = true;
            return true;
        }
        catch (IOException e) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_error_black_24dp)
                    .setTitle(R.string.audio_error_dialog_title)
                    .setMessage(R.string.audio_error_dialog_message)
                    .setNeutralButton(R.string.audio_error_dialog_neutral, null)
                    .show();
            opened = false;
            player = null;
            paused = false;
            duration = 0;
            position = 0;
            return false;
        }
    }

}