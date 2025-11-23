package com.example.nutrih.ui.specialists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.nutrih.R
import com.example.nutrih.databinding.DialogBookAppointmentBinding
import com.example.nutrih.domain.Appointment
import java.util.Calendar

class BookAppointmentDialogFragment(
    private val specialistName: String,
    private val specialistSpecialty: String,
    private val onSave: (Appointment) -> Unit
) : DialogFragment() {

    private var _binding: DialogBookAppointmentBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: Calendar = Calendar.getInstance()

    private val timeSlots = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBookAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        generateTimeSlots()
        setupNumberPicker()
        setupListeners()

        validateDate(selectedDate)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupView() {
        binding.tvDialogSubtitle.text = getString(R.string.dialog_schedule_subtitle, specialistName)
        binding.calendarView.minDate = System.currentTimeMillis() - 1000
    }

    private fun generateTimeSlots() {
        timeSlots.clear()
        val startHour = 8
        val endHour = 16
        val intervalMinutes = 30

        var currentHour = startHour
        var currentMinute = 0

        while (currentHour < endHour || (currentHour == endHour && currentMinute == 0)) {
            val timeString = String.format("%02d:%02d", currentHour, currentMinute)
            timeSlots.add(timeString)

            currentMinute += intervalMinutes
            if (currentMinute >= 60) {
                currentHour++
                currentMinute = 0
            }
        }
    }

    private fun setupNumberPicker() {
        binding.numberPickerTime.minValue = 0
        binding.numberPickerTime.maxValue = timeSlots.size - 1
        binding.numberPickerTime.displayedValues = timeSlots.toTypedArray()
        binding.numberPickerTime.wrapSelectorWheel = false
    }

    private fun setupListeners() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val tempDate = Calendar.getInstance()
            tempDate.set(year, month, dayOfMonth)
            selectedDate = tempDate

            validateDate(tempDate)
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnConfirm.setOnClickListener { saveAppointment() }
    }

    private fun validateDate(date: Calendar) {
        val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
        val month = date.get(Calendar.MONTH)

        var errorMessage: String? = null

        if (dayOfWeek == Calendar.SUNDAY) {
            errorMessage = "Não realizamos consultas aos domingos."
        } else if (isHoliday(dayOfMonth, month)) {
            errorMessage = "Não atendemos em feriados."
        }

        if (errorMessage != null) {
            binding.layoutTimeSelection.visibility = View.GONE
            binding.tvErrorMessage.text = errorMessage
            binding.tvErrorMessage.visibility = View.VISIBLE
        } else {
            binding.layoutTimeSelection.visibility = View.VISIBLE
            binding.tvErrorMessage.visibility = View.GONE
        }
    }

    private fun isHoliday(day: Int, month: Int): Boolean {
        val holidays = listOf(
            Pair(1, 0), Pair(21, 3), Pair(1, 4), Pair(7, 8),
            Pair(12, 9), Pair(2, 10), Pair(15, 10), Pair(25, 11)
        )
        return holidays.contains(Pair(day, month))
    }

    private fun saveAppointment() {
        if (binding.layoutTimeSelection.visibility != View.VISIBLE) {
            return
        }

        val selectedIndex = binding.numberPickerTime.value
        val timeString = timeSlots[selectedIndex]

        val (hour, minute) = timeString.split(":").map { it.toInt() }
        selectedDate.set(Calendar.HOUR_OF_DAY, hour)
        selectedDate.set(Calendar.MINUTE, minute)

        val finalTimestamp = selectedDate.timeInMillis
        val appointment = Appointment(
            id = 0,
            specialistName = specialistName,
            specialistSpecialty = specialistSpecialty,
            timestamp = finalTimestamp
        )

        onSave(appointment)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}