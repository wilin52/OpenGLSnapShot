package wilin.openglsnapshot.GLSurfaceView;

import android.opengl.GLES20;
import android.util.Log;

class RendererHelper {
	
	private final static String TAG = "RendererHelper";

	/**
	 * 生成纹理 create a texture to display
	 * @return 返回纹理 texture
     */
	static int[] createTexture() {
		int[] textures = new int[1];
		GLES20.glGenTextures(textures.length, textures, 0);
		checkGlError("glGenTextures");
		return textures;
	}

	/**
	 * 检查OpenGL 操作是否出错，check that OpenGL operation runs right or not
	 * @param op 操作名字 the operation
	 * @return the result
     */
	static boolean checkGlError(String op) {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			Log.e("OpenL",op + ": glError " + error);
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Helper function to compile a shader.
	 * 
	 * @param shaderType
	 *            The shader type.
	 * @param shaderSource
	 *            The shader source code.
	 * @return An OpenGL handle to the shader.
	 */
	static int compileShader(final int shaderType, final String shaderSource) {
		// 创建着色器
		int shaderHandle = GLES20.glCreateShader(shaderType);

		if (shaderHandle != 0) {
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle);

			// Get the compilation status. 检查状态
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) {
				Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}

		if (shaderHandle == 0) {
			throw new RuntimeException("Error creating shader.");
		}

		return shaderHandle;
	}

	/**
	 * Helper function to compile and link a program.
	 * 
	 * @param vertexShaderHandle
	 *            An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle
	 *            An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes
	 *            Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
	static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle,
			final String[] attributes) {
		// 创建程序
		int programHandle = GLES20.glCreateProgram();

		if (programHandle != 0) {
			// Bind the vertex shader to the program.与着色器连接，不负责编译
			GLES20.glAttachShader(programHandle, vertexShaderHandle);

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			// Bind attributes
			if (attributes != null) {
				final int size = attributes.length;
				for (int i = 0; i < size; i++) {
					GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
				}
			}

			// Link the two shaders together into a program.连接着色器，负责生产最终的可执行程序(生成硬件指令)
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) {
				Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		if (programHandle == 0) {
			throw new RuntimeException("Error creating program.");
		}

		return programHandle;
	}
}
