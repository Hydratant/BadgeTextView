package com.tami.example

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible


class BadgeTextView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    // BadgeIcon
    private var badgeIcon: Drawable? = null
    var isShowBadge: Boolean = false
        set(value) {
            field = value
            if (value)
                submitBadgeIconText()
        }

    init {
        setAttribute()
    }

    private fun setAttribute() {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.BadgeTextView, defStyleAttr, 0)

        badgeIcon = typedArray.getDrawable(R.styleable.BadgeTextView_badgeIcon)
        isShowBadge = typedArray.getBoolean(R.styleable.BadgeTextView_showBadge, false)

        typedArray.recycle()
    }


    private var updateText: CharSequence = ""


    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        if (isShowBadge)
            submitBadgeIconText()
    }

    private fun submitBadgeIconText() {
        if (isInvisible || text == null || text.isEmpty() || updateText == text) {
            return
        }
        doOnLayout {
            post {
                // 1. 현재 Text의 ellipsis 여부를 확인 후 Ellipsis가 true면 Ellipsis부분을 제외한 Text를 가져온다.
                // 2. adjustCuntCount를  확인한다.
                // 3. substring

                val removeText = if (isEllipsis()) getEllipsisRemoveText() else text
                val adJustCount = getAdjustCutCount(removeText.toString())

                val subStringText = removeText.substring(0, removeText.length - adJustCount)
                val isAdjustCutCount = adJustCount > 0
                val addBadgeText = addBadgeIcon(isAdjustCutCount, subStringText)
                addBadgeText?.let {
                    updateText = it
                    text = updateText
                }
            }
        }
    }

    private fun getAdjustCutCount(removeText: String): Int {



        val start = layout.getLineStart(maxLines - 2)
        val lastLineStartIndex = layout.getLineVisibleEnd(maxLines - 2)

        // Compare를 구한다.
        val compareFirstText = text.substring(start, lastLineStartIndex)

        val compareBounds = Rect()
        paint.getTextBounds(compareFirstText, 0, compareFirstText.length, compareBounds)

        val compareText = removeText.substring(lastLineStartIndex, removeText.length)

        val replaceTextBounds = Rect()
        var adjustCutCount = -1
        do {
            adjustCutCount++
            val subText =
                compareText.substring(0, compareText.length - adjustCutCount)

            val replacedText = subText + ELLIPSIS_PREFIX + IMAGE_SPAN_TEMP_VALUE
            paint.getTextBounds(replacedText, 0, replacedText.length, replaceTextBounds)

            val replacedTextWidth = replaceTextBounds.width()
        } while (replacedTextWidth > compareBounds.width())

        return adjustCutCount
    }

    /**
     * ... 으로 제외 된 텍스트를 제외하고 값을 리턴한다.
     */
    private fun getEllipsisRemoveText(): CharSequence {
        val ellipsisCount = layout.getEllipsisCount(maxLines - 1)
        return text.substring(0, text.length - ellipsisCount)
    }

    private fun isEllipsis(): Boolean {
        return if (layout != null) {
            layout.getEllipsisCount(lineCount - 1) > 0
        } else false
    }


    /**
     * BadgeIcon을 추가한다.
     * @return BadgeIcon이 추가 된 값을 반환한다.
     */
    private fun addBadgeIcon(isAdjustCutCount: Boolean, text: CharSequence): CharSequence? {

        badgeIcon?.let { icon ->
            // BadgeIcon Size 조절 (Text Size로 크기 조정)
            icon.setBounds(0, 0, textSize.toInt(), textSize.toInt())

            // BadgeIcon 붙이기 전 String 값
            val beforeAddBadgeIconString = buildString {
                append(text)

                // Ellipsis 여부를 확인 후 Prefix를 붙인다.잘리지 않는 글자면 Prefix를 붙이면 안된다.
                if (isAdjustCutCount)
                    append(ELLIPSIS_PREFIX)

                append(IMAGE_SPAN_TEMP_VALUE)
            }

            // ImageSpan Add
            val span = ImageSpan(icon, ImageSpan.ALIGN_BASELINE)
            val spannedString = buildSpannedString {
                append(beforeAddBadgeIconString)
                setSpan(
                    span,
                    beforeAddBadgeIconString.lastIndex,
                    beforeAddBadgeIconString.lastIndex + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannedString
        }
        return null
    }

    companion object {
        private const val ELLIPSIS_PREFIX = "..."
        private const val IMAGE_SPAN_TEMP_VALUE = "뱃"
    }

}