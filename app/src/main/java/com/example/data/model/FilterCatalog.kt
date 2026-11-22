package com.example.data.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DistortionType {
    NONE,
    BIG_HEAD,
    SMALL_FACE,
    FUNNY_EYES,
    DOG_FACE,
    CAT_FACE,
    CARTOON_POP,
    ANIME_EDGE,
    NEON_GLOW,
    CHROMA_3D,
    RAIN_DROPS,
    FOG_MIST,
    BOKEH_LIGHT,
    LENS_FLARE,
    BLUR_DREAM,
    FESTIVAL_DIWALI,
    INDIAN_TRADITIONAL,
    WEDDING_ROYAL
}

data class FilterCategoryMeta(
    val id: String,
    val title: String,
    val emoji: String
)

data class SnapFilter(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryTitle: String,
    val saturation: Float,
    val contrast: Float,
    val brightnessOffset: Float,
    val warmth: Float,
    val tintMagenta: Float,
    val hueDegrees: Float,
    val sepiaAmount: Float,
    val overlayColorArgb: Long,
    val overlayAlpha: Float,
    val vignetteStrength: Float,
    val glowIntensity: Float,
    val blurStrength: Float,
    val distortionType: DistortionType,
    val accentColorArgb: Long,
    val trending: Boolean = false
) {
    fun buildColorMatrix(extraBeauty: Float = 0f, extraBrightness: Float = 0f): FloatArray {
        val cm = ColorMatrix()

        // 1. Hue rotation
        if (hueDegrees != 0f) {
            val rad = hueDegrees * PI.toFloat() / 180f
            val cosVal = cos(rad.toDouble()).toFloat()
            val sinVal = sin(rad.toDouble()).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            val hueMat = ColorMatrix(
                floatArrayOf(
                    lumR + cosVal * (1f - lumR) + sinVal * (-lumR),
                    lumG + cosVal * (-lumG) + sinVal * (-lumG),
                    lumB + cosVal * (-lumB) + sinVal * (1f - lumB),
                    0f, 0f,

                    lumR + cosVal * (-lumR) + sinVal * 0.143f,
                    lumG + cosVal * (1f - lumG) + sinVal * 0.140f,
                    lumB + cosVal * (-lumB) + sinVal * (-0.283f),
                    0f, 0f,

                    lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)),
                    lumG + cosVal * (-lumG) + sinVal * lumG,
                    lumB + cosVal * (1f - lumB) + sinVal * lumB,
                    0f, 0f,

                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(hueMat)
        }

        // 2. Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation((saturation + extraBeauty * 0.12f).coerceIn(0f, 2.5f))
        cm.postConcat(satMatrix)

        // 3. Sepia blend
        if (sepiaAmount > 0.01f) {
            val s = sepiaAmount.coerceIn(0f, 1f)
            val inv = 1f - s
            val sepia = ColorMatrix(
                floatArrayOf(
                    inv + s * 0.393f, s * 0.769f, s * 0.189f, 0f, 0f,
                    s * 0.349f, inv + s * 0.686f, s * 0.168f, 0f, 0f,
                    s * 0.272f, s * 0.534f, inv + s * 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(sepia)
        }

        // 4. Contrast, warmth, tint, brightness
        val c = (contrast - extraBeauty * 0.08f).coerceIn(0.45f, 1.95f)
        val t = (1f - c) * 128f
        val b = brightnessOffset + extraBrightness * 70f + extraBeauty * 14f
        val rScale = c * (1f + warmth * 0.22f)
        val gScale = c * (1f - tintMagenta * 0.12f)
        val bScale = c * (1f - warmth * 0.22f)

        val toneMat = ColorMatrix(
            floatArrayOf(
                rScale, 0f, 0f, 0f, t + b,
                0f, gScale, 0f, 0f, t + b,
                0f, 0f, bScale, 0f, t + b,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(toneMat)
        return cm.array
    }
}

object FilterCatalog {
    val categories: List<FilterCategoryMeta> = listOf(
        FilterCategoryMeta("trending", "Trending Filters", "🔥"),
        FilterCategoryMeta("funny", "Funny Filters", "🤪"),
        FilterCategoryMeta("distortion", "Face Distortion", "🌀"),
        FilterCategoryMeta("big_head", "Big Head Filters", "🎈"),
        FilterCategoryMeta("small_face", "Small Face Filters", "🔎"),
        FilterCategoryMeta("funny_eyes", "Funny Eyes", "👀"),
        FilterCategoryMeta("animal", "Animal Face Filters", "🐾"),
        FilterCategoryMeta("dog", "Dog Filters", "🐶"),
        FilterCategoryMeta("cat", "Cat Filters", "🐱"),
        FilterCategoryMeta("cartoon", "Cartoon Filters", "🎨"),
        FilterCategoryMeta("anime", "Anime Filters", "⚡"),
        FilterCategoryMeta("glow", "Glow Filters", "✨"),
        FilterCategoryMeta("neon", "Neon Glow", "🌈"),
        FilterCategoryMeta("hdr", "HDR Filters", "📸"),
        FilterCategoryMeta("iphone_look", "iPhone Camera Look", "📱"),
        FilterCategoryMeta("iphone_hdr", "iPhone HDR Look", "💎"),
        FilterCategoryMeta("dslr", "DSLR Look", "📷"),
        FilterCategoryMeta("cinematic", "Cinematic Filters", "🎬"),
        FilterCategoryCategory("portrait", "Portrait Filters", "🧑"),
        FilterCategoryMeta("vintage", "Vintage Filters", "🎞️"),
        FilterCategoryMeta("retro", "Retro Filters", "📼"),
        FilterCategoryMeta("bw", "Black & White", "🖤"),
        FilterCategoryMeta("golden_hour", "Golden Hour", "☀️"),
        FilterCategoryMeta("sunset", "Sunset Filters", "🌇"),
        FilterCategoryMeta("sunrise", "Sunrise Filters", "🌅"),
        FilterCategoryMeta("night", "Night Mode Filters", "🌃"),
        FilterCategoryMeta("moonlight", "Moonlight Filters", "🌙"),
        FilterCategoryMeta("rainy", "Rainy Filters", "🌧️"),
        FilterCategoryMeta("fog", "Fog Filters", "🌫️"),
        FilterCategoryMeta("warm", "Warm Tone", "🔥"),
        FilterCategoryMeta("cool", "Cool Tone", "❄️"),
        FilterCategoryMeta("blue", "Blue Tone", "💙"),
        FilterCategoryMeta("orange_teal", "Orange & Teal", "🧡"),
        FilterCategoryMeta("beauty", "Natural Beauty Filters", "🌸"),
        FilterCategoryMeta("soft_skin", "Soft Skin Filters", "🪷"),
        FilterCategoryMeta("light_fx", "Light Effects", "💫"),
        FilterCategoryMeta("lens_fx", "Lens Effects", "🔮"),
        FilterCategoryMeta("blur_fx", "Blur Effects", "💧"),
        FilterCategoryMeta("bg_fx", "Background Effects", "🖼️"),
        FilterCategoryMeta("three_d", "3D Effects", "🧊"),
        FilterCategoryMeta("festival", "Festival Filters", "🪔"),
        FilterCategoryMeta("indian_trad", "Indian Traditional Filters", "🪷"),
        FilterCategoryMeta("wedding", "Wedding Filters", "👑")
    )

    private fun FilterCategoryCategory(id: String, title: String, emoji: String) =
        FilterCategoryMeta(id, title, emoji)

    private val styleDescriptors = listOf(
        "Classic", "Studio", "Pro", "Ultra", "Vivid",
        "Soft", "Crisp", "Deep", "Pure", "Velvet",
        "Prism", "Nova", "Aura", "Luxe", "Mist",
        "Pulse", "Flash", "Silk", "Silk Pro", "Dream",
        "Spark", "Prime", "Edge", "Radiant", "Max"
    )

    val originalFilter = SnapFilter(
        id = "original",
        name = "Original",
        categoryId = "trending",
        categoryTitle = "Trending Filters",
        saturation = 1f,
        contrast = 1f,
        brightnessOffset = 0f,
        warmth = 0f,
        tintMagenta = 0f,
        hueDegrees = 0f,
        sepiaAmount = 0f,
        overlayColorArgb = 0x00000000L,
        overlayAlpha = 0f,
        vignetteStrength = 0f,
        glowIntensity = 0f,
        blurStrength = 0f,
        distortionType = DistortionType.NONE,
        accentColorArgb = 0xFFFFFC00L,
        trending = true
    )

    // 43 categories x 25 distinct tuned variants + Original = 1,076 unique filters!
    val allFilters: List<SnapFilter> by lazy {
        val list = ArrayList<SnapFilter>(1080)
        list.add(originalFilter)
        categories.forEachIndexed { catIdx, cat ->
            styleDescriptors.forEachIndexed { varIdx, descriptor ->
                val step = (varIdx - 12) / 12f // -1.0 .. +1.06
                val variantRatio = varIdx / 24f // 0.0 .. 1.0
                val filter = buildDistinctFilter(cat, catIdx, varIdx, descriptor, step, variantRatio)
                list.add(filter)
            }
        }
        list
    }

    private fun buildDistinctFilter(
        cat: FilterCategoryMeta,
        catIdx: Int,
        varIdx: Int,
        descriptor: String,
        step: Float,
        ratio: Float
    ): SnapFilter {
        val numTag = (varIdx + 1).toString().padStart(2, '0')
        val shortTitle = cat.title
            .replace(" Filters", "")
            .replace(" Look", "")
            .replace(" Mode", "")
            .replace(" Effects", "")
        val name = "$shortTitle $descriptor $numTag"
        val id = "${cat.id}_$numTag"

        var saturation = 1f + step * 0.25f
        var contrast = 1.02f + ratio * 0.24f
        var brightness = step * 10f
        var warmth = 0f
        var tint = 0f
        var hue = 0f
        var sepia = 0f
        var overlayColor = 0x00000000L
        var overlayAlpha = 0f
        var vignette = 0.1f + ratio * 0.25f
        var glow = 0f
        var blur = 0f
        var distortion = DistortionType.NONE
        var accent = 0xFFFFFC00L

        when (cat.id) {
            "trending" -> {
                saturation = 1.15f + ratio * 0.35f
                contrast = 1.08f + ratio * 0.2f
                warmth = (varIdx % 5 - 2) * 0.15f
                hue = (varIdx - 12) * 3.5f
                overlayColor = if (varIdx % 2 == 0) 0xFFFFFC00L else 0xFF00E5FFL
                overlayAlpha = 0.06f + ratio * 0.08f
                accent = overlayColor
            }
            "funny" -> {
                distortion = listOf(
                    DistortionType.BIG_HEAD, DistortionType.SMALL_FACE, DistortionType.FUNNY_EYES,
                    DistortionType.CARTOON_POP, DistortionType.CHROMA_3D
                )[varIdx % 5]
                hue = ((varIdx * 23) % 360 - 180).toFloat()
                saturation = 1.35f + ratio * 0.45f
                accent = 0xFFFF6D00L
            }
            "distortion" -> {
                distortion = if (varIdx % 3 == 0) DistortionType.BIG_HEAD else if (varIdx % 3 == 1) DistortionType.SMALL_FACE else DistortionType.FUNNY_EYES
                hue = (varIdx * 11 - 120).toFloat()
                contrast = 1.1f + ratio * 0.3f
                accent = 0xFF7C4DFFL
            }
            "big_head" -> {
                distortion = DistortionType.BIG_HEAD
                saturation = 1.1f + ratio * 0.3f
                warmth = 0.12f + step * 0.18f
                hue = step * 18f
                accent = 0xFFFFD600L
            }
            "small_face" -> {
                distortion = DistortionType.SMALL_FACE
                saturation = 1.05f + ratio * 0.25f
                warmth = -0.1f + step * 0.2f
                hue = step * -20f
                accent = 0xFF00E5FFL
            }
            "funny_eyes" -> {
                distortion = DistortionType.FUNNY_EYES
                saturation = 1.25f + ratio * 0.35f
                contrast = 1.12f + ratio * 0.2f
                hue = (varIdx * 9 - 90).toFloat()
                accent = 0xFFFF4081L
            }
            "animal" -> {
                distortion = if (varIdx % 2 == 0) DistortionType.DOG_FACE else DistortionType.CAT_FACE
                warmth = 0.15f + step * 0.15f
                saturation = 1.1f + ratio * 0.2f
                accent = 0xFFFFAB40L
            }
            "dog" -> {
                distortion = DistortionType.DOG_FACE
                warmth = 0.2f + ratio * 0.25f
                brightness = 6f + step * 8f
                overlayColor = 0xFFFFCC80L
                overlayAlpha = 0.08f + ratio * 0.06f
                accent = 0xFFFFB74DL
            }
            "cat" -> {
                distortion = DistortionType.CAT_FACE
                tint = 0.18f + ratio * 0.2f
                brightness = 8f + step * 7f
                overlayColor = 0xFFF48FB1L
                overlayAlpha = 0.08f + ratio * 0.07f
                accent = 0xFFF06292L
            }
            "cartoon" -> {
                distortion = DistortionType.CARTOON_POP
                saturation = 1.55f + ratio * 0.45f
                contrast = 1.28f + ratio * 0.3f
                hue = step * 16f
                accent = 0xFF00E676L
            }
            "anime" -> {
                distortion = DistortionType.ANIME_EDGE
                saturation = 1.4f + ratio * 0.35f
                contrast = 1.22f + ratio * 0.22f
                brightness = 10f + ratio * 12f
                overlayColor = 0xFF80D8FFL
                overlayAlpha = 0.07f + ratio * 0.06f
                accent = 0xFF40C4FFL
            }
            "glow" -> {
                glow = 0.35f + ratio * 0.65f
                brightness = 12f + ratio * 16f
                saturation = 1.15f + ratio * 0.25f
                overlayColor = 0xFFFFF59DL
                overlayAlpha = 0.10f + ratio * 0.10f
                accent = 0xFFFFEE58L
            }
            "neon" -> {
                distortion = DistortionType.NEON_GLOW
                glow = 0.55f + ratio * 0.45f
                contrast = 1.25f + ratio * 0.25f
                saturation = 1.5f + ratio * 0.4f
                hue = (varIdx * 15 - 180).toFloat()
                overlayColor = if (varIdx % 3 == 0) 0xFF00E5FFL else if (varIdx % 3 == 1) 0xFFFF00E5L else 0xFFCCFF00L
                overlayAlpha = 0.14f + ratio * 0.10f
                accent = overlayColor
            }
            "hdr" -> {
                contrast = 1.28f + ratio * 0.32f
                saturation = 1.32f + ratio * 0.3f
                brightness = -4f + step * 10f
                vignette = 0.18f + ratio * 0.2f
                accent = 0xFF69F0AEL
            }
            "iphone_look" -> {
                contrast = 1.10f + ratio * 0.14f
                saturation = 1.08f + ratio * 0.14f
                warmth = 0.08f + step * 0.12f
                brightness = 6f + ratio * 8f
                vignette = 0.06f
                accent = 0xFFE0E0E0L
            }
            "iphone_hdr" -> {
                contrast = 1.22f + ratio * 0.18f
                saturation = 1.22f + ratio * 0.2f
                brightness = 8f + step * 8f
                warmth = 0.10f + step * 0.1f
                glow = 0.15f + ratio * 0.15f
                accent = 0xFF82B1FFL
            }
            "dslr" -> {
                contrast = 1.18f + ratio * 0.2f
                saturation = 1.12f + ratio * 0.18f
                vignette = 0.28f + ratio * 0.28f
                blur = 0.15f + ratio * 0.25f
                warmth = 0.06f + step * 0.15f
                accent = 0xFFFFAB00L
            }
            "cinematic" -> {
                contrast = 1.24f + ratio * 0.22f
                saturation = 0.88f + ratio * 0.24f
                warmth = 0.12f - ratio * 0.28f
                vignette = 0.32f + ratio * 0.32f
                overlayColor = if (varIdx % 2 == 0) 0xFF0D47A1L else 0xFFBF360CL
                overlayAlpha = 0.10f + ratio * 0.08f
                accent = 0xFFFF7043L
            }
            "portrait" -> {
                contrast = 1.06f + ratio * 0.14f
                saturation = 1.05f + ratio * 0.15f
                brightness = 8f + ratio * 10f
                glow = 0.2f + ratio * 0.2f
                vignette = 0.22f + ratio * 0.2f
                accent = 0xFFFF80ABL
            }
            "vintage" -> {
                sepia = 0.35f + ratio * 0.45f
                contrast = 0.92f + ratio * 0.2f
                saturation = 0.72f + ratio * 0.22f
                vignette = 0.35f + ratio * 0.35f
                overlayColor = 0xFF8D6E63L
                overlayAlpha = 0.12f + ratio * 0.08f
                accent = 0xFFD7CCC8L
            }
            "retro" -> {
                sepia = 0.22f + ratio * 0.35f
                hue = -18f + ratio * 36f
                contrast = 1.12f + ratio * 0.22f
                overlayColor = if (varIdx % 2 == 0) 0xFFFF8A65L else 0xFFBA68C8L
                overlayAlpha = 0.14f + ratio * 0.09f
                vignette = 0.28f + ratio * 0.28f
                accent = 0xFFFF8A65L
            }
            "bw" -> {
                saturation = 0f
                contrast = 1.08f + ratio * 0.55f
                brightness = -12f + ratio * 24f
                vignette = 0.20f + ratio * 0.40f
                accent = 0xFFBDBDBDL
            }
            "golden_hour" -> {
                warmth = 0.45f + ratio * 0.45f
                saturation = 1.18f + ratio * 0.22f
                brightness = 8f + ratio * 12f
                glow = 0.25f + ratio * 0.35f
                overlayColor = 0xFFFFB300L
                overlayAlpha = 0.15f + ratio * 0.12f
                accent = 0xFFFFCA28L
            }
            "sunset" -> {
                warmth = 0.40f + ratio * 0.40f
                tint = 0.25f + ratio * 0.3f
                contrast = 1.15f + ratio * 0.2f
                overlayColor = 0xFFFF5252L
                overlayAlpha = 0.14f + ratio * 0.12f
                accent = 0xFFFF5252L
            }
            "sunrise" -> {
                warmth = 0.30f + ratio * 0.35f
                brightness = 14f + ratio * 16f
                tint = 0.15f + ratio * 0.2f
                overlayColor = 0xFFFFD54FL
                overlayAlpha = 0.14f + ratio * 0.10f
                accent = 0xFFFFD54FL
            }
            "night" -> {
                brightness = -18f + ratio * 16f
                contrast = 1.25f + ratio * 0.3f
                warmth = -0.35f - ratio * 0.2f
                overlayColor = 0xFF1A237EL
                overlayAlpha = 0.18f + ratio * 0.14f
                vignette = 0.38f + ratio * 0.3f
                accent = 0xFF536DFEL
            }
            "moonlight" -> {
                warmth = -0.45f - ratio * 0.25f
                saturation = 0.65f + ratio * 0.25f
                brightness = -8f + step * 8f
                overlayColor = 0xFF80DEEAL
                overlayAlpha = 0.14f + ratio * 0.12f
                glow = 0.25f + ratio * 0.25f
                accent = 0xFF84FFFFL
            }
            "rainy" -> {
                distortion = DistortionType.RAIN_DROPS
                warmth = -0.30f - ratio * 0.2f
                saturation = 0.80f + ratio * 0.2f
                contrast = 1.10f + ratio * 0.2f
                overlayColor = 0xFF455A64L
                overlayAlpha = 0.14f + ratio * 0.12f
                accent = 0xFF4FC3F7L
            }
            "fog" -> {
                distortion = DistortionType.FOG_MIST
                contrast = 0.72f + ratio * 0.18f
                brightness = 16f + ratio * 18f
                saturation = 0.75f + ratio * 0.18f
                overlayColor = 0xFFECEFF1L
                overlayAlpha = 0.20f + ratio * 0.20f
                accent = 0xFFCFD8DCL
            }
            "warm" -> {
                warmth = 0.35f + ratio * 0.50f
                saturation = 1.10f + ratio * 0.2f
                overlayColor = 0xFFFF8F00L
                overlayAlpha = 0.08f + ratio * 0.10f
                accent = 0xFFFFA726L
            }
            "cool" -> {
                warmth = -0.35f - ratio * 0.45f
                saturation = 1.05f + ratio * 0.2f
                overlayColor = 0xFF00ACC1L
                overlayAlpha = 0.08f + ratio * 0.10f
                accent = 0xFF26C6DAL
            }
            "blue" -> {
                warmth = -0.55f - ratio * 0.35f
                hue = -12f - ratio * 14f
                overlayColor = 0xFF1565C0L
                overlayAlpha = 0.16f + ratio * 0.16f
                accent = 0xFF42A5F5L
            }
            "orange_teal" -> {
                warmth = 0.22f + step * 0.2f
                hue = -14f + ratio * 20f
                contrast = 1.24f + ratio * 0.24f
                saturation = 1.28f + ratio * 0.24f
                overlayColor = if (varIdx % 2 == 0) 0xFF00838FL else 0xFFE65100L
                overlayAlpha = 0.12f + ratio * 0.08f
                accent = 0xFF26A69AL
            }
            "beauty" -> {
                brightness = 12f + ratio * 12f
                contrast = 0.94f + ratio * 0.10f
                saturation = 1.06f + ratio * 0.12f
                warmth = 0.10f + ratio * 0.10f
                glow = 0.22f + ratio * 0.25f
                accent = 0xFFFF80ABL
            }
            "soft_skin" -> {
                brightness = 14f + ratio * 14f
                contrast = 0.88f + ratio * 0.10f
                tint = 0.10f + ratio * 0.12f
                glow = 0.35f + ratio * 0.35f
                blur = 0.15f + ratio * 0.2f
                accent = 0xFFF8BBD0L
            }
            "light_fx" -> {
                distortion = DistortionType.BOKEH_LIGHT
                glow = 0.40f + ratio * 0.5f
                brightness = 10f + ratio * 12f
                hue = step * 25f
                overlayColor = 0xFFFFF176L
                overlayAlpha = 0.12f + ratio * 0.10f
                accent = 0xFFFFF176L
            }
            "lens_fx" -> {
                distortion = DistortionType.LENS_FLARE
                contrast = 1.14f + ratio * 0.22f
                glow = 0.35f + ratio * 0.4f
                overlayColor = 0xFF40C4FFL
                overlayAlpha = 0.10f + ratio * 0.10f
                accent = 0xFF40C4FFL
            }
            "blur_fx" -> {
                distortion = DistortionType.BLUR_DREAM
                blur = 0.35f + ratio * 0.65f
                glow = 0.20f + ratio * 0.25f
                brightness = 8f + ratio * 8f
                accent = 0xFFB388FFL
            }
            "bg_fx" -> {
                distortion = DistortionType.BOKEH_LIGHT
                vignette = 0.35f + ratio * 0.35f
                blur = 0.20f + ratio * 0.3f
                hue = step * 30f
                accent = 0xFF80CBC4L
            }
            "three_d" -> {
                distortion = DistortionType.CHROMA_3D
                contrast = 1.22f + ratio * 0.25f
                saturation = 1.35f + ratio * 0.3f
                hue = step * 20f
                accent = 0xFF00E5FFL
            }
            "festival" -> {
                distortion = DistortionType.FESTIVAL_DIWALI
                warmth = 0.35f + ratio * 0.35f
                saturation = 1.38f + ratio * 0.3f
                glow = 0.35f + ratio * 0.35f
                overlayColor = 0xFFFF9800L
                overlayAlpha = 0.14f + ratio * 0.10f
                accent = 0xFFFFAB00L
            }
            "indian_trad" -> {
                distortion = DistortionType.INDIAN_TRADITIONAL
                warmth = 0.30f + ratio * 0.3f
                saturation = 1.32f + ratio * 0.28f
                sepia = 0.10f + ratio * 0.15f
                overlayColor = 0xFFD81B60L
                overlayAlpha = 0.10f + ratio * 0.08f
                accent = 0xFFFF4081L
            }
            "wedding" -> {
                distortion = DistortionType.WEDDING_ROYAL
                warmth = 0.28f + ratio * 0.28f
                glow = 0.35f + ratio * 0.35f
                brightness = 10f + ratio * 10f
                overlayColor = 0xFFFFD700L
                overlayAlpha = 0.14f + ratio * 0.10f
                accent = 0xFFFFD700L
            }
        }

        return SnapFilter(
            id = id,
            name = name,
            categoryId = cat.id,
            categoryTitle = cat.title,
            saturation = saturation,
            contrast = contrast,
            brightnessOffset = brightness,
            warmth = warmth,
            tintMagenta = tint,
            hueDegrees = hue,
            sepiaAmount = sepia,
            overlayColorArgb = overlayColor,
            overlayAlpha = overlayAlpha,
            vignetteStrength = vignette,
            glowIntensity = glow,
            blurStrength = blur,
            distortionType = distortion,
            accentColorArgb = accent,
            trending = (cat.id == "trending" || varIdx == 0)
        )
    }
}

object FilterEngine {
    fun applyFilterToBitmap(
        source: Bitmap,
        filter: SnapFilter,
        beautyLevel: Float = 0f,
        brightnessLevel: Float = 0f
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix(filter.buildColorMatrix(beautyLevel, brightnessLevel))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Overlay color tint
        if (filter.overlayAlpha > 0.01f && filter.overlayColorArgb != 0L) {
            val baseColor = Color(filter.overlayColorArgb)
            val tintPaint = Paint().apply {
                color = android.graphics.Color.argb(
                    (filter.overlayAlpha * 255).toInt().coerceIn(0, 255),
                    (baseColor.red * 255).toInt(),
                    (baseColor.green * 255).toInt(),
                    (baseColor.blue * 255).toInt()
                )
            }
            canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), tintPaint)
        }

        // Vignette
        if (filter.vignetteStrength > 0.05f) {
            val cx = out.width / 2f
            val cy = out.height / 2f
            val radius = maxOf(cx, cy) * 1.15f
            val edgeAlpha = (filter.vignetteStrength * 200).toInt().coerceIn(0, 210)
            val vigPaint = Paint().apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb(edgeAlpha, 0, 0, 0)),
                    floatArrayOf(0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), vigPaint)
        }

        // Decorative distortion/frame accents burned into saved bitmap
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (out.width * 0.045f).coerceAtLeast(24f)
            color = android.graphics.Color.WHITE
        }
        when (filter.distortionType) {
            DistortionType.DOG_FACE -> {
                canvas.drawText("🐶", out.width * 0.44f, out.height * 0.14f, badgePaint)
            }
            DistortionType.CAT_FACE -> {
                canvas.drawText("🐱", out.width * 0.44f, out.height * 0.14f, badgePaint)
            }
            DistortionType.FESTIVAL_DIWALI -> {
                canvas.drawText("🪔 ✨ 🪔", out.width * 0.36f, out.height * 0.12f, badgePaint)
            }
            DistortionType.WEDDING_ROYAL -> {
                canvas.drawText("👑", out.width * 0.45f, out.height * 0.12f, badgePaint)
            }
            DistortionType.INDIAN_TRADITIONAL -> {
                canvas.drawText("🪷", out.width * 0.45f, out.height * 0.12f, badgePaint)
            }
            else -> Unit
        }
        return out
    }
}
