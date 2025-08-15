package com.combo.plugin.sample.common.di

import com.combo.plugin.sample.common.navigation.AppComposeNavigator
import com.combo.plugin.sample.common.navigation.AppScreen
import com.combo.plugin.sample.common.navigation.IHubComposeNavigator
import org.koin.dsl.module

val navigationModule =
    module {
        single { IHubComposeNavigator() }

        single<AppComposeNavigator<AppScreen>> { get() }
    }
