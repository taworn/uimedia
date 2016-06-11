package diy.uimedia;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileInputStream;
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

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-hhmmss-", Locale.US);

    private ImageButton buttonRecord;
    private ImageButton buttonOpen;
    private ImageButton buttonSave;
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
        buttonOpen = (ImageButton) findViewById(R.id.button_open);
        buttonSave = (ImageButton) findViewById(R.id.button_save);
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
                                    .setMessage(R.string.audio_error_dialog_message)
                                    .setNeutralButton(R.string.audio_error_dialog_neutral, null)
                                    .show();
                        }
                        else
                            buttonRecord.setEnabled(false);
                    }
                    else {
                        fragmentMedia.close();
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
                        startActivityForResult(Intent.createChooser(intent, "Select New Audio"), BROWSE_FILE);
                    }
                }
            }
        });

        keepFileName = null;
        recordFileName = null;
        recorder = null;
    }

    @Override
    protected void onDestroy() {
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
                String path = uri.getPath();

                File source = new File(keepFileName);
                File target = new File(path);
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target);
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
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
        if (recorder == null) {
            if (recordStart()) {
                buttonRecord.setImageResource(R.drawable.ic_stop_black_24dp);
                buttonOpen.setEnabled(false);
            }
        }
        else {
            buttonOpen.setEnabled(true);
            buttonRecord.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
            recordStop();
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
                return true;
            }
            else {
                // cannot write file
            }
        }
        catch (IOException e) {
            // error e
        }
        return false;
    }

    private void recordStop() {
        recorder.stop();
        recorder.release();
        recorder = null;

        if (keepFileName != null) {
            File file = new File(keepFileName);
            if (file.exists()) {
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_error_black_24dp)
                        .setMessage("Append file or overwrite?")
                        .setPositiveButton("Append", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                progress = new ProgressDialog(AudioRecorderActivity.this);
                                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                progress.setMessage("Appending files, please wait...");
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
                        .setNeutralButton("Overwrite", new DialogInterface.OnClickListener() {
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
        //List<Track> videoTracks = new LinkedList<Track>();
        for (Movie m : movies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun"))
                    audioTracks.add(t);
                //if (t.getHandler().equals("vide"))
                //videoTracks.add(t);
            }
        }

        Movie result = new Movie();
        if (audioTracks.size() > 0)
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        //if (videoTracks.size() > 0)
        //result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));

        Container out = new DefaultMp4Builder().build(result);
        FileChannel fc = new RandomAccessFile(resultFile, "rw").getChannel();
        out.writeContainer(fc);
        fc.close();
    }

}
