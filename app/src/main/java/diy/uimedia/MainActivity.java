package diy.uimedia;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onAboutClick(View view) {
        String message = getResources().getString(R.string.about_dialog_message) + " " + getResources().getString(R.string.version);
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.about_dialog_positive, null)
                .show();
    }

}
