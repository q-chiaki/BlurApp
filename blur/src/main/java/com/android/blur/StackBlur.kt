package com.android.blur

import android.graphics.Bitmap
import android.graphics.Color

class StackBlur {

    companion object {

        /**
         * Apply Stack Blur to a bitmap
         * @param bitmap Original bitmap
         * @param radius Blur radius (1-254)
         * @return Blurred bitmap
         */
        fun blur(bitmap: Bitmap, radius: Int): Bitmap {
            require(radius >= 1) { "Radius must be >= 1" }
            require(radius <= 254) { "Radius must be <= 254" }

            val width = bitmap.width
            val height = bitmap.height

            // Create mutable copy of the bitmap
            val blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            // Get pixel array
            val pixels = IntArray(width * height)
            blurredBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Apply horizontal blur
            blurHorizontal(pixels, width, height, radius)

            // Apply vertical blur
            blurVertical(pixels, width, height, radius)

            // Set pixels back to bitmap
            blurredBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            return blurredBitmap
        }

        /**
         * Apply horizontal blur pass
         */
        private fun blurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
            val div = radius * 2 + 1
            val r = IntArray(width)
            val g = IntArray(width)
            val b = IntArray(width)
            val a = IntArray(width)

            val vMin = IntArray(maxOf(width, height))
            val vMax = IntArray(maxOf(width, height))

            val dv = IntArray(256 * div)
            for (i in 0 until 256 * div) {
                dv[i] = i / div
            }

            var yi = 0

            for (y in 0 until height) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var aSum = 0

                for (i in -radius..radius) {
                    val p = pixels[yi + minOf(width - 1, maxOf(0, i))]
                    rSum += Color.red(p)
                    gSum += Color.green(p)
                    bSum += Color.blue(p)
                    aSum += Color.alpha(p)
                }

                for (x in 0 until width) {
                    r[x] = dv[rSum]
                    g[x] = dv[gSum]
                    b[x] = dv[bSum]
                    a[x] = dv[aSum]

                    if (y == 0) {
                        vMin[x] = minOf(x + radius + 1, width - 1)
                        vMax[x] = maxOf(x - radius, 0)
                    }

                    val p1 = pixels[yi + vMin[x]]
                    val p2 = pixels[yi + vMax[x]]

                    rSum += Color.red(p1) - Color.red(p2)
                    gSum += Color.green(p1) - Color.green(p2)
                    bSum += Color.blue(p1) - Color.blue(p2)
                    aSum += Color.alpha(p1) - Color.alpha(p2)
                }

                for (x in 0 until width) {
                    pixels[yi + x] = Color.argb(a[x], r[x], g[x], b[x])
                }

                yi += width
            }
        }

        /**
         * Apply vertical blur pass
         */
        private fun blurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
            val div = radius * 2 + 1
            val r = IntArray(height)
            val g = IntArray(height)
            val b = IntArray(height)
            val a = IntArray(height)

            val vMin = IntArray(maxOf(width, height))
            val vMax = IntArray(maxOf(width, height))

            val dv = IntArray(256 * div)
            for (i in 0 until 256 * div) {
                dv[i] = i / div
            }

            for (x in 0 until width) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var aSum = 0

                var yp = -radius * width

                for (i in -radius..radius) {
                    val yi = maxOf(0, yp) + x
                    val p = pixels[yi]

                    rSum += Color.red(p)
                    gSum += Color.green(p)
                    bSum += Color.blue(p)
                    aSum += Color.alpha(p)

                    yp += width
                }

                var yi = x
                for (y in 0 until height) {
                    r[y] = dv[rSum]
                    g[y] = dv[gSum]
                    b[y] = dv[bSum]
                    a[y] = dv[aSum]

                    if (x == 0) {
                        vMin[y] = minOf(y + radius + 1, height - 1) * width
                        vMax[y] = maxOf(y - radius, 0) * width
                    }

                    val p1 = pixels[x + vMin[y]]
                    val p2 = pixels[x + vMax[y]]

                    rSum += Color.red(p1) - Color.red(p2)
                    gSum += Color.green(p1) - Color.green(p2)
                    bSum += Color.blue(p1) - Color.blue(p2)
                    aSum += Color.alpha(p1) - Color.alpha(p2)

                    yi += width
                }

                var yi2 = x
                for (y in 0 until height) {
                    pixels[yi2] = Color.argb(a[y], r[y], g[y], b[y])
                    yi2 += width
                }
            }
        }
    }
}

/**
 * Extension functions for easier usage
 */
fun Bitmap.stackBlur(radius: Int): Bitmap {
    return StackBlur.blur(this, radius)
}

/**
 * Builder class for advanced blur operations
 */
class StackBlurBuilder {
    private var radius: Int = 10
    private var bitmap: Bitmap? = null

    fun radius(radius: Int): StackBlurBuilder {
        this.radius = radius
        return this
    }

    fun bitmap(bitmap: Bitmap): StackBlurBuilder {
        this.bitmap = bitmap
        return this
    }

    fun build(): Bitmap {
        val bmp = bitmap ?: throw IllegalStateException("Bitmap not set")
        return StackBlur.blur(bmp, radius)
    }
}

/**
 * Utility class for different blur effects
 */
object BlurEffects {

    /**
     * Apply light blur effect
     */
    fun lightBlur(bitmap: Bitmap): Bitmap {
        return StackBlur.blur(bitmap, 5)
    }

    /**
     * Apply medium blur effect
     */
    fun mediumBlur(bitmap: Bitmap): Bitmap {
        return StackBlur.blur(bitmap, 15)
    }

    /**
     * Apply heavy blur effect
     */
    fun heavyBlur(bitmap: Bitmap): Bitmap {
        return StackBlur.blur(bitmap, 25)
    }

    /**
     * Apply progressive blur (multiple passes with increasing radius)
     */
    fun progressiveBlur(bitmap: Bitmap, steps: Int = 3): Bitmap {
        var result = bitmap
        val baseRadius = 5

        for (i in 1..steps) {
            result = StackBlur.blur(result, baseRadius * i)
        }

        return result
    }
}