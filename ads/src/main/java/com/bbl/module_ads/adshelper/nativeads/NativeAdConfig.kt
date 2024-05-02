package com.bbl.module_ads.adshelper.nativeads

import android.util.Log
import androidx.annotation.LayoutRes
import com.bbl.module_ads.ads.AdNativeMediation
import com.google.android.gms.ads.nativead.NativeAd
import com.bbl.module_ads.adshelper.params.NativeLayoutMediation

class NativeAdConfig(
    override val idAds: String,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean,
    @LayoutRes val layoutId: Int,
) : IAdsConfig {

    var listLayoutByMediation: List<NativeLayoutMediation> = emptyList()
        private set

    fun setLayoutMediation(vararg layoutMediation: NativeLayoutMediation) = apply {
        this.listLayoutByMediation = layoutMediation.toList()
    }

    fun setLayoutMediation(listLayoutMediation: List<NativeLayoutMediation>) = apply {
        this.listLayoutByMediation = listLayoutMediation
    }

    @LayoutRes
    fun getLayoutIdByMediationNativeAd(nativeAd: NativeAd?): Int {
        val listLayout = listLayoutByMediation

        return if (listLayout.isEmpty() || nativeAd == null) {
            layoutId
        } else {
            val currentMediation = AdNativeMediation.get(nativeAd)
            listLayout.find { currentMediation == it.mediationType }?.also {
                Log.d("NativeAdHelper", "show with mediation ${it.mediationType.name}")
            }?.layoutId ?: layoutId
        }
    }
}
