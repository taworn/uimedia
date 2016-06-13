package diy.uimedia;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoRecorderActivity extends AppCompatActivity {

    private static final String TAG = VideoRecorderActivity.class.getSimpleName();

    private static final int BROWSE_FILE = 100;
    private static final int RECORD_VIDEO = 101;

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss", Locale.US);

    private ImageButton buttonRecord;
    private ImageButton buttonOpen;
    private ImageButton buttonSave;
    private SurfaceView surfaceView;
    private MediaPlayerFragment fragmentMedia;

    private String keepFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_recorder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        buttonRecord = (ImageButton) findViewById(R.id.button_record);
        buttonOpen = (ImageButton) findViewById(R.id.button_open);
        buttonSave = (ImageButton) findViewById(R.id.button_save);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        fragmentMedia = (MediaPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_media);

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonRecordClick(view);
            }
        });
        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (keepFileName != null) {
                    if (!fragmentMedia.isOpened()) {
                        if (!fragmentMedia.open(keepFileName)) {
                            new AlertDialog.Builder(VideoRecorderActivity.this)
                                    .setIcon(R.drawable.ic_error_black_24dp)
                                    .setTitle(R.string.video_error_dialog_title)
                                    .setMessage(R.string.video_error_dialog_message_play)
                                    .setNeutralButton(R.string.video_error_dialog_neutral, null)
                                    .show();
                        }
                        else {
                            buttonOpen.setImageResource(R.drawable.ic_stop_black_24dp);
                            buttonRecord.setEnabled(false);
                        }
                    }
                    else {
                        fragmentMedia.close();
                        buttonOpen.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                        buttonRecord.setEnabled(true);
                    }
                }
            }
        });
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (keepFileName != null && !fragmentMedia.isOpened()) {
                    File file = new File(keepFileName);
                    if (file.exists()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("video/*");
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        startActivityForResult(Intent.createChooser(intent, VideoRecorderActivity.this.getResources().getString(R.string.video_recorder_browse)), BROWSE_FILE);
                    }
                }
            }
        });

        keepFileName = null;
        SurfaceHolder holder = surfaceView.getHolder();
        fragmentMedia.setHolder(holder);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("opened", false))
                buttonOpen.setImageResource(R.drawable.ic_stop_black_24dp);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("opened", fragmentMedia.isOpened());
    }

    @Override
    protected void onDestroy() {
        if (keepFileName != null) {
            File file = new File(keepFileName);
            if (file.exists())
                file.delete();
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == BROWSE_FILE && resultCode == RESULT_OK) {
            Uri uri = resultIntent.getData();
            final String path = uri.getPath();
            final File file = new File(path);
            if (file.exists()) {
                new AlertDialog.Builder(VideoRecorderActivity.this)
                        .setIcon(R.drawable.ic_warning_black_24dp)
                        .setTitle(R.string.audio_warning_dialog_title)
                        .setMessage(R.string.audio_warning_dialog_message)
                        .setNegativeButton(R.string.audio_warning_dialog_negative, null)
                        .setPositiveButton(R.string.audio_warning_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                File oldFile = new File(keepFileName);
                                oldFile.renameTo(file);
                                fragmentMedia.close();
                                fragmentMedia.open(file.getPath());
                                keepFileName = null;
                            }
                        })
                        .show();
            }
            else {
                File oldFile = new File(keepFileName);
                oldFile.renameTo(file);
                fragmentMedia.close();
                fragmentMedia.open(file.getPath());
                keepFileName = null;
            }
        }
        else if (requestCode == RECORD_VIDEO && resultCode == RESULT_OK) {
            Uri uri = resultIntent.getData();
            keepFileName = uri.getPath();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonRecordClick(View view) {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            File file = File.createTempFile("uimedia-" + formatter.format(new Date()), ".mp4", path);
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, RECORD_VIDEO);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
