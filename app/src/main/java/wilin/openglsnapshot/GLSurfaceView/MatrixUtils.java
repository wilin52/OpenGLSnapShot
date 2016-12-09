package wilin.openglsnapshot.GLSurfaceView;

import android.opengl.Matrix;

public class MatrixUtils {

    private static float[] mProjMatrix = new float[16];// 4x4矩阵 投影用
    private static float[] mVMatrix = new float[16];// 摄像机位置朝向9参数矩阵
    private static float[] mMVPMatrix;// 最后起作用的总变换矩阵
    private static float[] mMMatrix = new float[16];// 具体物体的移动旋转矩阵

    private static final  float[] fullScreenShotMatrix = new float[16];

    // 保护变换矩阵的栈
    static float[][] mStack = new float[10][16];
    static int stackTop = -1;

    // 获取不变换初始矩阵
    public static void setInitStack() {
        Matrix.setRotateM(mMMatrix, 0, 0, 1, 0, 0);
        Matrix.setRotateM(fullScreenShotMatrix, 0, 0, 1, 0, 0);
        stackTop = -1;
    }

    // 保护变换矩阵
    public static void pushMatrix() {
        stackTop++;
        for (int i = 0; i < 16; i++) {
            mStack[stackTop][i] = mMMatrix[i];
        }
    }

    // 恢复变换矩阵
    public static void popMatrix() {
        for (int i = 0; i < 16; i++) {
            mMMatrix[i] = mStack[stackTop][i];
        }
        stackTop--;
    }

    // 设置沿xyz轴移动
    public static void translate(float x, float y, float z) {
        Matrix.translateM(mMMatrix, 0, x, y, z);
    }

    // 设置绕xyz轴转动
    public static void rotate(float angle, float x, float y, float z) {
        Matrix.rotateM(mMMatrix, 0, angle, x, y, z);
    }

    public static void scale(float x, float y, float z) {
        Matrix.scaleM(mMMatrix, 0, x, y, z);
    }

    /**
     * 设置摄像机
     *
     * @param cx
     *            摄像机位置x
     * @param cy
     *            摄像机位置y
     * @param cz
     *            摄像机位置z
     * @param tx
     *            摄像机目标点x
     * @param ty
     *            摄像机目标点y
     * @param tz
     *            摄像机目标点z
     * @param upx
     *            摄像机UP向量X分量
     * @param upy
     *            摄像机UP向量Y分量
     * @param upz
     *            摄像机UP向量Z分量
     */
    public static void setCamera(float cx, float cy, float cz, float tx, float ty, float tz, float upx, float upy,
                                 float upz) {
        Matrix.setLookAtM(mVMatrix, 0, cx, cy, cz, tx, ty, tz, upx, upy, upz);
    }

    /**
     * 设置透视投影参数
     *
     * @param left
     *            near面的left
     * @param right
     *            near面的right
     * @param bottom
     *            near面的bottom
     * @param top
     *            near面的top
     * @param near
     *            near面距离
     * @param far
     *            far面距离
     */
    public static void setProjectFrustum(float left, float right, float bottom, float top, float near, float far) {
        Matrix.frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
    }

    // 获取具体物体的总变换矩阵
    public static float[] getFinalMatrix() {
        mMVPMatrix = new float[16];
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }
}
