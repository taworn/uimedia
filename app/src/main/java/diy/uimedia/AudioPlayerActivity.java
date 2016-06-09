package diy.uimedia;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.IOException;

public class AudioPlayerActivity extends AppCompatActivity {

    private static final String TAG = AudioPlayerActivity.class.getSimpleName();

    private static final int BROWSE_FILE = 100;

    private EditText editFile;
    private ImageButton buttonFile;
    private ImageButton buttonPlay;
    private ImageButton buttonStop;
    private ImageButton buttonFastRewind;
    private ImageButton buttonFastForward;

    private boolean played = false;
    private boolean paused = false;
    private int pauseLength = 0;
    private MediaPlayer mediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editFile = (EditText) findViewById(R.id.edit_file);
        buttonFile = (ImageButton) findViewById(R.id.button_file);
        buttonPlay = (ImageButton) findViewById(R.id.button_play);
        buttonStop = (ImageButton) findViewById(R.id.button_stop);
        buttonFastRewind = (ImageButton) findViewById(R.id.button_fast_rewind);
        buttonFastForward = (ImageButton) findViewById(R.id.button_fast_forward);

        buttonFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Select New Audio"), BROWSE_FILE);
            }
        });

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate without savedInstanceState");
        }
        else {
            editFile.setText(savedInstanceState.getString("editFileText"));
            played = savedInstanceState.getBoolean("played");
            paused = savedInstanceState.getBoolean("paused");
            pauseLength = savedInstanceState.getInt("pauseLength");
            if (!played) {
                Log.d(TAG, "onCreate with savedInstanceState, played == false");
            }
            else {
                Log.d(TAG, "onCreate with savedInstanceState, played == true");
                if (startMediaPlayer()) {
                    mediaPlayer.seekTo(pauseLength);
                    if (paused)
                        mediaPlayer.pause();
                }
            }
        }
        setPlayEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("editFileText", editFile.getText().toString());
        savedInstanceState.putBoolean("played", played);
        savedInstanceState.putBoolean("paused", paused);
        savedInstanceState.putInt("pauseLength", played ? mediaPlayer.getCurrentPosition() : 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == BROWSE_FILE && resultCode == RESULT_OK) {
            Uri uri = resultIntent.getData();
            editFile.setText(uri.getPath());
        }
    }

    public void onPlayClick(View view) {
        if (!played) {
            Log.d(TAG, "Play clicked, startMediaPlayer()");
            if (startMediaPlayer()) {
                played = true;
                paused = false;
                setPlayEnabled();
            }
        }
        else {
            if (!paused) {
                Log.d(TAG, "Play clicked, !paused, set to paused");
                mediaPlayer.pause();
                pauseLength = mediaPlayer.getCurrentPosition();
                paused = true;
                setPlayEnabled();
            }
            else {
                Log.d(TAG, "Play clicked, paused, set to !paused");
                mediaPlayer.seekTo(pauseLength);
                mediaPlayer.start();
                paused = false;
                setPlayEnabled();
            }
        }
    }

    public void onStopClick(View view) {
        Log.d(TAG, "Stop clicked");
        if (played) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            played = false;
            setPlayEnabled();
        }
    }

    public void onFastRewindClick(View view) {
        Log.d(TAG, "Fast Rewind clicked");
    }

    public void onFastForwardClick(View view) {
        Log.d(TAG, "Fast Forward clicked");
    }

    private void setPlayEnabled() {
        if (!played) {
            editFile.setEnabled(true);
            buttonFile.setEnabled(true);
            buttonStop.setEnabled(false);
            buttonFastRewind.setEnabled(false);
            buttonFastForward.setEnabled(false);
            buttonPlay.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }
        else {
            editFile.setEnabled(false);
            buttonFile.setEnabled(false);
            buttonStop.setEnabled(true);
            buttonFastRewind.setEnabled(true);
            buttonFastForward.setEnabled(true);
            if (!paused)
                buttonPlay.setImageResource(R.drawable.ic_pause_black_24dp);
            else
                buttonPlay.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }
    }

    private boolean startMediaPlayer() {
        if (!editFile.getText().toString().equals("")) {
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(editFile.getText().toString());
                mediaPlayer.prepare();
                mediaPlayer.start();
                return true;
            }
            catch (IOException e) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.audio_error_dialog_title)
                        .setMessage(R.string.audio_error_dialog_message)
                        .setNeutralButton(R.string.audio_error_dialog_neutral, null)
                        .show();
                mediaPlayer = null;
            }
        }
        return false;
    }

}
