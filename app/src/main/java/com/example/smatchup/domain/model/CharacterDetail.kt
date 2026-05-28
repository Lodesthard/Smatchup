package com.example.smatchup.domain.model

data class CharacterDetail(
    val character: Character,
    val combos: List<String> = emptyList(),
    val gameplan: String? = null,
    val movesUtility: Map<String, String> = emptyMap(),
    val framedata: List<Move> = emptyList(),
    val framedataSource: FramedataSource = FramedataSource.NONE,
    val synergyPartners: List<Character> = emptyList(),
    val stagesBan: List<Stage> = emptyList(),
    val stagesCounterpick: List<Stage> = emptyList(),
    val bestPlayer: Player? = null,
    val lastVideoUrl: String? = null,
    val lastVideoTitle: String? = null,
)

enum class FramedataSource { NONE, REMOTE, BUNDLED }
