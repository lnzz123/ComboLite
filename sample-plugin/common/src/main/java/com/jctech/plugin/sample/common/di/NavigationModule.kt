package com.jctech.plugin.sample.common.di

import com.jctech.plugin.sample.common.navigation.AppComposeNavigator
import com.jctech.plugin.sample.common.navigation.AppScreen
import com.jctech.plugin.sample.common.navigation.IHubComposeNavigator
import org.koin.dsl.module

val navigationModule = module {
    single { IHubComposeNavigator() }

    single<AppComposeNavigator<AppScreen>> { get() }
}
