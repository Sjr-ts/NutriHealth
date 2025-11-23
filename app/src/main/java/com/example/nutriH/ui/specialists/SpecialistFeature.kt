package com.example.nutrih.ui.specialists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrih.R
import com.example.nutrih.databinding.FragmentSpecialistsBinding
import com.example.nutrih.di.ServiceLocator
import com.example.nutrih.domain.Appointment
import com.example.nutrih.domain.IAppointmentRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpecialistFragment : Fragment() {
    private var _binding: FragmentSpecialistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpecialistViewModel by viewModels {
        SpecialistViewModelFactory(requireContext().applicationContext)
    }
    private lateinit var appointmentAdapter: AppointmentAdapter
    private var hasActiveAppointment = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpecialistsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAppointmentsRecyclerView()
        observeViewModel()
        setupStaticSpecialists()
    }
    private fun setupAppointmentsRecyclerView() {
        appointmentAdapter = AppointmentAdapter { appointment ->
            viewModel.cancelAppointment(appointment)
            Toast.makeText(requireContext(), "Agendamento cancelado.", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewAppointments.apply {
            adapter = appointmentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    private fun observeViewModel() {
        viewModel.allAppointments.observe(viewLifecycleOwner) { appointments ->
            appointmentAdapter.submitList(appointments)
            hasActiveAppointment = appointments.isNotEmpty()
            binding.tvNoAppointments.isVisible = appointments.isEmpty()
            binding.recyclerViewAppointments.isVisible = appointments.isNotEmpty()
        }
    }
    private fun setupStaticSpecialists() {
        val spec1Name = "Dr. Carlos Andrade"
        val spec1Esp = "Nutricionista Esportivo"
        binding.specialist1.tvSpecialistName.text = spec1Name
        binding.specialist1.tvSpecialistSpecialty.text = spec1Esp
        binding.specialist1.btnSchedule.setOnClickListener { trySchedule(spec1Name, spec1Esp) }
        val spec2Name = "Dra. Beatriz Lima"
        val spec2Esp = "Nutrição Clínica"
        binding.specialist2.tvSpecialistName.text = spec2Name
        binding.specialist2.tvSpecialistSpecialty.text = spec2Esp
        binding.specialist2.btnSchedule.setOnClickListener { trySchedule(spec2Name, spec2Esp) }
    }
    private fun trySchedule(name: String, specialty: String) {
        if (hasActiveAppointment) {
            Toast.makeText(requireContext(), "Você já possui um agendamento ativo.", Toast.LENGTH_LONG).show()
        } else {
            showBookingDialog(name, specialty)
        }
    }
    private fun showBookingDialog(name: String, specialty: String) {
        val dialog = BookAppointmentDialogFragment(name, specialty) { appointment ->
            viewModel.bookAppointment(appointment)
            showConfirmationMessage(name)
        }
        dialog.show(childFragmentManager, "BookAppointmentDialog")
    }
    private fun showConfirmationMessage(doctorName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agendamento Solicitado!")
            .setMessage("Sua solicitação foi enviada.\n\nO $doctorName entrará em contato em breve.")
            .setPositiveButton("Entendi", null)
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SpecialistViewModel(private val repository: IAppointmentRepository) : ViewModel() {
    val allAppointments: LiveData<List<Appointment>> = repository.allAppointments
    fun bookAppointment(appointment: Appointment) = viewModelScope.launch { repository.bookAppointment(appointment) }
    fun cancelAppointment(appointment: Appointment) = viewModelScope.launch { repository.cancelAppointment(appointment) }
}

class SpecialistViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpecialistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpecialistViewModel(ServiceLocator.provideAppointmentRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AppointmentAdapter(private val onCancelClick: (Appointment) -> Unit) : ListAdapter<Appointment, AppointmentAdapter.AppointmentViewHolder>(AppointmentComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }
    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position), onCancelClick)
    }
    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_appointment_title)
        private val date: TextView = itemView.findViewById(R.id.tv_appointment_date)
        private val time: TextView = itemView.findViewById(R.id.tv_appointment_time)
        private val btnCancel: Button = itemView.findViewById(R.id.btn_cancel_appointment)
        private val dateFormatter = SimpleDateFormat("EEE, dd 'de' MMM.", Locale("pt", "BR"))
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
        fun bind(appointment: Appointment, onCancelClick: (Appointment) -> Unit) {
            val appointmentDate = Date(appointment.timestamp)
            title.text = "Consulta com ${appointment.specialistName}"
            date.text = dateFormatter.format(appointmentDate).replaceFirstChar { it.titlecase() }
            time.text = timeFormatter.format(appointmentDate)
            btnCancel.setOnClickListener { onCancelClick(appointment) }
        }
    }
    class AppointmentComparator : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment) = oldItem == newItem
    }
}