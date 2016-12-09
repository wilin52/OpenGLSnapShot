package wilin.openglsnapshot.GLSurfaceView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import wilin.openglsnapshot.R;


public class GLRenderer implements GLSurfaceView.Renderer{
    private int width;
    private int height;
    private static final int FLOAT_SIZE_BYTES = 4;
    //坐标
    private FloatBuffer vertexBuffer;
    private final float[] squareCoords = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0,//
            1.0f, -1.0f, 0, //
            -1.0f, 1.0f, 0, //
            1.0f, 1.0f, 0, //
    };

    /**
     * 纹理坐标
     **/
    private FloatBuffer textureBuffer;
    private final float textureCoords[] = {0.f, 0.f, 1.f, 0.f, 0.f, 1.f, 1.f, 1.f};

    /**
     * 纹理绘制顺序
     **/
    private final short[] pointOrder = {0, 1, 2, 3};
    private ShortBuffer pointOrderBuffer;

    private int shaderProgram;
    private int textureCoordinateHandle;
    private int positionHandle;
    private int textureTransformHandle;
    private int muMVPMatrixHandle;

    /**
     * 纹理标识
     **/
    private int[] textureId;
    private boolean textureInited = false;
    // 利用SurfaceTexture 的缓冲区，对帧数据进行处理，并输出给Surface
    private SurfaceTexture mSurfaceTexture;
    // 显示视图
    private Surface mSurface;
    /**
     * 视图矩阵
     **/
    private float[] videoTextureTransform;
    private Context mContext;
    private boolean isSnap;
    private boolean surfaceTextureValid;
    // 帧缓存，用于截图数据读取
    private int framebuffer;
    private SnapshotListener listener;

    public GLRenderer(Context context) {
        this.mContext = new WeakReference<>(context).get();
        this.videoTextureTransform = new float[16];
    }

    void setWidthHeight(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        /**初始化视图矩阵**/
        MatrixUtils.setInitStack();
        /**初始化顶点和纹理信息**/
        initVertexData();
        /**初始化绘图程序**/
        initShader();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        // 设置视窗大小及位置
        GLES20.glViewport(0, 0, width, height);
        // 计算GLSurfaceView的宽高比
        float ratio = (float) width / height;
        initFrameBuffer();
        // 调用此方法计算产生透视投影矩阵
        MatrixUtils.setProjectFrustum(-ratio, ratio, -1, 1, 80, 90);
        // 调用此方法产生摄像机9参数位置矩阵
        MatrixUtils.setCamera(0, 0, 80, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 清除深度缓冲与颜色缓冲
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        MatrixUtils.pushMatrix();
        // 纹理初始化

        if (!surfaceTextureValid) {
            surfaceTextureValid = RendererHelper.checkGlError("before updateTexImage");
        }

        if (textureInited  && surfaceTextureValid) {
            float ratio = (float) width / height;
            MatrixUtils.scale(ratio, 1, 1);
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(videoTextureTransform);
            drawTexture(textureId[0]);
            // 进行截图
            if (isSnap) {
                isSnap = false;
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
                // 需要将数据传入framebuffer
                drawTexture(textureId[0]);
                saveScreenShot(listener);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }

        GLES20.glFinish();
        MatrixUtils.popMatrix();
    }

    /**
     * 进行绘制纹理
     *
     * @param textureId      与数据绑定的纹理
     */
    private void drawTexture(int textureId) {
        // Draw texture
        GLES20.glUseProgram(shaderProgram);

        GLES20.glUseProgram(shaderProgram);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_BLEND);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glEnableVertexAttribArray(positionHandle);// 将顶点数据传递至OpenGL
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, MatrixUtils.getFinalMatrix(), 0);
        GLES20.glUniformMatrix4fv(textureTransformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // 正式画图
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, pointOrder.length, GLES20.GL_UNSIGNED_SHORT, pointOrderBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
        GLES20.glUseProgram(0);
    }

    /***
     * 截图
     */
    private void saveScreenShot(final SnapshotListener listener) {

        final int bitmapBuffer[] = new int[width * height];
        final int bitmapSource[] = new int[width * height];

        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
        // 需要对渲染数据进行处理，图像倒置
        new Thread(new Runnable() {

            @Override
            public void run() {

                int offset1, offset2;
                for (int i = 0; i < height; i++) {
                    offset1 = i * width;
                    offset2 = (height - i - 1) * width;
                    for (int j = 0; j < width; j++) {
                        int texturePixel = bitmapBuffer[offset1 + j];
                        int blue = (texturePixel >> 16) & 0xff;
                        int red = (texturePixel << 16) & 0x00ff0000;
                        int pixel = (texturePixel & 0xff00ff00) | red | blue;
                        bitmapSource[offset2 + j] = pixel;
                    }
                }
                Bitmap bitmap = Bitmap.createBitmap(bitmapSource, width, height, Bitmap.Config.ARGB_8888);
                listener.onSnapshot(bitmap);
            }
        }).start();

    }

    /**
     * 初始化着色器
     */
    private void initShader() {
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.vetext_sharder);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.fragment_sharder);

        final int vertexShaderHandle = RendererHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = RendererHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        shaderProgram = RendererHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[]{
                "texture", "vPosition", "vTexCoordinate", "textureTransform"});

        GLES20.glUseProgram(shaderProgram);
        textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        textureTransformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");
        // 获取程序中总变换矩阵引用id
        muMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
    }


    /**
     * 初始化绘图顶点和纹理顶点
     */
    private void initVertexData() {
        vertexBuffer = ByteBuffer.allocateDirect(squareCoords.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(squareCoords).position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureCoords.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(textureCoords).position(0);

        pointOrderBuffer = ByteBuffer.allocateDirect(pointOrder.length * 2).order(ByteOrder.nativeOrder())
                .asShortBuffer();
        pointOrderBuffer.put(pointOrder).position(0);

        Matrix.setIdentityM(videoTextureTransform, 0);
    }

    /**
     * 初始化帧缓存
     */
    private void initFrameBuffer() {
        int[] mFrameBuffer = new int[1];
        int[] mFrameBufferTexture = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES20.glGenTextures(1, mFrameBufferTexture, 0);
        framebuffer = mFrameBuffer[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTexture[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFrameBufferTexture[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /**
     * 初始化纹理
     */
    void initTexture(TextureInitListener listener) {
        // 生成纹理
        textureId = RendererHelper.createTexture();
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);

        if (!RendererHelper.checkGlError("glBindTexture mTextureID")) {
            if (null != listener) {
                listener.onFailure();
            }
            return;
        }
        // http://blog.csdn.net/liyuanjinglyj/article/details/46660603
        //GL_TEXTURE_MIN_FILTER 缩小过滤，GL_TEXTURE_MAG_FILTER（放大功能）给GL_LINEAR，确保图片是平滑的在它被拉伸的时候。
        // 最近的纹理，在缩小时锯齿比线性过滤更不明显，而在放大时，线性过滤能有效地解决锯齿现象
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 设置S/T轴或者称U/V轴
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        this.mSurfaceTexture = new SurfaceTexture(this.textureId[0]);
        // 声明一个Surface来显示图像，与SurfaceTexture进行关联
        this.mSurface = new Surface(this.mSurfaceTexture);
        if (null != listener) {
            listener.onSuccess();
        }
        textureInited = true;
    }

    /**
     * capture the screen
     */
    void snapshot() {
        this.isSnap = true;
    }

    void setSnapshotListener(SnapshotListener listener) {
        this.listener = listener;
    }

    public interface TextureInitListener {
        void onSuccess();

        void onFailure();
    }

    public Surface getSurface() {
        return mSurface;
    }

    void destroy() {
        textureInited = false;

        if (null != mSurfaceTexture) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (null != mSurface) {
            mSurface.release();
        }

        if (textureId != null) {  // 删除纹理
            GLES20.glDeleteTextures(1, textureId, 0);
        }

        listener = null;

    }
}
