package com.tehreembabar23hayya.financialhealthauditor

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tehreembabar23hayya.financialhealthauditor.ui.screens.*

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Welcome)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Welcome> {
          WelcomeScreen(
            onNavigateToChooseModules = { backStack.add(ChooseModules) },
            onNavigateToFlaggedIssues = { backStack.add(FlaggedIssues) },
            onNavigateToMonthlySummary = { backStack.add(MonthlySummary) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<ChooseModules> {
          ChooseModulesScreen(
            onBack = { backStack.removeLastOrNull() },
            onNavigateToAddTransaction = { backStack.add(AddTransaction) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<AddTransaction> {
          AddTransactionScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<FlaggedIssues> {
          FlaggedIssuesScreen(
            onBack = { backStack.removeLastOrNull() },
            onNavigateToReviewIssue = { backStack.add(ReviewIssue) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<ReviewIssue> {
          ReviewIssueScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<MonthlySummary> {
          MonthlySummaryScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
      },
  )
}
