package com.uniper.otatest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amlogic.update.OtaUpgradeUtils;

import java.io.File;

import javax.annotation.Nullable;

public class MainActivity extends Activity implements OtaUpgradeUtils.ProgressListener {

    private TextView otaFileFound;
    private LinearLayout status;

    private LayoutInflater inflater;

    private File otaFile;

    private PrefUtils mPref = null;
    private OtaUpgradeUtils updateUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inflater = LayoutInflater.from(this);

        updateUtils = new OtaUpgradeUtils(this);
        mPref = new PrefUtils(this);

        status = findViewById(R.id.status);
        otaFileFound = findViewById(R.id.ota_file);
        otaFileFound.setText(findOtaInDownload());
    }

    private String findOtaInDownload() {
        File downloadDir = new File("storage/emulated/0/Download");

        if (!downloadDir.exists()) {
            return "Download folder missing. Please check path to it";
        }


        File[] files = downloadDir.listFiles();

        if (files == null) {
            return "No download files present. Please put OTA to Download folder";
        }

        for (File file : files) {
            if (file.getName().startsWith("KA2-ota")) {
                otaFile = file;
                findViewById(R.id.update_button).setVisibility(View.VISIBLE);
                return "OTA found: " + file.getAbsolutePath();
            }
        }

        return "OTA file not found. Please put it to Download folder";
    }

    public void onClickUpdate(View view) {
        addMessage("Preparing...");
        updateUtils.registerBattery();
        new Thread(() -> {
            mPref.copyBKFile();
            updateUtils.setDeleteSource(false);
            updateUtils.upgradeDefault(otaFile, MainActivity.this, OtaUpgradeUtils.UPDATE_OTA, false);
        }).start();

    }

    private void addMessage(String message) {
        runOnUiThread(() -> {
            TextView textView = (TextView) inflater.inflate(R.layout.medium_text, null);
            textView.setText(message);
            status.addView(textView);
        });
    }

    @Override
    public void onProgress(int progress) {
        Log.d(TAG, "onProgress() called with: progress = [" + progress + "]");
        if (progress == 0) {
            addMessage("Checking package");
        } else if (progress == 100) {
            addMessage("Package Verified Successfully");
        } else {
            if (progress % 10 == 0) {
                addMessage("Progress: " + progress + "%");
            }
        }
    }

    @Override
    public void onVerifyFailed(int i, Object o) {
        Log.d(TAG, "onVerifyFailed() called with: i = [" + i + "], o = [" + o + "]");
        addMessage("On Verify Failed " + i);
        updateUtils.unregistBattery();
    }

    @Override
    public void onCopyProgress(int progress) {
        Log.d(TAG, "onCopyProgress() called with: progress = [" + progress + "]");
        if (progress == 0) {
            addMessage("Copying...");
        } else if (progress == 100) {
            addMessage("Copying Complete...");
            addMessage("Ready to Restart System...");
        } else {
            if (progress % 10 == 0) {
                addMessage("Copying " + progress + "%");
            }
        }
    }

    @Override
    public void onCopyFailed(int i, Object o) {
        Log.d(TAG, "onCopyFailed() called with: i = [" + i + "], o = [" + o + "]");
        addMessage("On Copy Failed " + i);
        updateUtils.unregistBattery();
    }

    @Override
    public void onStopProgress(int i) {
        Log.d(TAG, "onStopProgress() called with: i = [" + i + "]");
        addMessage("On Stop Progress " + i);
        updateUtils.unregistBattery();
    }

    private static final String TAG = "MainActivity";

}
