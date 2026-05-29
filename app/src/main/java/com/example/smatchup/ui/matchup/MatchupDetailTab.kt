package com.example.smatchup.ui.matchup

import androidx.annotation.StringRes
import com.example.smatchup.R

enum class MatchupDetailTab(@StringRes val labelRes: Int) {
    GAMEPLAN(R.string.mu_tab_gameplan),
    STRONG(R.string.mu_tab_strong),
    PUNISH(R.string.mu_tab_punish),
    STAGES(R.string.mu_tab_stages),
    VIDEO(R.string.mu_tab_video),
}
