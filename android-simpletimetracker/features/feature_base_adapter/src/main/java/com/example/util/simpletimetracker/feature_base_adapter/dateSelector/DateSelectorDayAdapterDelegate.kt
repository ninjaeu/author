package com.example.util.simpletimetracker.feature_base_adapter.dateSelector

import android.animation.Animator
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.util.simpletimetracker.feature_base_adapter.InfiniteRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.R
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.animateAlphaWithAnimator
import com.example.util.simpletimetracker.feature_views.extension.animateTextSize
import com.example.util.simpletimetracker.feature_views.extension.pxToDp
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.extension.setOnLongClick
import com.example.util.simpletimetracker.feature_views.extension.setRounded
import com.example.util.simpletimetracker.feature_views.extension.setTextOptional
import kotlin.reflect.KMutableProperty0
import com.example.util.simpletimetracker.feature_base_adapter.dateSelector.DateSelectorDayViewData as ViewData
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemDateDaySelectorBinding as Binding

fun createDateSelectorDayAdapterDelegate(
    onItemClick: ((ViewData) -> Unit),
    onItemLongClick: ((ViewData) -> Unit),
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData
        val animationState = tvDateSelectorAdditionalHint.getDateSelectorAnimationState()
        val animateSelection = animationState.shouldAnimateSelection(
            position = item.position,
            isSelected = item.cardData.isSelected,
        )

        setTestTag(root, item)
        setAdditionalHint(
            dayMonth = item.dayMonth,
            additionalText = tvDateSelectorAdditionalHint,
            animateSelection = animateSelection,
            animator = animationState::additionalHintAnimator,
        )
        setDayMoth(
            dayMonth = item.dayMonth,
            topText = tvDateSelectorTopText,
            bottomText = tvDateSelectorBottomText,
            increasedTextSize = item.cardData.increasedTextSize,
            animateSelection = animateSelection,
            topTextAnimator = animationState::topTextAnimator,
            bottomTextAnimator = animationState::bottomTextAnimator,
        )
        root.setCardData(
            cardData = item.cardData,
            viewSelected = viewDateSelectorBackgroundSelected,
            viewToday = viewDateSelectorBackgroundToday,
            viewClickable = viewDateSelectorClickable,
            textViews = listOf(
                tvDateSelectorTopText,
                tvDateSelectorBottomText,
            ),
        )

        root.setOnClickWith(item, onItemClick)
        root.setOnLongClick { onItemLongClick(item) }

        animationState.onBound(
            position = item.position,
            isSelected = item.cardData.isSelected,
        )
    }
}

internal class DateSelectorAnimationState {
    var additionalHintAnimator: Animator? = null
    var topTextAnimator: Animator? = null
    var bottomTextAnimator: Animator? = null
    var topText2Animator: Animator? = null
    var bottomText2Animator: Animator? = null

    private var boundPosition: Int? = null
    private var wasSelected: Boolean = false

    fun shouldAnimateSelection(
        position: Int,
        isSelected: Boolean,
    ): Boolean {
        return isSelected && (boundPosition != position || !wasSelected)
    }

    fun onBound(
        position: Int,
        isSelected: Boolean,
    ) {
        boundPosition = position
        wasSelected = isSelected
    }
}

private fun View.getAnimationDuration(): Long {
    return (resources.getInteger(android.R.integer.config_shortAnimTime) * 0.75f).toLong()
}

internal fun TextView.getDateSelectorAnimationState(): DateSelectorAnimationState {
    return (tag as? DateSelectorAnimationState)
        ?: DateSelectorAnimationState().also { tag = it }
}

internal fun setTestTag(
    root: View,
    data: InfiniteRecyclerAdapter.Data,
) {
    root.tag = InfiniteRecyclerAdapter.TEST_TAG + data.position
}

internal fun setAdditionalHint(
    dayMonth: ViewData.DayMonth,
    additionalText: TextView,
    animateSelection: Boolean,
    animator: KMutableProperty0<Animator?>,
) {
    additionalText.text = dayMonth.additionalHint
    if (dayMonth.additionalHint.isNotBlank()) {
        additionalText.isVisible = true
        if (animateSelection) {
            animator.get()?.cancel()
            additionalText.alpha = 0f
            val duration = additionalText.getAnimationDuration()
            animator.set(additionalText.animateAlphaWithAnimator(isVisible = true, duration = duration))
        } else if (animator.get()?.isRunning != true) {
            additionalText.alpha = 1f
        }
    } else {
        animator.get()?.cancel()
        animator.set(null)
        additionalText.isVisible = false
        additionalText.alpha = 0f
    }
}

internal fun setDayMoth(
    dayMonth: ViewData.DayMonth,
    topText: TextView,
    bottomText: TextView,
    increasedTextSize: Boolean,
    animateSelection: Boolean,
    topTextAnimator: KMutableProperty0<Animator?>,
    bottomTextAnimator: KMutableProperty0<Animator?>,
) {
    topText.setTextOptional(dayMonth.topText)
    bottomText.text = dayMonth.bottomText

    if (increasedTextSize) {
        if (animateSelection) {
            topTextAnimator.get()?.cancel()
            bottomTextAnimator.get()?.cancel()
            topText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            bottomText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val duration = topText.getAnimationDuration()
            topTextAnimator.set(topText.animateTextSize(14f, duration = duration))
            bottomTextAnimator.set(bottomText.animateTextSize(18f, duration = duration))
        } else {
            if (topTextAnimator.get()?.isRunning != true) {
                topText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            }
            if (bottomTextAnimator.get()?.isRunning != true) {
                bottomText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }
        }
    } else {
        topTextAnimator.get()?.cancel()
        bottomTextAnimator.get()?.cancel()
        topTextAnimator.set(null)
        bottomTextAnimator.set(null)
        topText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        bottomText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    }
}

internal fun View.setCardData(
    cardData: ViewData.CardData,
    viewSelected: View,
    viewToday: View,
    viewClickable: View,
    textViews: List<View>,
) {
    val cornerRadius = resources.getDimensionPixelSize(R.dimen.record_type_card_corner_radius)
    viewClickable.setRounded(cornerRadius.pxToDp())

    val textAlpha = if (cardData.isFuture) 0.4f else 1.0f
    textViews.forEach { it.alpha = textAlpha }

    viewSelected.isVisible = cardData.isSelected
    viewToday.isVisible = cardData.isToday
}

data class DateSelectorDayViewData(
    override val position: Int,
    val dayMonth: DayMonth,
    val cardData: CardData,
) : InfiniteRecyclerAdapter.Data {

    data class DayMonth(
        val additionalHint: String,
        val topText: String,
        val bottomText: String,
    )

    data class CardData(
        val isToday: Boolean,
        val isSelected: Boolean,
        val isFuture: Boolean,
        val increasedTextSize: Boolean,
    )

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}
