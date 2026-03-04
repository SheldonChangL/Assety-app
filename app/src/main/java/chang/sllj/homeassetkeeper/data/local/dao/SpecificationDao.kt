package chang.sllj.homeassetkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecificationDao {

    /** All specs for one item, in user-defined display order. */
    @Query("SELECT * FROM specifications WHERE itemId = :itemId ORDER BY sortOrder ASC")
    fun getSpecsForItem(itemId: String): Flow<List<SpecificationEntity>>

    /** Insert or update a single spec. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpec(spec: SpecificationEntity)

    /**
     * Bulk replace for the "save form" flow: replaces the entire spec list for an
     * item atomically (caller should delete existing rows first in a transaction).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpecs(specs: List<SpecificationEntity>)

    /** Remove a single spec row. */
    @Delete
    suspend fun deleteSpec(spec: SpecificationEntity)

    /** Remove all specs for an item (used before re-inserting a revised list). */
    @Query("DELETE FROM specifications WHERE itemId = :itemId")
    suspend fun deleteSpecsForItem(itemId: String)
}
