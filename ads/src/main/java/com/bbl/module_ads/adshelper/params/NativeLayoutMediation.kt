package com.bbl.module_ads.adshelper.params

import androidx.annotation.LayoutRes
import com.bbl.module_ads.ads.AdNativeMediation

data class NativeLayoutMediation(
    val mediationType: AdNativeMediation,
    @LayoutRes
    val layoutId: Int
)