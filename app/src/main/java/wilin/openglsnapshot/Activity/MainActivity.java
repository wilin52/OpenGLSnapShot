package wilin.openglsnapshot.Activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Surface;
import android.widget.Button;
import android.widget.ImageView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import wilin.openglsnapshot.GLSurfaceView.GLRenderer;
import wilin.openglsnapshot.GLSurfaceView.SnapshotListener;
import wilin.openglsnapshot.GLSurfaceView.WGLSurfaceView;
import wilin.openglsnapshot.R;

public class MainActivity extends AppCompatActivity implements SnapshotListener, MediaPlayer.OnPreparedListener, EasyPermissions.PermissionCallbacks {

    @BindView(R.id.surfaceView)
    WGLSurfaceView wglSurfaceView;
    @BindView(R.id.snapshot_image)
    ImageView snapshotImage;
    @BindView(R.id.video_operation_btn)
    Button videoBtn;
    private final static int REQUEST_ACCESS_STORAGE_PERMISSION_CODE = 0;
    private final static int REQUEST_ACCESS_SETTING_CODE = 1;
    private final static String videoPath = Environment.getExternalStorageDirectory().getPath() + "/one.mp4";
    private MediaPlayer mediaPlayer;

    private final static int VIDEO_STATE_IDLE = 0;
    private final static int VIDEO_STATE_PLAY = 1;
    private final static int VIDEO_STATE_PAUSE = 2;
    private final static int VIDEO_STATE_STOP = 3;
    private int videoState = VIDEO_STATE_IDLE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initGLSurfaceView();
    }

    private void checkPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            initMediaPlayer();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_storage_requested), R.string.action_allow,
                    R.string.action_deny, REQUEST_ACCESS_STORAGE_PERMISSION_CODE,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void initGLSurfaceView() {
        GLRenderer renderer = new GLRenderer(this);
        wglSurfaceView.setEGLContextClientVersion(2);
        wglSurfaceView.setRenderer(renderer);
        wglSurfaceView.setSnapshotListener(this);
    }

    private void initMediaPlayer() {

        this.mediaPlayer = new MediaPlayer();

        wglSurfaceView.initSurface(new GLRenderer.TextureInitListener() {
            @Override
            public void onSuccess() {
                try {
                    Surface surface = wglSurfaceView.getSurface();
                    mediaPlayer.setDataSource(videoPath);
                    mediaPlayer.setSurface(surface);

                    surface.release();

                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(MainActivity.this);
                    mediaPlayer.setLooping(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure() {
                Log.e("Fail", "Failed");
            }
        });
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            if (mp != null) {
                mp.start();
                videoState = VIDEO_STATE_PLAY;
                videoBtn.setText(R.string.video_pause);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.video_operation_btn)
    void operateVideo() {
        if (mediaPlayer != null) {
            switch (videoState){
                case VIDEO_STATE_IDLE:
                case VIDEO_STATE_PAUSE:
                case VIDEO_STATE_STOP:{
                    mediaPlayer.start();
                    videoState = VIDEO_STATE_PLAY;
                    videoBtn.setText(R.string.video_pause);
                }
                break;
                case VIDEO_STATE_PLAY:{
                    mediaPlayer.pause();
                    videoState = VIDEO_STATE_PAUSE;
                    videoBtn.setText(R.string.video_start);
                }
                break;
            }
        }
    }

    @OnClick(R.id.video_snapshot_btn)
    void snapshot() {
        wglSurfaceView.snapshot();
    }

    @Override
    public void onSnapshot(final Bitmap bitmap) {
        snapshotImage.post(new Runnable() {
            @Override
            public void run() {
                snapshotImage.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        wglSurfaceView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.setting_permission_ask_again))
                    .setPositiveButton(getString(R.string.action_allow))
                    .setNegativeButton(getString(R.string.action_deny), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                        }
                    })
                    .setRequestCode(REQUEST_ACCESS_SETTING_CODE)
                    .build()
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        initMediaPlayer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_ACCESS_SETTING_CODE == requestCode) {
            if (!EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                finish();
            } else {
                initMediaPlayer();
            }
        }

    }
}
