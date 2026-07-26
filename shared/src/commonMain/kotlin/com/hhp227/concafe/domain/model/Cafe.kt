package com.hhp227.concafe.domain.model

data class Cafe(
    val id: String,
    val name: String,
    val desc: String,
    val region: Region,
    val thumbnailImage: String?,
    val ratingAvg: Double,
    val reviewCount: Int,
    val approved: Boolean,
    val conceptType: String,
    val ownerIds: List<String> = emptyList(),
    val socialMedia: Map<String, String> = emptyMap(),
    val reservationUrl: String? = null,
    val tableCounts: TableCounts = TableCounts(),
    val favoriteCount: Int = 0
)

data class TableCounts(
    val current: Int = 0,
    val total: Int = 0
)

enum class ConceptType(val displayNameKo: String) {
    MAID("메이드"),
    BUTLER("집사"),
    IDOL("아이돌"),
    DEVIL("악마"),
    DOLL("인형"),
    COSPLAY("코스프레"),
    NAMJANG("남장"),
    YOKAI("요괴"),
    CAT("고양이"),
    OTHER("기타");

    companion object {
        fun fromRaw(value: String?): ConceptType? {
            val normalized = value
                ?.trim()
                ?.uppercase()
                ?.replace("-", "_")
                ?.replace(" ", "_")
                .orEmpty()

            return entries.firstOrNull { it.name == normalized }
        }
    }
}
