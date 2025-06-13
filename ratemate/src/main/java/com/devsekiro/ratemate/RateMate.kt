package com.devsekiro.ratemate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class RateMate(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatRatingBar(context, attrs) {

    private var mSampleTile: Bitmap? = null
    private var emptyStarDrawable: Drawable? = null
    private var emptyLastStarDrawable: Drawable? = null
    private var filledStarDrawable: Drawable? = null
    private var filledLastStarDrawable: Drawable? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.RateMate, 0, 0).apply {
            try {
                emptyStarDrawable = getDrawable(R.styleable.RateMate_emptyStarDrawable)
                    ?: ContextCompat.getDrawable(context, R.drawable.ic_rating_star_empty)
                emptyLastStarDrawable = getDrawable(R.styleable.RateMate_emptyLastStarDrawable)
                    ?: ContextCompat.getDrawable(context, R.drawable.ic_rating_star_empty_last_star)
                filledStarDrawable = getDrawable(R.styleable.RateMate_filledStarDrawable)
                    ?: ContextCompat.getDrawable(context, R.drawable.ic_rating_star_filled)
                filledLastStarDrawable = getDrawable(R.styleable.RateMate_filledLastStarDrawable)
                    ?: ContextCompat.getDrawable(context, R.drawable.ic_rating_star_filled_last_star)
                numStars = getInt(R.styleable.RateMate_numStars, numStars)
            } finally {
                recycle()
            }
        }
        progressDrawable = createProgressDrawable()
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mSampleTile?.let {
            val width = it.width * numStars
            setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), measuredHeight)
        }
    }

    private fun createProgressDrawable(): LayerDrawable {
        val backgroundDrawable = createBackgroundDrawableShape()
        val progressDrawableShape = createProgressDrawableShape()
        return LayerDrawable(arrayOf(backgroundDrawable, backgroundDrawable, progressDrawableShape)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.secondaryProgress)
            setId(2, android.R.id.progress)
        }
    }

    private fun createBackgroundDrawableShape(): Drawable {
        emptyStarDrawable?.let {
            val tileBitmap = drawableToBitmap(it)
            if (mSampleTile == null) mSampleTile = tileBitmap
            return createShapeDrawable(tileBitmap, drawableToBitmap(emptyLastStarDrawable!!))
        }
        return ShapeDrawable() // Fallback to avoid potential NPEs
    }

    private fun createProgressDrawableShape(): Drawable {
        filledStarDrawable?.let {
            val tileBitmap = drawableToBitmap(it)
            return ClipDrawable(
                createShapeDrawable(tileBitmap, drawableToBitmap(filledLastStarDrawable!!)),
                Gravity.LEFT, ClipDrawable.HORIZONTAL
            )
        }
        return ShapeDrawable() // Fallback to avoid potential NPEs
    }

    private fun createShapeDrawable(baseBitmap: Bitmap, highlightBitmap: Bitmap): ShapeDrawable {
        val shapeDrawable = ShapeDrawable(drawableShape)
        val shader = createComposeShader(baseBitmap, highlightBitmap)
        shapeDrawable.paint.shader = shader
        return shapeDrawable
    }

    private fun createComposeShader(baseBitmap: Bitmap, highlightBitmap: Bitmap): Shader {
        val repeatShader = BitmapShader(baseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        val highlightShader = BitmapShader(highlightBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val matrix = Matrix()
        matrix.setTranslate((baseBitmap.width * numStars - baseBitmap.width).toFloat(), 0f)
        highlightShader.setLocalMatrix(matrix)

        return ComposeShader(repeatShader, highlightShader, PorterDuff.Mode.SRC_OVER)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val starsMargin = 50
        val width = (drawable.intrinsicWidth * 1.1 + starsMargin).roundToInt()
        val height = (drawable.intrinsicHeight * 1.1).roundToInt()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            drawable.setBounds(starsMargin / 2, 0, canvas.width - starsMargin / 2, canvas.height)
            drawable.draw(canvas)
        }
    }

    private val drawableShape: Shape
        get() {
            val roundedCorners = FloatArray(8) { 5f }
            return RoundRectShape(roundedCorners, null, roundedCorners)
        }
}
