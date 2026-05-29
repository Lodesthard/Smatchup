package com.example.smatchup.ui.character

import androidx.annotation.StringRes
import com.example.smatchup.R

enum class DetailTab(@StringRes val labelRes: Int) {
    COMBOS(R.string.tab_combos),
    GAMEPLAN(R.string.tab_gameplan),
    MOVES(R.string.tab_moves),
    FRAME(R.string.tab_frame),
    SYNERGY(R.string.tab_synergy),
    STAGES(R.string.tab_stages),
    VIDEO(R.string.tab_video),
}
