package com.llpine.anchorviewinno.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.llpine.anchorviewinno.R
import kotlin.math.min

open class AnchorPopupLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        enum class AnchorDirection {
            DOWN, UP, LEFT, RIGHT
        }

        enum class GradientDirection {
            DOWN, UP, RIGHT, LEFT
        }
    }

    private val mBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resources.getColor(R.color.colorPrimary)
    }

    private var mAnchorDirection: AnchorDirection = AnchorDirection.DOWN
    private var mCorner: Float = resources.getDimension(R.dimen.popup_layout_corner)
    private var mAnchorWidth = resources.getDimension(R.dimen.popup_layout_anchor_width)
    private var mAnchorHeight = resources.getDimension(R.dimen.popup_layout_anchor_height)
    private var mAnchorOffsetPercentage = 0.5f
    private var mAnchorOffset = 0f
    private val mLayoutColorList = mutableListOf<Int>()
    private var mGradientDirection = GradientDirection.RIGHT
    private val bgPath = Path()
    private var mCubicAnchor = false

    interface OnLayoutListener {
        fun onSizeChanged(w: Int, h: Int)
    }

    private var mLayoutListener: OnLayoutListener? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AnchorPopupLayout)
        try {
            val anchorDirection = ta.getInt(R.styleable.AnchorPopupLayout_anchor_direction, 0)
            setAnchorDirection(anchorDirection, false)

            val color = ta.getColor(R.styleable.AnchorPopupLayout_layout_color, resources.getColor(R.color.colorPrimary))
            mLayoutColorList.add(color)
            if (ta.hasValue(R.styleable.AnchorPopupLayout_layout_gradient_color)) {
                val gradientColor = ta.getColor(R.styleable.AnchorPopupLayout_layout_gradient_color, resources.getColor(R.color.colorPrimary))
                mLayoutColorList.add(gradientColor)
            }

            val gradientDirection = ta.getColor(R.styleable.AnchorPopupLayout_layout_gradient_direction, 3)
            setGradientDirection(gradientDirection)
            setupLayoutColor()

            val cornerPathEffect = ta.getBoolean(R.styleable.AnchorPopupLayout_corner_path_effect, false)
            setEnableCornerPathEffect(cornerPathEffect, false)

            val cubicAnchor = ta.getBoolean(R.styleable.AnchorPopupLayout_cubic_anchor, false)
            setEnableCubicAnchor(cubicAnchor, false)

            val corner = ta.getDimension(R.styleable.AnchorPopupLayout_layout_corner, resources.getDimension(R.dimen.popup_layout_corner))
            setLayoutCorner(corner, false)

            val offsetPercentage = ta.getFloat(R.styleable.AnchorPopupLayout_anchor_offset_percentage, 0.5f)
            setAnchorOffsetPercentage(offsetPercentage, false)

            val offset = ta.getDimension(R.styleable.AnchorPopupLayout_anchor_offset, 0f)
            setAnchorOffset(offset, false)

            val anchorWidth = ta.getDimension(R.styleable.AnchorPopupLayout_anchor_width, resources.getDimension(R.dimen.popup_layout_anchor_width))
            val anchorHeight = ta.getDimension(R.styleable.AnchorPopupLayout_anchor_height, resources.getDimension(R.dimen.popup_layout_anchor_height))
            setAnchorSize(anchorWidth, anchorHeight)
        } finally {
            ta.recycle()
        }

        setupPadding()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupLayoutPath()
        if (mLayoutColorList.size > 1) {
            setupLayoutColor()
        }
        mLayoutListener?.onSizeChanged(w, h)
    }

    fun setOnLayoutListener(listener: OnLayoutListener) {
        mLayoutListener = listener
    }

    // Only invoked during initialization
    private fun setAnchorDirection(direction: Int, invalidate: Boolean = true) {
        mAnchorDirection = if (direction < 0 || direction >= AnchorDirection.values().size) {
            AnchorDirection.DOWN
        } else {
            AnchorDirection.values()[direction]
        }

        if (invalidate) {
            setupLayoutPath()
        }
    }

    fun setGradientDirection(direction: Int) {
        mGradientDirection = if (direction < 0 || direction >= GradientDirection.values().size) {
            GradientDirection.RIGHT
        } else {
            GradientDirection.values()[direction]
        }
    }

    fun setupLayoutColor() {
        if (mLayoutColorList.isEmpty()) {
            return
        }

        if (mLayoutColorList.size == 1) {
            mBgPaint.color = mLayoutColorList[0]
        } else {
            val startX = when (mGradientDirection) {
                GradientDirection.LEFT -> width.toFloat()
                else -> 0f
            }
            val endX = when (mGradientDirection) {
                GradientDirection.RIGHT -> width.toFloat()
                else -> 0f
            }
            val startY = when (mGradientDirection) {
                GradientDirection.UP -> height.toFloat()
                else -> 0f
            }
            val endY = when (mGradientDirection) {
                GradientDirection.DOWN -> height.toFloat()
                else -> 0f
            }
            val shader = LinearGradient(startX, startY, endX, endY, mLayoutColorList.toIntArray(), null, Shader.TileMode.CLAMP)
            mBgPaint.shader = shader
        }
        invalidate()
    }

    fun setLayoutCorner(corner: Float, invalidate: Boolean = true) {
        mCorner = corner
        if (invalidate) {
            setupLayoutPath()
        }
    }

    fun setAnchorSize(width: Float, height: Float, invalidate: Boolean = true) {
        mAnchorWidth = width
        mAnchorHeight = height
        if (invalidate) {
            setupLayoutPath()
        }
    }

    fun setAnchorOffsetPercentage(percentage: Float, invalidate: Boolean = true) {
        mAnchorOffsetPercentage = percentage
        if (invalidate) {
            setupLayoutPath()
        }
    }

    fun setAnchorOffset(offset: Float, invalidate: Boolean = true) {
        mAnchorOffset = offset
        if (invalidate) {
            setupLayoutPath()
        }
    }

    fun setEnableCornerPathEffect(enable: Boolean, invalidate: Boolean = true) {
        if (enable) {
            if (mBgPaint.pathEffect !is CornerPathEffect) {
                mBgPaint.pathEffect = CornerPathEffect(30F)

                if (invalidate) {
                    invalidate()
                }
            }
        } else {
            if (mBgPaint.pathEffect != null) {
                mBgPaint.pathEffect = null

                if (invalidate) {
                    invalidate()
                }
            }
        }
    }

    fun setEnableCubicAnchor(enable: Boolean, invalidate: Boolean = true) {
        if (mCubicAnchor != enable) {
            mCubicAnchor = enable

            if (invalidate) {
                invalidate()
            }
        }
    }

    private fun setupPadding() {
        when (mAnchorDirection) {
            AnchorDirection.DOWN -> setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + mAnchorHeight.toInt())
            AnchorDirection.UP -> setPadding(paddingLeft, paddingTop + mAnchorHeight.toInt(), paddingRight, paddingBottom)
            AnchorDirection.LEFT -> setPadding(paddingLeft + mAnchorHeight.toInt(), paddingTop, paddingRight, paddingBottom)
            AnchorDirection.RIGHT -> setPadding(paddingLeft, paddingTop, paddingRight + mAnchorHeight.toInt(), paddingBottom)
        }
    }

    private fun getAnchorTipOffset(): Float {
        val fullSize = when (mAnchorDirection) {
            AnchorDirection.DOWN, AnchorDirection.UP -> width
            else -> height
        }
        var horAnchorOffset = mCorner + (fullSize.toFloat() - mCorner * 2) * mAnchorOffsetPercentage + mAnchorOffset
        horAnchorOffset = min(horAnchorOffset, fullSize - mCorner - mAnchorWidth / 2)
        return horAnchorOffset
    }

    private fun setupLayoutPath() {
        val corner = mCorner
        val anchorOffset = getAnchorTipOffset()
        bgPath.rewind()

        when (mAnchorDirection) {
            AnchorDirection.DOWN -> {
                bgPath.moveTo(0f, 0f + corner)
                bgPath.arcTo(0f, 0f, 0f + corner, 0f + corner, 180f, 90f, false)
                bgPath.lineTo(width.toFloat() - corner, 0f)
                bgPath.arcTo(width.toFloat() - corner, 0f, width.toFloat(), 0f + corner, 270f, 90f, false)
                bgPath.lineTo(width.toFloat(), height.toFloat() - corner - mAnchorHeight)
                bgPath.arcTo(width.toFloat() - corner, height.toFloat() - corner - mAnchorHeight, width.toFloat(), height.toFloat() - mAnchorHeight, 0f, 90f, false)
                bgPath.lineTo(anchorOffset + mAnchorWidth / 2, height.toFloat() - mAnchorHeight)

                // draw anchor
                if (mCubicAnchor) {
                    bgPath.cubicTo(anchorOffset + mAnchorWidth / 4, height.toFloat() - mAnchorHeight, anchorOffset + mAnchorWidth / 8, height.toFloat(), anchorOffset, height.toFloat())
                    bgPath.cubicTo(anchorOffset - mAnchorWidth / 8, height.toFloat(), anchorOffset - mAnchorWidth / 4, height.toFloat() - mAnchorHeight, anchorOffset - mAnchorWidth / 2, height.toFloat() - mAnchorHeight)
                } else {
                    bgPath.lineTo(anchorOffset, height.toFloat())
                    bgPath.lineTo(anchorOffset - mAnchorWidth / 2, height.toFloat() - mAnchorHeight)
                }

                bgPath.lineTo(0f + corner, height.toFloat() - mAnchorHeight)
                bgPath.arcTo(0f, height.toFloat() - corner - mAnchorHeight, 0f + corner, height.toFloat() - mAnchorHeight, 90f, 90f, false)
                bgPath.close()
            }
            AnchorDirection.UP -> {
                bgPath.moveTo(0f, 0f + corner + mAnchorHeight)
                bgPath.arcTo(0f, mAnchorHeight, 0f + corner, 0f + corner + mAnchorHeight, 180f, 90f, false)
                bgPath.lineTo(anchorOffset - mAnchorWidth / 2, mAnchorHeight)

                // draw anchor
                if (mCubicAnchor) {
                    bgPath.cubicTo(anchorOffset - mAnchorWidth / 4, mAnchorHeight, anchorOffset - mAnchorWidth / 8, 0f, anchorOffset, 0f)
                    bgPath.cubicTo(anchorOffset + mAnchorWidth / 8, 0f, anchorOffset + mAnchorWidth / 4, mAnchorHeight, anchorOffset + mAnchorWidth / 2, mAnchorHeight)
                } else {
                    bgPath.lineTo(anchorOffset, 0f)
                    bgPath.lineTo(anchorOffset + mAnchorWidth / 2, mAnchorHeight)
                }

                bgPath.lineTo(width.toFloat() - corner, mAnchorHeight)
                bgPath.arcTo(width.toFloat() - corner, mAnchorHeight, width.toFloat(), mAnchorHeight + corner, 270f, 90f, false)
                bgPath.lineTo(width.toFloat(), height.toFloat() - corner)
                bgPath.arcTo(width.toFloat() - corner, height.toFloat() - corner, width.toFloat(), height.toFloat(), 0f, 90f, false)
                bgPath.lineTo(corner, height.toFloat())
                bgPath.arcTo(0f, height.toFloat() - corner, 0f + corner, height.toFloat(), 90f, 90f, false)
                bgPath.close()
            }
            AnchorDirection.LEFT -> {
                bgPath.moveTo(0f + mAnchorHeight, 0f + corner)
                bgPath.arcTo(0f + mAnchorHeight, 0f, 0f + corner + mAnchorHeight, 0f + corner, 180f, 90f, false)
                bgPath.lineTo(width.toFloat() - corner, 0f)
                bgPath.arcTo(width.toFloat() - corner, 0f, width.toFloat(), 0 + corner, 270f, 90f, false)
                bgPath.lineTo(width.toFloat(), height.toFloat() - corner)
                bgPath.arcTo(width.toFloat() - corner, height.toFloat() - corner, width.toFloat(), height.toFloat(), 0f, 90f, false)
                bgPath.lineTo(0f + corner + mAnchorHeight, height.toFloat())
                bgPath.arcTo(0f + mAnchorHeight, height.toFloat() - corner, 0f + corner + mAnchorHeight, height.toFloat(), 90f, 90f, false)
                bgPath.lineTo(0f + mAnchorHeight, anchorOffset + mAnchorWidth / 2)

                // draw anchor
                if (mCubicAnchor) {
                    bgPath.cubicTo(0f + mAnchorHeight, anchorOffset + mAnchorWidth / 4, 0f, anchorOffset + mAnchorWidth / 8, 0f, anchorOffset)
                    bgPath.cubicTo(0f, anchorOffset - mAnchorWidth / 8, 0f + mAnchorHeight, anchorOffset - mAnchorWidth / 4, 0f + mAnchorHeight, anchorOffset - mAnchorWidth / 2)
                } else {
                    bgPath.lineTo(0f, anchorOffset)
                    bgPath.lineTo(0f + mAnchorHeight, anchorOffset - mAnchorWidth / 2)
                }

                bgPath.close()
            }
            AnchorDirection.RIGHT -> {
                bgPath.moveTo(0f, 0f + corner)
                bgPath.arcTo(0f, 0f, 0f + corner, 0f + corner, 180f, 90f, false)
                bgPath.lineTo(width.toFloat() - corner - mAnchorHeight, 0f)
                bgPath.arcTo(width.toFloat() - corner - mAnchorHeight, 0f, width.toFloat() - mAnchorHeight, corner, 270f, 90f, false)
                bgPath.lineTo(width.toFloat() - mAnchorHeight, anchorOffset - mAnchorWidth / 2)

                // draw anchor
                if (mCubicAnchor) {
                    bgPath.cubicTo(width.toFloat() - mAnchorHeight, anchorOffset - mAnchorWidth / 4, width.toFloat(), anchorOffset - mAnchorWidth / 8, width.toFloat(), anchorOffset)
                    bgPath.cubicTo(width.toFloat(), anchorOffset + mAnchorWidth / 8, width.toFloat() - mAnchorHeight, anchorOffset + mAnchorWidth / 4, width.toFloat() - mAnchorHeight, anchorOffset + mAnchorWidth / 2)
                } else {
                    bgPath.lineTo(width.toFloat(), anchorOffset)
                    bgPath.lineTo(width.toFloat() - mAnchorHeight, anchorOffset + mAnchorWidth / 2)
                }

                bgPath.lineTo(width.toFloat() - mAnchorHeight, height.toFloat() - corner)
                bgPath.arcTo(width.toFloat() - mAnchorHeight - corner, height.toFloat() - corner, width.toFloat() - mAnchorHeight, height.toFloat(), 0f, 90f, false)
                bgPath.lineTo(0f + corner, height.toFloat())
                bgPath.arcTo(0f, height.toFloat() - corner, 0f + corner, height.toFloat(), 90f, 90f, false)
                bgPath.close()
            }
        }

        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        canvas?.let {
            canvas.drawPath(bgPath, mBgPaint)
        }
        super.dispatchDraw(canvas)
    }
}