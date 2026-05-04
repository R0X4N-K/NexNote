package com.example.nexnote.domain.repository

import com.example.nexnote.domain.model.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    val allTemplates: Flow<List<Template>>

    suspend fun getTemplateById(id: Long): Template?
    suspend fun saveTemplate(template: Template): Long
    suspend fun deleteTemplate(template: Template)
}
