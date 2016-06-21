package diy.uimedia;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class AudioRecorderActivity extends AppCompatActivity {

    private static final String TAG = AudioRecorderActivity.class.getSimpleName();

    private static final int BROWSE_FILE = 100;
    private static final int RECORD_AUDIO = 101;

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

    private ImageButton buttonRecord;
    private CheckBox checkIntent;
    private ImageButton buttonOpen;
    private ImageButton buttonSave;
    private TextView textHint;
    private MediaPlayerFragment fragmentMedia;

    private String keepFileName;
    private String recordFileName;

    private MediaRecorder recorder = null;
    private ProgressDialog progress = null;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        buttonRecord = (ImageButton) findViewById(R.id.button_record);
        checkIntent = (CheckBox) findViewById(R.id.check_intent);
        buttonOpen = (ImageButton) findViewById(R.id.button_open);
        buttonSave = (ImageButton) findViewById(R.id.button_save);
        textHint = (TextView) findViewById(R.id.text_hint);
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
                            new AlertDialog.Builder(AudioRecorderActivity.this)
                                    .setIcon(R.drawable.ic_error_black_24dp)
                                    .setTitle(R.string.audio_error_dialog_title)
                                    .setMessage(R.string.audio_error_dialog_message_play)
                                    .setNeutralButton(R.string.audio_error_dialog_neutral, null)
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
                if (keepFileName != null) {
                    File file = new File(keepFileName);
                    if (file.exists()) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("audio/*");
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        startActivityForResult(Intent.createChooser(intent, AudioRecorderActivity.this.getResources().getString(R.string.audio_recorder_browse)), BROWSE_FILE);
                    }
                }
            }
        });
        textHint.setVisibility(View.INVISIBLE);

        keepFileName = null;
        recordFileName = null;
        recorder = null;

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
        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
        if (keepFileName != null) {
            File file = new File(keepFileName);
            if (file.exists())
                file.delete();
        }
        if (recordFileName != null) {
            File file = new File(recordFileName);
            if (file.exists())
                file.delete();
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == BROWSE_FILE && resultCode == RESULT_OK) {
            try {
                Uri uri = resultIntent.getData();
                final String path = uri.getPath();
                File file = new File(path);
                if (file.exists()) {
                    new AlertDialog.Builder(AudioRecorderActivity.this)
                            .setIcon(R.drawable.ic_warning_black_24dp)
                            .setTitle(R.string.audio_warning_dialog_title)
                            .setMessage(R.string.audio_warning_dialog_message)
                            .setNegativeButton(R.string.audio_warning_dialog_negative, null)
                            .setPositiveButton(R.string.audio_warning_dialog_positive, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        copyFile(path, keepFileName);
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .show();
                }
                else {
                    copyFile(path, keepFileName);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (requestCode == RECORD_AUDIO && resultCode == RESULT_OK) {
            Uri uri = resultIntent.getData();
            ContentResolver resolver = getContentResolver();
            try {
                String[] columns = {MediaStore.Audio.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, columns, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(columns[0]);
                String audioPath = cursor.getString(columnIndex);
                cursor.close();

                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                File file = File.createTempFile("uimedia-" + formatter.format(new Date()), ".tmp", path);
                File sourceFile = new File(audioPath);
                sourceFile.renameTo(file);
                recordFileName = file.getPath();
                afterRecordStop();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
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
        checkIntent.setEnabled(false);
        if (!checkIntent.isChecked()) {
            if (recorder == null) {
                if (recordStart()) {
                    buttonRecord.setImageResource(R.drawable.ic_stop_black_24dp);
                    buttonOpen.setEnabled(false);
                }
                else {
                    new AlertDialog.Builder(AudioRecorderActivity.this)
                            .setIcon(R.drawable.ic_error_black_24dp)
                            .setTitle(R.string.audio_error_dialog_title)
                            .setMessage(R.string.audio_error_dialog_message_record)
                            .setNeutralButton(R.string.audio_error_dialog_neutral, null)
                            .show();
                }
            }
            else {
                buttonOpen.setEnabled(true);
                buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
                recordStop();
            }
        }
        else {
            Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(intent, RECORD_AUDIO);
        }
    }

    private boolean recordStart() {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File file = File.createTempFile("uimedia-" + formatter.format(new Date()), ".tmp", path);
            if (file.canWrite()) {
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setOutputFile(file.getPath());
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.prepare();
                recorder.start();
                recordFileName = file.getPath();
                textHint.setVisibility(View.VISIBLE);
                return true;
            }
        }
        catch (IOException e) {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        }
        return false;
    }

    private void recordStop() {
        recorder.stop();
        recorder.release();
        recorder = null;
        textHint.setVisibility(View.INVISIBLE);
        afterRecordStop();
    }

    private void afterRecordStop() {
        if (keepFileName != null) {
            File file = new File(keepFileName);
            if (file.exists()) {
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_warning_black_24dp)
                        .setMessage(R.string.audio_question_dialog_message)
                        .setPositiveButton(R.string.audio_question_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                progress = new ProgressDialog(AudioRecorderActivity.this);
                                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                progress.setMessage(getResources().getString(R.string.audio_waiting));
                                progress.setIndeterminate(true);
                                progress.setCancelable(false);
                                progress.setCanceledOnTouchOutside(false);
                                progress.show();
                                final String[] files = {keepFileName, recordFileName};
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            appendFiles(keepFileName + ".tmp", files);
                                            new File(recordFileName).delete();
                                            File targetFile = new File(keepFileName);
                                            targetFile.delete();
                                            File sourceFile = new File(keepFileName + ".tmp");
                                            sourceFile.renameTo(targetFile);
                                        }
                                        catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        progress.cancel();
                                    }
                                };
                                handler.postDelayed(runnable, 0);
                            }
                        })
                        .setNeutralButton(R.string.audio_question_dialog_neutral, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new File(keepFileName).delete();
                                keepFileName = recordFileName;
                            }
                        })
                        .show();
            }
            else
                keepFileName = recordFileName;
        }
        else
            keepFileName = recordFileName;
    }

    private void appendFiles(String resultFile, String[] files) throws IOException {
        Movie[] movies = new Movie[files.length];
        int index = 0;
        for (String video : files) {
            movies[index] = MovieCreator.build(video);
            index++;
        }
        List<Track> audioTracks = new LinkedList<Track>();
        for (Movie m : movies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun"))
                    audioTracks.add(t);
            }
        }

        Movie result = new Movie();
        if (audioTracks.size() > 0)
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));

        Container out = new DefaultMp4Builder().build(result);
        FileChannel fc = new RandomAccessFile(resultFile, "rw").getChannel();
        out.writeContainer(fc);
        fc.close();
    }

    private void copyFile(String target, String source) throws IOException {
        File sourceFile = new File(source);
        File targetFile = new File(target);
        InputStream sourceStream = new FileInputStream(sourceFile);
        OutputStream targetStream = new FileOutputStream(targetFile);
        byte[] buffer = new byte[4096];
        int length;
        while ((length = sourceStream.read(buffer)) > 0) {
            targetStream.write(buffer, 0, length);
        }
        sourceStream.close();
        targetStream.close();
    }

}
