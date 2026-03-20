package com.hhp227.concafe.data.source

interface SocialDataSource {
    val favoriteCafeIdsByUser: MutableMap<String, MutableSet<String>>
    val followedCastIdsByUser: MutableMap<String, MutableSet<String>>
    val favoriteUserIdsByCafeId: MutableMap<String, MutableSet<String>>
    val followerUserIdsByCastId: MutableMap<String, MutableSet<String>>
}