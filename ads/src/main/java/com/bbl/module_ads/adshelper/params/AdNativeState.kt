package com.bbl.module_ads.adshelper.params

import com.bbl.module_ads.ads.wrapper.ApNativeAd

sealed class AdNativeState {
    object None : AdNativeState()
    object Fail : AdNativeState()
    object Loading : AdNativeState()
    object Cancel : AdNativeState()
    data class Loaded(val adNative: ApNativeAd) : AdNativeState()
}