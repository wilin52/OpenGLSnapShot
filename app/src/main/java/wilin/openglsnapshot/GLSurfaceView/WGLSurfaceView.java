package wilin.openglsnapshot.GLSurfaceView;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

/**
 * @author LIN WENLONG
 */
public class WGLSurfaceView extends GLSurfaceView {
    private GLRenderer renderer;

    public WGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public WGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        setEGLContextClientVersion(2);
        setRenderer(new GLRenderer(context));
    }

    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
        super.setRenderer(renderer);
    }

    public void setPlaySize(int width, int height) {
        renderer.setWidthHeight(width, height);
        requestRender();
    }

    public void setSnapshotListener(SnapshotListener snapshotListener) {
        renderer.setSnapshotListener(snapshotListener);
    }

    public void initSurface(GLRenderer.TextureInitListener listener){
        renderer.initTexture(listener);
    }

    public void snapshot() {
        if (null != renderer) {
            renderer.snapshot();
        }
    }

    public Surface getSurface(){
        return renderer.getSurface();
    }

    public void onDestroy(){
        renderer.destroy();
    }
}
