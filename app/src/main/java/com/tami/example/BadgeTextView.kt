package com.tami.example

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
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
            else
                text = notBadgeText
        }

    init {
        setAttribute()
    }

    private fun setAttribute() {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.BadgeTextView, defStyleAttr, 0)

        // Badge Icon
        badgeIcon = typedArray.getDrawable(R.styleable.BadgeTextView_badgeIcon)

        // BadgeShow 여부를 판단한다.
        isShowBadge = typedArray.getBoolean(R.styleable.BadgeTextView_showBadge, false)

        typedArray.recycle()
    }


    private var notBadgeText: CharSequence = ""
    private var badgeText: CharSequence = ""


    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)

        // Badge false 일 때 Badge없는 text를 보여주기 위해서
        text?.let {
            if (it.isNotEmpty())
                notBadgeText = it
        }

        if (isShowBadge)
            submitBadgeIconText()

    }

    private fun submitBadgeIconText() {
        if (isDuplicateText()) return
        doOnLayout {
            post {
                // 같은 Text 중복 여부 확인
                if (isDuplicateText()) return@post

                // 현재 Text의 ellipsis 여부를 확인 후 Ellipsis가 true면 Ellipsis부분을 제외한 Text를 가져온다.
                val checkEllipsisText = if (isEllipsis()) getEllipsisRemoveText() else text

                // adjustCutCount를 구한다.
                val adjustCutCount = getAdjustCutCount(checkEllipsisText)
                val subStringText = subStringAdjustCutCount(checkEllipsisText, adjustCutCount)

                // 계산한 Text에 Badge를 붙인다.
                val isAdjustCutCount = adjustCutCount > 0
                val addBadgeText = addBadgeIcon(isAdjustCutCount, subStringText)
                addBadgeText?.let {
                    badgeText = it
                    text = badgeText
                }
            }
        }
    }

    private fun isDuplicateText(): Boolean {
        if (isInvisible || text == null || text.isEmpty() || badgeText == text) {
            return true
        }
        return false
    }

    /**
     * ... 으로 제외 된 텍스트를 제외하고 값을 리턴한다.
     */
    private fun getEllipsisRemoveText(): CharSequence {
        val ellipsisCount = layout.getEllipsisCount(maxLines - 1)
        return text.substring(0, text.length - ellipsisCount)
    }

    /**
     * Ellipsis 여부를 확인 한다.
     */
    private fun isEllipsis(): Boolean {
        return if (layout != null) {
            layout.getEllipsisCount(maxLines - 1) > 0
        } else false
    }

    private fun subStringAdjustCutCount(text: CharSequence, adjustCutCount: Int): CharSequence {
        return text.substring(0, text.length - adjustCutCount)
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

                // AdjustCutCount 가 있다는건 실제 텍스트에서 ... 공간을 포함 시키기 위해 문자를 잘랐기 때문에 ELLIPSIS_PREFIX 을 붙인다.
                if (isAdjustCutCount)
                    append(ELLIPSIS_PREFIX)

                // ImageSpan은 문자열을 이미지로 변경하기 때문에 ImageSpan용 문자열은 더한다.
                append(IMAGE_SPAN_TEMP_VALUE)
            }

            // Image의 Alignment Q 이상만 Center가 가능하기 때문에 처리 해준다.
            val verticalAlignment =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ImageSpan.ALIGN_CENTER
                } else {
                    ImageSpan.ALIGN_BASELINE
                }


            // ImageSpan Add
            val span = ImageSpan(icon, verticalAlignment)
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
     * 1. 텍스트가 변경되면, 텍스트가 생략되는지 여부를 확인합니다.
     * 2. 텍스트가 생략되었다면, 생략된 부분을 제외하고 텍스트를 가져옵니다.
     * 3. 생략된 부분을 제외한 텍스트의 마지막 줄 텍스트가 표시될 width를 계산합니다.
     * 4. 생략된 부분을 제외한 텍스트와 "..." 그리고 아이콘(ImageIcon)이 합쳐진 width를 계산합니다.
     * 5. 위의 계산에서 얻은 width가 한 줄에 표시할 수 있는 width를 넘으면, 한 줄에 표시할 수 있는 width보다 작아질 때까지 뒤에서부터 한 글자씩 빼면서 계산합니다.
     * 6. 5번에서 한 글자씩 뺀 count를 구합니다.
     * 7. 위의 과정에서 조정된 count만큼 생략된 부분을 제외한 텍스트에 이미지(Image)를 붙입니다.
     *
     *  @param checkEllipsisText Ellipsis 여부를 확인 후 EllipsisText 제외 된 텍스트
     *  @return adjustCutCount
     */
    private fun getAdjustCutCount(checkEllipsisText: CharSequence): Int {

        // MaxLine이 1이면
        val lastLineStartIndex = if (maxLines > 1)
            layout.getLineVisibleEnd(maxLines - 2) else 0

        val compareText = checkEllipsisText.substring(lastLineStartIndex, checkEllipsisText.length)

        val bounds = Rect()
        var adjustCutCount = -1
        do {
            adjustCutCount++

            val subText =
                compareText.substring(0, compareText.length - adjustCutCount) + " "

            val replacedText = subText + ELLIPSIS_PREFIX + IMAGE_SPAN_TEMP_VALUE
            paint.getTextBounds(replacedText, 0, replacedText.length, bounds)
            val replacedTextWidth = bounds.width()
        } while (replacedTextWidth > width)

        return adjustCutCount
    }

    companion object {
        private const val ELLIPSIS_PREFIX = "…"
        private const val IMAGE_SPAN_TEMP_VALUE = "뱃"
    }

}