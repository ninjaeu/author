package com.example.util.simpletimetracker.feature_base_adapter.dateSelector

import com.example.util.simpletimetracker.feature_base_adapter.InfiniteRecyclerAdapter
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_views.extension.setOnLongClick
import com.example.util.simpletimetracker.feature_base_adapter.databinding.ItemDateRangeSelectorBinding as Binding
import com.example.util.simpletimetracker.feature_base_adapter.dateSelector.DateSelectorRangeViewData as ViewData

fun createDateSelectorRangeAdapterDelegate(
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
            dayMonth = item.dayMonth1,
            additionalText = tvDateSelectorAdditionalHint,
            animateSelection = animateSelection,
            animator = animationState::additionalHintAnimator,
        )
        setDayMoth(
            dayMonth = item.dayMonth1,
            topText = tvDateSelectorTopText1,
            bottomText = tvDateSelectorBottomText1,
            increasedTextSize = item.cardData.increasedTextSize,
            animateSelection = animateSelection,
            topTextAnimator = animationState::topTextAnimator,
            bottomTextAnimator = animationState::bottomTextAnimator,
        )
        setDayMoth(
            dayMonth = item.dayMonth2,
            topText = tvDateSelectorTopText2,
            bottomText = tvDateSelectorBottomText2,
            increasedTextSize = item.cardData.increasedTextSize,
            animateSelection = animateSelection,
            topTextAnimator = animationState::topText2Animator,
            bottomTextAnimator = animationState::bottomText2Animator,
        )
        root.setCardData(
            cardData = item.cardData,
            viewSelected = viewDateSelectorBackgroundSelected,
            viewToday = viewDateSelectorBackgroundToday,
            viewClickable = viewDateSelectorClickable,
            textViews = listOf(
                tvDateSelectorTopText1,
                tvDateSelectorBottomText1,
                tvDateSelectorTopText2,
                tvDateSelectorBottomText2,
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

data class DateSelectorRangeViewData(
    override val position: Int,
    val dayMonth1: DateSelectorDayViewData.DayMonth,
    val dayMonth2: DateSelectorDayViewData.DayMonth,
    val cardData: DateSelectorDayViewData.CardData,
) : InfiniteRecyclerAdapter.Data {

    override fun isValidType(other: ViewHolderType): Boolean = other is ViewData
}
