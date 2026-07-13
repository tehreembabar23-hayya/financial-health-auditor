package com.tehreembabar23hayya.financialhealthauditor.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BillHistoryDao {
    @Query("SELECT * FROM bill_histories ORDER BY billDate DESC")
    fun getAllBillHistories(): Flow<List<BillHistory>>

    @Query("SELECT * FROM bill_histories WHERE providerName = :providerName ORDER BY billDate DESC")
    fun getBillHistoriesForProvider(providerName: String): Flow<List<BillHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillHistory(billHistory: BillHistory)

    @Delete
    suspend fun deleteBillHistory(billHistory: BillHistory)
}
