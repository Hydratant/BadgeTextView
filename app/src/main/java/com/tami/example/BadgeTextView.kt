package com.tami.example

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
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
                val adjustCount = getAdjustCutCount()
                val isEllipsis = adjustCount > 0

                Log.i("BadgeTextView", "isEllipsis : ${isEllipsis()}")

                val beforeText = if (isEllipsis) getEllipsisRemoveText(adjustCount)
                else text

                val addBadgeText = addBadgeIcon(isEllipsis, beforeText)
                addBadgeText?.let {
                    updateText = it
                    text = updateText
                }
            }
        }
    }

    /**
     * ... 으로 제외 된 텍스트를 제외하고 값을 리턴한다.
     */
    private fun getEllipsisRemoveText(adjustCount: Int): CharSequence {
        val startIndex = 0
        val maxTextIndex = layout.getLineVisibleEnd(maxLines - 1)

        val endIndex =
            maxTextIndex - adjustCount

        return text.substring(startIndex, endIndex)
    }

    fun isEllipsis(): Boolean {
        return if (layout != null) {
            layout.getEllipsisCount(lineCount - 1) > 0
        } else false
    }


    /**
     * BadgeIcon을 추가한다.
     * @return BadgeIcon이 추가 된 값을 반환한다.
     */
    private fun addBadgeIcon(isEllipsis: Boolean, text: CharSequence): CharSequence? {

        badgeIcon?.let { icon ->
            // BadgeIcon Size 조절 (Text Size로 크기 조정)
            icon.setBounds(0, 0, textSize.toInt(), textSize.toInt())

            // BadgeIcon 붙이기 전 String 값
            val beforeAddBadgeIconString = buildString {
                append(text)

                // Ellipsis 여부를 확인 후 Prefix를 붙인다.잘리지 않는 글자면 Prefix를 붙이면 안된다.
                if (isEllipsis)
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

    /**
     *
     * 1. 맨 마지막줄 텍스트를 가져옴
     * 2. 맨 마지막줄 텍스트가 표시될 width 계산
     * 3. 맨 마지막줄 텍스트 + … + ImageIcon 텍스트가 합쳐졌을때 width 계산
     * 4. 3번의 width가 한줄에 표시할수 있는 width를 넘으면
     * 5. 한줄에 표시할 수 있는 width보다 작아질때까지 뒤에서부터 한글자씩 빼면서 계산
     * 6. 4번에서 한글자씩 뺀 count를 구함
     * 7. 위의 과정에서 조정된 count만큼 실제 원본 텍스트에서 뺀뒤에 더보기 텍스트 붙임
     */
    private fun getAdjustCutCount(): Int {

        // 1. 맨 마지막줄 텍스트를 가져온다.
        val lastLineStartIndex = layout.getLineVisibleEnd(maxLines - 2)
        val lastLineEndIndex = layout.getLineVisibleEnd(maxLines - 1)
        val lastLineText = text.substring(lastLineStartIndex, lastLineEndIndex)

        val bounds = Rect()
        paint.getTextBounds(lastLineText, 0, lastLineText.length, bounds)

        var adjustCutCount = -1
        do {
            adjustCutCount++
            val subText =
                lastLineText.substring(0, lastLineText.length - adjustCutCount)
            val replacedText = subText + ELLIPSIS_PREFIX + IMAGE_SPAN_TEMP_VALUE
            paint.getTextBounds(replacedText, 0, replacedText.length, bounds)
            val replacedTextWidth = bounds.width()
        } while (replacedTextWidth > width)

        return adjustCutCount
    }
    // 1. ellisis 제외된 텍스트를 가져온다.
    // 2. 제외 된 텍스트 길이와 ...뱃을 더한 길이를 비교한다.
    // 3. ...뱃을 더한 길이가 낮아질때가지 갯수를 카운팅한다.


    companion object {
        private const val ELLIPSIS_PREFIX = "..."
        private const val IMAGE_SPAN_TEMP_VALUE = "뱃"
    }

}