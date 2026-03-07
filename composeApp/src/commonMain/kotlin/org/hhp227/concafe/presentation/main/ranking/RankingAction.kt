package org.hhp227.concafe.presentation.main.ranking

import org.hhp227.concafe.domain.model.RankingPeriod

sealed interface RankingAction {
    data class ChangeTab(val tab: RankingUiState.TabType) : RankingAction
    data class ChangePeriod(val period: RankingPeriod) : RankingAction
    data class ChangeRegion(val region: RankingUiState.RegionFilter) : RankingAction
    data class SelectAd(val index: Int) : RankingAction
    data class ClickMaid(val id: String) : RankingAction
    data class ClickCafe(val id: String) : RankingAction
}
