package dev.counterline.core.engine.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.counterline.core.engine.StockfishEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideStockfishEngine(): StockfishEngine = StockfishEngine()
}
