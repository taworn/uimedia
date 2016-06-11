package diy.uimedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

public class AudioPlayerActivity extends AppCompatActivity {

    private static final String TAG = AudioPlayerActivity.class.getSimpleName();

    private static final int BROWSE_FILE = 100;

    private EditText editFile;
    private ImageButton buttonFile;
    private ImageButton buttonOpen;
    private MediaPlayerFragment fragmentMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        editFile = (EditText) findViewById(R.id.edit_file);
        buttonFile = (ImageButton) findViewById(R.id.button_file);
        buttonOpen = (ImageButton) findViewById(R.id.button_open);
        fragmentMedia = (MediaPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_media);

        buttonFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, AudioPlayerActivity.this.getResources().getString(R.string.audio_player_browse)), BROWSE_FILE);
            }
        });
        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!fragmentMedia.isOpened()) {
                    if (!fragmentMedia.open(editFile.getText().toString())) {
                        new AlertDialog.Builder(AudioPlayerActivity.this)
                                .setIcon(R.drawable.ic_error_black_24dp)
                                .setTitle(R.string.audio_error_dialog_title)
                                .setMessage(R.string.audio_error_dialog_message)
                                .setNeutralButton(R.string.audio_error_dialog_neutral, null)
                                .show();
                    }
                    else
                        buttonOpen.setImageResource(R.drawable.ic_stop_black_24dp);
                }
                else {
                    fragmentMedia.close();
                    buttonOpen.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == BROWSE_FILE && resultCode == RESULT_OK) {
            Uri uri = resultIntent.getData();
            editFile.setText(uri.getPath());
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

}
