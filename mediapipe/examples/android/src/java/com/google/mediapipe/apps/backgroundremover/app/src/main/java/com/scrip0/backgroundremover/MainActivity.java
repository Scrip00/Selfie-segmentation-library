package com.scrip0.backgroundremover;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.contextaware.OnContextAvailableListener;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.scrip0.backremlib.BackActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ViewGroup viewGroup;
    private BackActivity activity;
    public static final int PICK_IMAGE = 1;

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewGroup = findViewById(R.id.preview_display_layout);
        activity = new BackActivity(this, viewGroup);

        Button setVideoBtn = findViewById(R.id.setVideoBtn);

        setVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissionForGallery(getBaseContext());
            }
        });

        Button setPartyBtn = findViewById(R.id.setPartyBtn);

        setPartyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                activity.partymode();
                onPause();
            }
        });

        Button setImageBtn = findViewById(R.id.setImageBtn);

        setImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                requestPermissionForGallery(getBaseContext());
                onResume();
            }
        });

        activity.partymode();
//
//
//        activity.setVideo(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Helpme.mp4", true);
//        try {
//            activity.setImageBackground(BitmapFactory.decodeStream(getAssets().open("img.png")), true);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        activity.setColor(Color.BLUE);
//        activity.setImageBackground(null, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissionForGallery(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            openGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_IMAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is needed to continue", Toast.LENGTH_SHORT).show();
            } else {
                openGallery();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            FileUtils utils = new FileUtils(getBaseContext());
            String currentPhotoPath = utils.getPath(data.getData());
            Bitmap image = BitmapFactory.decodeFile(currentPhotoPath);
            Log.d("LOL", String.valueOf(image.getHeight()));
            Log.d("LOL", String.valueOf(image.getHeight()));
        } else {
            Log.d("LOL", "");
        }
        activity.pause();
    }

    @Override
    protected void onResume() {
        activity.resume();
        super.onResume();
        Log.d("LOL", "RESUME");
    }

    @Override
    protected void onPause() {
        activity.pause();
        super.onPause();
        Log.d("LOL", "PAUSE");
    }
}