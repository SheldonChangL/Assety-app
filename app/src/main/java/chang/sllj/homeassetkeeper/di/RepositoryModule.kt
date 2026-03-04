package chang.sllj.homeassetkeeper.di

import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import chang.sllj.homeassetkeeper.data.repository.ItemRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the [ItemRepository] interface to its [ItemRepositoryImpl] concrete class.
 *
 * Using @Binds (instead of @Provides) is more efficient: Hilt generates a direct
 * delegation rather than a wrapper factory, and the binding is validated at
 * compile time by the Hilt/Dagger annotation processor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository
}
