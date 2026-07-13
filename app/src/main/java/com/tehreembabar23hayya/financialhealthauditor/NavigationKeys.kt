package com.tehreembabar23hayya.financialhealthauditor

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Welcome : NavKey
@Serializable data object ChooseModules : NavKey
@Serializable data object AddTransaction : NavKey
@Serializable data object FlaggedIssues : NavKey
@Serializable data object ReviewIssue : NavKey
@Serializable data object MonthlySummary : NavKey
