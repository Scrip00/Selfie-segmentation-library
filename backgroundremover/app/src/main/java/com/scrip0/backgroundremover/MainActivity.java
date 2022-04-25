package com.scrip0.backgroundremover;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.madrapps.pikolo.ColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;
import com.scrip0.backremlib.BackActivity;

public class MainActivity extends AppCompatActivity {

    private BackActivity activity;
    public static final int PICK_IMAGE = 1;
    public static final int PICK_VIDEO = 2;

    @SuppressLint({"NewApi", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        activity = new BackActivity(this, viewGroup);

        Button setVideoBtn = findViewById(R.id.setVideoBtn);

        setVideoBtn.setOnClickListener(view -> requestPermissionForGallery(getBaseContext(), true));

        Button setPartyBtn = findViewById(R.id.setPartyBtn);

        setPartyBtn.setOnClickListener(view -> activity.partymode());

        Button setImageBtn = findViewById(R.id.setImageBtn);

        setImageBtn.setOnClickListener(view -> requestPermissionForGallery(getBaseContext(), false));

        Button setColorBtn = findViewById(R.id.setColorBtn);
        final ColorPicker colorPicker = findViewById(R.id.colorPicker);
        colorPicker.setVisibility(View.INVISIBLE);

        setColorBtn.setOnClickListener(view -> {
            if (setColorBtn.getText().equals("Set color")) {
                setColorBtn.setText("Pick color");
                colorPicker.setVisibility(View.VISIBLE);
                colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
                    @Override
                    public void onColorSelected(int color) {
                        activity.setColor(color);
                    }
                });
            } else {
                setColorBtn.setText("Set color");
                colorPicker.setVisibility(View.INVISIBLE);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissionForGallery(Context context, boolean video) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            if (video) {
                pickVideo();

            } else {
                pickImage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_IMAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is needed to continue", Toast.LENGTH_SHORT).show();
            } else {
                pickImage();
            }
        }
        if (requestCode == PICK_VIDEO) {
            if (grantResults.length > 0 &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is needed to continue", Toast.LENGTH_SHORT).show();
            } else {
                pickVideo();
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_VIDEO);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            FileUtils utils = new FileUtils(getBaseContext());
            String currentPhotoPath = utils.getPath(data.getData());
            Bitmap image = BitmapFactory.decodeFile(currentPhotoPath);
            activity.setImageBackground(image, true);
        }
        if (requestCode == PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            FileUtils utils = new FileUtils(getBaseContext());
            activity.setVideo(utils.getPath(data.getData()), true);
        }
        activity.pause();
    }

    @Override
    protected void onResume() {
        activity.resume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        activity.pause();
        super.onPause();
    }
}