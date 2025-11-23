package com.example.nutrih.ui.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.nutrih.R

class PlanFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val textView = TextView(requireContext()).apply {
            text = "Tela de Plano (WIP)"
            textSize = 24f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(16, 16, 16, 16)
        }
        return textView
    }
}