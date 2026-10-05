package com.glass.player.design.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.asComposeRenderEffect

const val AGSL_GLASS_SHADER: String = """
    uniform shader content;
    uniform float2 uResolution;
    uniform float2 uTouchPos;
    uniform float uTouchRadius;
    uniform float uRefractionIndex;
    uniform float uChromaticDispersion;
    uniform float uCornerRadius;
    uniform float uSheenAlpha;
    uniform float uSaturation;

    half4 main(float2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) {
            return content.eval(fragCoord);
        }

        float2 center = uResolution * 0.5;
        float2 d = (fragCoord - center) / center;
        float distFromCenter = length(d);

        float edgeFactor = smoothstep(0.65, 1.0, distFromCenter);
        float2 refractionDir = distFromCenter > 0.0001 ? normalize(d) : float2(0.0, 0.0);
        float minDimension = min(uResolution.x, uResolution.y);
        float2 displacement = refractionDir * (edgeFactor * edgeFactor * uRefractionIndex * minDimension);

        float rScale = 1.0 + uChromaticDispersion;
        float bScale = 1.0 - uChromaticDispersion;

        float2 coordR = clamp(fragCoord + displacement * rScale, float2(0.0, 0.0), uResolution);
        float2 coordG = clamp(fragCoord + displacement, float2(0.0, 0.0), uResolution);
        float2 coordB = clamp(fragCoord + displacement * bScale, float2(0.0, 0.0), uResolution);

        half4 colorR = content.eval(coordR);
        half4 colorG = content.eval(coordG);
        half4 colorB = content.eval(coordB);

        half4 baseColor = half4(colorR.r, colorG.g, colorB.b, colorG.a);

        if (uSaturation > 1.0) {
            half luma = dot(baseColor.rgb, half3(0.2126, 0.7152, 0.0722));
            baseColor.rgb = mix(half3(luma), baseColor.rgb, uSaturation);
        }

        float distToTouch = distance(fragCoord, uTouchPos);
        float touchRadius = max(uTouchRadius, 1.0);
        float touchSpecular = exp(-pow(distToTouch / touchRadius, 2.0)) * uSheenAlpha;

        float diagonal = (fragCoord.x + fragCoord.y) / (uResolution.x + uResolution.y);
        float beam = exp(-pow((diagonal - 0.28) / 0.14, 2.0)) * 0.06;

        float bevelGlint = smoothstep(0.92, 1.0, distFromCenter) * 
                           max(0.0, -dot(refractionDir, float2(0.7071, 0.7071))) * 0.12;

        float totalSheen = touchSpecular + beam + bevelGlint;
        baseColor.rgb += half3(totalSheen);

        return baseColor;
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class GlassRuntimeShaderInstance {
    val shader: RuntimeShader = RuntimeShader(AGSL_GLASS_SHADER)

    fun updateUniforms(
        resolution: Size,
        touchPos: Offset,
        touchRadius: Float,
        refractionIndex: Float = 0.045f,
        chromaticDispersion: Float = 0.018f,
        cornerRadius: Float = 22f,
        sheenAlpha: Float = 0.22f,
        saturation: Float = 1.6f
    ) {
        val safeWidth = resolution.width.coerceAtLeast(1f)
        val safeHeight = resolution.height.coerceAtLeast(1f)
        shader.setFloatUniform("uResolution", safeWidth, safeHeight)

        val posX = if (touchPos.isSpecified) touchPos.x else safeWidth * 0.25f
        val posY = if (touchPos.isSpecified) touchPos.y else safeHeight * 0.25f
        shader.setFloatUniform("uTouchPos", posX, posY)
        shader.setFloatUniform("uTouchRadius", touchRadius.coerceAtLeast(10f))
        shader.setFloatUniform("uRefractionIndex", refractionIndex)
        shader.setFloatUniform("uChromaticDispersion", chromaticDispersion)
        shader.setFloatUniform("uCornerRadius", cornerRadius)
        shader.setFloatUniform("uSheenAlpha", sheenAlpha)
        shader.setFloatUniform("uSaturation", saturation)
    }

    fun toComposeRenderEffect(): androidx.compose.ui.graphics.RenderEffect {
        val androidEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
        return androidEffect.asComposeRenderEffect()
    }
}

fun isAgslGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
fun rememberGlassRuntimeShader(): GlassRuntimeShaderInstance? {
    return if (isAgslGlassSupported()) {
        remember { try { GlassRuntimeShaderInstance() } catch (t: Throwable) { null } }
    } else {
        null
    }
}
