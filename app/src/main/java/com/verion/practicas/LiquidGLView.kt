package com.verion.practicas

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
class LiquidGLView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    init {
        setEGLContextClientVersion(2)
        setRenderer(LiquidRenderer())
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private class LiquidRenderer : Renderer {

        private var program = 0
        private lateinit var vertexBuffer: FloatBuffer
        private var startTime = 0L
        private var uTime = -1
        private var uResolution = -1
        private var surfaceW = 1
        private var surfaceH = 1

        private val VERT = """
            attribute vec2 a_position;
            void main() {
              gl_Position = vec4(a_position, 0.0, 1.0);
            }
        """.trimIndent()

        private val FRAG = """
            precision highp float;

            uniform float u_time;
            uniform vec2  u_resolution;

            vec2 hash2(vec2 p) {
              p = vec2(dot(p, vec2(127.1, 311.7)),
                       dot(p, vec2(269.5, 183.3)));
              return -1.0 + 2.0 * fract(sin(p) * 43758.5453);
            }

            float noise(vec2 p) {
              vec2 i = floor(p);
              vec2 f = fract(p);
              vec2 u = f * f * (3.0 - 2.0 * f);
              return mix(
                mix(dot(hash2(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0)),
                    dot(hash2(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
                mix(dot(hash2(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
                    dot(hash2(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x),
                u.y
              );
            }

            float fbm(vec2 p) {
              float val  = 0.0;
              float amp  = 0.5;
              float freq = 1.0;
              for (int i = 0; i < 5; i++) {
                val  += amp  * noise(p * freq);
                freq *= 2.17;
                amp  *= 0.48;
              }
              return val;
            }

            float warpedFbm(vec2 p, float t) {
              float warp = 1.6;
              vec2 q = vec2(
                fbm(p + vec2(0.0, 0.0) + vec2(0.3  * t, 0.0)),
                fbm(p + vec2(5.2, 1.3) + vec2(0.0,  0.2 * t))
              );
              vec2 r = vec2(
                fbm(p + warp * q + vec2(1.7, 9.2) + vec2(0.15 * t, 0.0)),
                fbm(p + warp * q + vec2(8.3, 2.8) + vec2(0.0, 0.12 * t))
              );
              float f = fbm(p + warp * r);
              return clamp(f * 1.5, 0.0, 1.0);
            }

            void main() {
              vec2 uv     = gl_FragCoord.xy / u_resolution;
              float aspect = u_resolution.x / u_resolution.y;
              vec2 p      = (uv - 0.5) * vec2(aspect, 1.0) * 3.0;

              float t = u_time * 0.5 * 0.15;
              float f = warpedFbm(p, t);

              // Colores exactos del portfolio (main.ts)
              vec3 colorA = vec3(0.22, 0.18, 0.75);
              vec3 colorB = vec3(0.03, 0.03, 0.12);
              vec3 colorC = vec3(0.48, 0.28, 0.96);

              vec3 col = mix(colorA, colorB, smoothstep(0.0, 0.6, f));
              col       = mix(col,   colorC, smoothstep(0.4, 0.9, f));

              float vignette = 1.0 - 0.35 * length((uv - 0.5) * 1.8);
              col *= vignette;

              gl_FragColor = vec4(col, 1.0);
            }
        """.trimIndent()


        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            startTime = System.nanoTime()
            val verts = floatArrayOf(-1f, -1f,  1f, -1f, -1f,  1f,
                                     -1f,  1f,  1f, -1f,  1f,  1f)
            vertexBuffer = ByteBuffer.allocateDirect(verts.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
                .apply { put(verts); position(0) }

            program = buildProgram(VERT, FRAG)
            uTime       = GLES20.glGetUniformLocation(program, "u_time")
            uResolution = GLES20.glGetUniformLocation(program, "u_resolution")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            surfaceW = width
            surfaceH = height
        }

        override fun onDrawFrame(gl: GL10?) {
            val t = (System.nanoTime() - startTime) / 1_000_000_000f

            GLES20.glUseProgram(program)

            val posLoc = GLES20.glGetAttribLocation(program, "a_position")
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

            GLES20.glUniform1f(uTime, t)
            GLES20.glUniform2f(uResolution, surfaceW.toFloat(), surfaceH.toFloat())

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        }

        private fun compileShader(type: Int, src: String): Int {
            val id = GLES20.glCreateShader(type)
            GLES20.glShaderSource(id, src)
            GLES20.glCompileShader(id)
            return id
        }

        private fun buildProgram(vertSrc: String, fragSrc: String): Int {
            val prog = GLES20.glCreateProgram()
            GLES20.glAttachShader(prog, compileShader(GLES20.GL_VERTEX_SHADER, vertSrc))
            GLES20.glAttachShader(prog, compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc))
            GLES20.glLinkProgram(prog)
            return prog
        }
    }
}
