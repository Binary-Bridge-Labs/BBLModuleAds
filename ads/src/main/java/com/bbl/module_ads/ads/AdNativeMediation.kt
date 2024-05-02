package com.bbl.module_ads.ads

import com.google.ads.mediation.adcolony.AdColonyMediationAdapter
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.ads.mediation.applovin.AppLovinMediationAdapter
import com.google.ads.mediation.facebook.FacebookMediationAdapter
import com.google.ads.mediation.vungle.VungleMediationAdapter
import com.google.android.gms.ads.nativead.NativeAd

enum class AdNativeMediation(val clazz: Class<*>) {
    ADMOB(AdMobAdapter::class.java),
    FACEBOOK(FacebookMediationAdapter::class.java),
    ADCOLONY(AdColonyMediationAdapter::class.java),
    APPLOVIN(AppLovinMediationAdapter::class.java),
    VUNGLE(VungleMediationAdapter::class.java);

    companion object {
        fun get(nativeAd: NativeAd): AdNativeMediation? {
            val adapterClassName = nativeAd.responseInfo?.mediationAdapterClassName ?: return null
            return values().find { adapterClassName.contains(it.clazz.simpleName) }
        }
    }
}