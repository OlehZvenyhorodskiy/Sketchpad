package com.example.academic

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

object OilPaintShader {

    val AGSL_OIL_PAINT_SRC = """
        uniform shader sTexture;
        uniform float2 iResolution;
        uniform float iTime;

        float hash(float2 p) {
            return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution;
            half4 col = sTexture.eval(fragCoord);
            if (col.a < 0.05) return col;

            float noise = hash(fragCoord * 0.05 + iTime * 0.1);
            float2 offset = (float2(noise, hash(fragCoord.yx * 0.05)) - 0.5) * 4.0;
            half4 blended = sTexture.eval(fragCoord + offset);

            return lerp(col, blended, 0.4);
        }
    """.trimIndent()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createOilPaintEffect(width: Float, height: Float, time: Float): androidx.compose.ui.graphics.RenderEffect? {
        return try {
            val shader = RuntimeShader(AGSL_OIL_PAINT_SRC)
            shader.setFloatUniform("iResolution", width, height)
            shader.setFloatUniform("iTime", time)
            RenderEffect.createRuntimeShaderEffect(shader, "sTexture").asComposeRenderEffect()
        } catch (e: Exception) {
            null
        }
    }
}
