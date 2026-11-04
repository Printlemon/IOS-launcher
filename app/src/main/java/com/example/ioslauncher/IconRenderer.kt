package com.example.ioslauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Layer-based icon rendering: masking, per-layer filters, gradients and highlights (no flat overlays). */
object IconRenderer {
    private const val S = 192
    private const val F = S.toFloat()

    fun render(src: Drawable, s: LauncherSettings): ImageBitmap {
        val d = src.constantState?.newDrawable()?.mutate() ?: src
        val raw = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(raw)
        val adaptive = d is AdaptiveIconDrawable
        val bg: Drawable? = (d as? AdaptiveIconDrawable)?.background
        val fg: Drawable = if (d is AdaptiveIconDrawable) d.foreground else d
        if (adaptive) {
            val o = (S * 0.25f).toInt() // adaptive layers are 108dp, visible 72dp
            bg?.setBounds(-o, -o, S + o, S + o)
            fg.setBounds(-o, -o, S + o, S + o)
        } else {
            val i = (S * 0.14f).toInt()
            fg.setBounds(i, i, S - i, S - i)
        }
        when (s.style) {
            IconStyle.Default -> {
                if (adaptive) bg?.draw(c) else c.drawColor(Color.WHITE)
                fg.draw(c)
            }
            IconStyle.Dark -> dark(c, bg, fg, adaptive)
            IconStyle.Tinted -> tinted(c, fg, adaptive, s)
            IconStyle.Glass -> glass(c, bg, fg, adaptive, s)
        }
        val out = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val oc = Canvas(out)
        val bp = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (s.brightness != 1f) {
            bp.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(s.brightness, s.brightness, s.brightness, 1f) })
        }
        oc.drawBitmap(raw, 0f, 0f, bp)
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        val r = s.radius * F
        oc.drawRoundRect(RectF(0f, 0f, F, F), r, r, mask)
        raw.recycle()
        return out.asImageBitmap()
    }

    private fun gradient(c: Canvas, top: Int, bottom: Int) {
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, F, top, bottom, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, F, F, p)
    }

    private fun blend(a: Int, b: Int, f: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * f).toInt().coerceIn(0, 255)
        return Color.argb(255, ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }

    private fun white(a: Float) = Color.argb((255 * a).toInt().coerceIn(0, 255), 255, 255, 255)

    private fun dark(c: Canvas, bg: Drawable?, fg: Drawable, adaptive: Boolean) {
        gradient(c, 0xFF3A3A3C.toInt(), 0xFF1A1A1C.toInt())
        if (adaptive) bg?.let {
            it.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.24f, 0.24f, 0.24f, 1f) })
            it.draw(c)
        }
        fg.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
            if (adaptive) setSaturation(1.15f) else setScale(0.55f, 0.55f, 0.55f, 1f)
        })
        fg.draw(c)
    }

    private fun tinted(c: Canvas, fg: Drawable, adaptive: Boolean, s: LauncherSettings) {
        val t = blend(s.tint, Color.WHITE, 1f - s.tintIntensity)
        gradient(
            c,
            blend(0xFF2C2C2E.toInt(), s.tint, 0.28f * s.tintIntensity),
            blend(0xFF111113.toInt(), s.tint, 0.12f * s.tintIntensity)
        )
        val r = Color.red(t).toFloat(); val g = Color.green(t).toFloat(); val b = Color.blue(t).toFloat()
        // Adaptive foregrounds: flat silhouette in tint. Legacy icons: luminance -> alpha so the white plate drops out.
        val m = if (adaptive) floatArrayOf(
            0f, 0f, 0f, 0f, r, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 1f, 0f
        ) else floatArrayOf(
            0f, 0f, 0f, 0f, r, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, b, -0.213f, -0.715f, -0.072f, 0f, 255f
        )
        fg.colorFilter = ColorMatrixColorFilter(ColorMatrix(m))
        fg.draw(c)
    }

    private fun glass(c: Canvas, bg: Drawable?, fg: Drawable, adaptive: Boolean, s: LauncherSettings) {
        val g = s.glass
        // 1. translucent body from the app's own background layer
        if (adaptive) bg?.let { it.alpha = (255 * (0.92f - 0.5f * g)).toInt(); it.draw(c) }
        else c.drawColor(Color.argb((255 * (0.85f - 0.4f * g)).toInt(), 235, 240, 255))
        // 2. frost layer
        val frost = Paint().apply { shader = LinearGradient(0f, 0f, 0f, F, white(0.40f * g), white(0.05f * g), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, F, F, frost)
        // 3. glyph
        fg.alpha = 240
        fg.draw(c)
        // 4. specular dome highlight
        val hl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, F * 0.5f, white(0.55f * g), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawOval(RectF(-F * 0.15f, -F * 0.5f, F * 1.15f, F * 0.5f), hl)
        // 5. rim light (bright top-left, soft bottom-right)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(0f, 0f, F, F, intArrayOf(white(0.9f * g), white(0.12f), white(0.5f * g)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        val r = s.radius * F
        c.drawRoundRect(RectF(1.5f, 1.5f, F - 1.5f, F - 1.5f), r, r, rim)
    }
}
