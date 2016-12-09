package wilin.openglsnapshot.GLSurfaceView;

import android.graphics.Bitmap;

/**
 * snapshot callback
 */
public interface SnapshotListener {
    /**
     * return the snapshot
     * @param bitmap the screenshot
     */
    void onSnapshot(Bitmap bitmap);
}
