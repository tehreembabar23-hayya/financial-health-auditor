package com.tehreembabar23hayya.financialhealthauditor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorProfileDao {
    @Query("SELECT * FROM behavior_profiles")
    fun getAllProfiles(): Flow<List<BehaviorProfile>>

    @Query("SELECT * FROM behavior_profiles WHERE merchant = :merchant LIMIT 1")
    suspend fun getProfileForMerchant(merchant: String): BehaviorProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BehaviorProfile)

    @Delete
    suspend fun deleteProfile(profile: BehaviorProfile)
}
