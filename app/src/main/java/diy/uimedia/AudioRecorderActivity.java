package diy.uimedia;

import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

public class AudioRecorderActivity extends AppCompatActivity {

    private static final String TAG = AudioRecorderActivity.class.getSimpleName();

    private MediaRecorder recorder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

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
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
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
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

            File fileA = new File(path, "A.m4a");
            File fileB = new File(path, "B.m4a");
            File fileC = new File(path, "C.m4a");

            String[] files = {fileA.getPath(), fileB.getPath()};
            appendFiles(fileC.getPath(), files);

            File file = File.createTempFile("uimedia-", ".tmp", path);
            if (file.canWrite()) {
                String fileName = file.getPath();
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setOutputFile(fileName);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.prepare();
            }
        }
        catch (IOException e) {
            Log.d(TAG, String.format("prepare() failed, reason: %s", e.getMessage()));
        }
        recorder.start();
    }

    public void onButtonStopClick(View view) {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
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
