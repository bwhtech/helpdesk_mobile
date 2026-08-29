package com.example.helpdeskanalytics.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpdeskanalytics.domain.model.report.ReportTemplate
import com.example.helpdeskanalytics.domain.repository.ReportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReportTemplatesViewModel(
    private val reportRepository: ReportRepository
) : ViewModel() {

    val templates: StateFlow<List<ReportTemplate>> = reportRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun duplicateTemplate(template: ReportTemplate) {
        val config = template.config ?: return
        viewModelScope.launch {
            reportRepository.saveTemplate(copyName(template.name), config, null)
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { reportRepository.deleteTemplate(id) }
    }

    private fun copyName(name: String): String {
        val taken = templates.value.map { it.name }.toSet()
        val base = "$name copy"
        if (base !in taken) return base
        return generateSequence(2) { it + 1 }.map { "$base $it" }.first { it !in taken }
    }
}
