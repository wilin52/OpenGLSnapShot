attribute vec4 vPosition;
attribute vec4 vTexCoordinate;
uniform mat4 textureTransform;
uniform mat4 uMVPMatrix; //总变换矩阵
varying vec2 v_TexCoordinate;

void main () {
    v_TexCoordinate = (textureTransform * vTexCoordinate).xy;
    gl_Position = uMVPMatrix * vPosition;
}