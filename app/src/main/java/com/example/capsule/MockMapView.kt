package com.example.capsule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.hypot

/**
 * A hand-drawn placeholder map: streets, a park, a pond, the user's dot and capsule pins.
 *
 * It's only for building the UI. Later it can be replaced with a real map
 * (Google Maps or osmdroid) that keeps the same [onCapsuleSelected] callback.
 */
class MockMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Called with the tapped capsule, or null when empty map is tapped. */
    var onCapsuleSelected: ((Capsule?) -> Unit)? = null

    var selectedId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    private var capsules: List<Capsule> = emptyList()

    fun setCapsules(list: List<Capsule>) {
        capsules = list
        invalidate()
    }

    // ---- Map layout (fractions of the view size) ----
    private val streetXs = floatArrayOf(0.12f, 0.34f, 0.58f, 0.82f)
    private val streetYs = floatArrayOf(0.18f, 0.42f, 0.66f, 0.88f)
    private val avenueStart = floatArrayOf(0f, 0.97f)
    private val avenueEnd = floatArrayOf(1f, 0.30f)

    private val dp = resources.displayMetrics.density

    private val streetWidth = 10 * dp
    private val avenueWidth = 18 * dp
    private val pinRadius = 14 * dp
    private val selectedPinRadius = 18 * dp
    private val pinLift = 24 * dp

    // ---- Paints ----
    private fun fill(colorRes: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, colorRes)
        style = Paint.Style.FILL
    }

    private val streetPaint = fill(R.color.map_street)
    private val blockPaint = fill(R.color.map_block)
    private val parkPaint = fill(R.color.map_park)
    private val waterPaint = fill(R.color.map_water)
    private val unlockedPaint = fill(R.color.pin_unlocked)
    private val lockedPaint = fill(R.color.pin_locked)
    private val userPaint = fill(R.color.map_user)
    private val userHaloPaint = fill(R.color.map_user).apply { alpha = 50 }
    private val whitePaint = fill(android.R.color.white)
    private val shadowPaint = fill(android.R.color.black).apply { alpha = 40 }

    private val avenuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_avenue)
        style = Paint.Style.STROKE
        strokeWidth = avenueWidth
        strokeCap = Paint.Cap.ROUND
    }
    private val avenueEdgePaint = Paint(avenuePaint).apply {
        color = ContextCompat.getColor(context, R.color.map_avenue_edge)
        strokeWidth = avenueWidth + 2 * dp
    }
    private val pinOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dp
    }
    private val selectedRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 60
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_label)
        textSize = 11 * dp
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    private val lockIcon = icon(R.drawable.ic_lock)
    private val unlockIcon = icon(R.drawable.ic_lock_open)

    private fun icon(res: Int) = ContextCompat.getDrawable(context, res)!!.mutate().also {
        DrawableCompat.setTint(it, ContextCompat.getColor(context, android.R.color.white))
    }

    private val rect = RectF()
    private val path = Path()

    // ---- Drawing ----
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, streetPaint)
        drawBlocks(canvas, w, h)
        drawAvenue(canvas, w, h)
        drawUser(canvas, MockCapsules.USER_X * w, MockCapsules.USER_Y * h)

        // Draw the selected pin last so it sits on top.
        capsules.filter { it.id != selectedId }.forEach { drawPin(canvas, it, w, h, false) }
        capsules.firstOrNull { it.id == selectedId }?.let { drawPin(canvas, it, w, h, true) }
    }

    private fun drawBlocks(canvas: Canvas, w: Float, h: Float) {
        val xs = floatArrayOf(0f, *streetXs, 1f)
        val ys = floatArrayOf(0f, *streetYs, 1f)
        val half = streetWidth / 2
        val corner = 6 * dp

        for (col in 0 until xs.size - 1) {
            for (row in 0 until ys.size - 1) {
                val left = if (col == 0) -corner else xs[col] * w + half
                val right = if (col == xs.size - 2) w + corner else xs[col + 1] * w - half
                val top = if (row == 0) -corner else ys[row] * h + half
                val bottom = if (row == ys.size - 2) h + corner else ys[row + 1] * h - half
                rect.set(left, top, right, bottom)

                if (col == 1 && row == 2) {
                    // Park block
                    canvas.drawRoundRect(rect, corner, corner, parkPaint)
                    drawLabel(canvas, "CAMPUS GREEN", rect.centerX(), rect.centerY())
                } else {
                    canvas.drawRoundRect(rect, corner, corner, blockPaint)
                }
            }
        }
        // Pond in the bottom-right corner, on top of the blocks
        drawPond(canvas, w, h)
    }

    private fun drawPond(canvas: Canvas, w: Float, h: Float) {
        val left = streetXs[3] * w + streetWidth
        val top = streetYs[2] * h + streetWidth
        rect.set(left, top, w + 20 * dp, h + 20 * dp)
        canvas.drawOval(rect, waterPaint)
        drawLabel(canvas, "MILL POND", (left + w) / 2 + 6 * dp, (top + h) / 2)
    }

    private fun drawAvenue(canvas: Canvas, w: Float, h: Float) {
        val x1 = avenueStart[0] * w
        val y1 = avenueStart[1] * h
        val x2 = avenueEnd[0] * w
        val y2 = avenueEnd[1] * h
        canvas.drawLine(x1, y1, x2, y2, avenueEdgePaint)
        canvas.drawLine(x1, y1, x2, y2, avenuePaint)

        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        canvas.drawTextOnPath("MAIN ST", path, 0f, labelPaint.textSize / 3, labelPaint)
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float) {
        canvas.drawText(text, cx, cy + labelPaint.textSize / 3, labelPaint)
    }

    private fun drawUser(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawCircle(cx, cy, 24 * dp, userHaloPaint)
        canvas.drawCircle(cx, cy, 9 * dp, whitePaint)
        canvas.drawCircle(cx, cy, 6.5f * dp, userPaint)
    }

    private fun drawPin(canvas: Canvas, capsule: Capsule, w: Float, h: Float, selected: Boolean) {
        val tipX = capsule.x * w
        val tipY = capsule.y * h
        val r = if (selected) selectedPinRadius else pinRadius
        val lift = if (selected) pinLift * 1.25f else pinLift
        val cx = tipX
        val cy = tipY - lift
        val unlocked = capsule.isUnlocked()
        val paint = if (unlocked) unlockedPaint else lockedPaint

        // Shadow at the tip
        rect.set(tipX - 7 * dp, tipY - 2.5f * dp, tipX + 7 * dp, tipY + 2.5f * dp)
        canvas.drawOval(rect, shadowPaint)

        // Glow ring behind the selected pin
        if (selected) {
            selectedRingPaint.color = paint.color
            selectedRingPaint.alpha = 60
            canvas.drawCircle(cx, cy, r + 10 * dp, selectedRingPaint)
        }

        // Teardrop: circle head + point down to the tip
        path.reset()
        path.addCircle(cx, cy, r, Path.Direction.CW)
        val tail = Path().apply {
            moveTo(cx - r * 0.72f, cy + r * 0.62f)
            lineTo(tipX, tipY)
            lineTo(cx + r * 0.72f, cy + r * 0.62f)
            close()
        }
        path.op(tail, Path.Op.UNION)
        canvas.drawPath(path, paint)
        canvas.drawPath(path, pinOutlinePaint)

        // Lock icon in the head
        val icon = if (unlocked) unlockIcon else lockIcon
        val half = (r * 0.62f).toInt()
        icon.setBounds(cx.toInt() - half, cy.toInt() - half, cx.toInt() + half, cy.toInt() + half)
        icon.draw(canvas)
    }

    // ---- Touch ----
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                onCapsuleSelected?.invoke(findPinAt(event.x, event.y))
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun findPinAt(x: Float, y: Float): Capsule? {
        val w = width.toFloat()
        val h = height.toFloat()
        val touchSlop = 12 * dp
        // Check the selected pin first since it's drawn on top.
        val ordered = capsules.sortedByDescending { it.id == selectedId }
        return ordered.firstOrNull { c ->
            val selected = c.id == selectedId
            val r = if (selected) selectedPinRadius else pinRadius
            val lift = if (selected) pinLift * 1.25f else pinLift
            val cx = c.x * w
            val cy = c.y * h - lift
            hypot(x - cx, y - cy) <= r + touchSlop ||
                (kotlin.math.abs(x - cx) <= r && y in cy..(c.y * h))
        }
    }
}
