package com.example.util.simpletimetracker.feature_change_reminder.interactor

import com.example.util.simpletimetracker.core.mapper.ChangeReminderViewDataMapper
import com.example.util.simpletimetracker.core.mapper.DayOfWeekViewDataMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.base.CurrentTimestampProvider
import com.example.util.simpletimetracker.domain.extension.toLocalDateTime
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.feature_base_adapter.dayOfWeek.DayOfWeekViewData
import com.example.util.simpletimetracker.feature_change_reminder.R
import com.example.util.simpletimetracker.feature_change_reminder.model.ChangeActivityReminderEditor
import com.example.util.simpletimetracker.feature_change_reminder.model.ChangeActivityReminderEditor.Mode
import com.example.util.simpletimetracker.feature_change_reminder.viewData.ChangeActivityReminderViewData
import com.example.util.simpletimetracker.feature_views.spinner.CustomSpinner
import java.util.TimeZone
import javax.inject.Inject

class ChangeActivityReminderViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val prefsInteractor: PrefsInteractor,
    private val timeMapper: TimeMapper,
    private val dayOfWeekViewDataMapper: DayOfWeekViewDataMapper,
    private val currentTimestampProvider: CurrentTimestampProvider,
    private val changeReminderViewDataMapper: ChangeReminderViewDataMapper,
) {

    val modes = listOf(
        Mode.DISABLED,
        Mode.CUSTOM,
    )

    suspend fun getViewData(
        activity: RecordType?,
        editor: ChangeActivityReminderEditor,
        controlsEnabled: Boolean,
        activitySelectionEnabled: Boolean,
        deleteVisible: Boolean,
    ): ChangeActivityReminderViewData {
        val timeZone = TimeZone.getDefault()
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val date = currentTimestampProvider.get().toLocalDateTime(timeZone).toLocalDate()

        return ChangeActivityReminderViewData(
            activityName = activity?.name
                ?: resourceRepo.getString(R.string.change_record_message_choose_type),
            activitySelectionEnabled = activitySelectionEnabled && controlsEnabled,
            modeItems = mapModeItems(),
            modeSelectedPosition = modes.indexOf(editor.mode),
            customFieldsVisible = editor.mode == Mode.CUSTOM,
            durationText = editor.durationSeconds
                .let(timeMapper::formatDuration),
            recurrent = editor.recurrent,
            daysOfWeek = dayOfWeekViewDataMapper.mapViewData(
                selectedDaysOfWeek = editor.daysOfWeek,
                isDarkTheme = prefsInteractor.getDarkMode(),
                firstDayOfWeek = prefsInteractor.getFirstDayOfWeek(),
                width = DayOfWeekViewData.Width.MatchParent,
                paddingHorizontalDp = 4,
            ),
            doNotDisturbStartText = changeReminderViewDataMapper.formatTimeOfDay(
                millis = editor.doNotDisturbStartMillis,
                useMilitaryTime = useMilitaryTime,
                date = date,
                timeZone = timeZone,
            ),
            doNotDisturbEndText = changeReminderViewDataMapper.formatTimeOfDay(
                millis = editor.doNotDisturbEndMillis,
                useMilitaryTime = useMilitaryTime,
                date = date,
                timeZone = timeZone,
            ),
            controlsEnabled = controlsEnabled,
            deleteVisible = deleteVisible,
        )
    }

    fun mapMode(position: Int): Mode? {
        return modes.getOrNull(position)
    }

    private fun mapModeItems(): List<CustomSpinner.CustomSpinnerTextItem> {
        return modes.map {
            val textRes = when (it) {
                Mode.DISABLED -> R.string.activity_reminder_mode_disabled
                Mode.CUSTOM -> R.string.activity_reminder_mode_custom
            }
            CustomSpinner.CustomSpinnerTextItem(resourceRepo.getString(textRes))
        }
    }
}
