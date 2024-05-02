package com.bbl.module_ads.adshelper.nativeads

import com.bbl.module_ads.ads.BBLAdCallback
import com.bbl.module_ads.ads.wrapper.ApAdError
import com.bbl.module_ads.ads.wrapper.ApInterstitialAd
import com.bbl.module_ads.ads.wrapper.ApNativeAd
import com.bbl.module_ads.ads.wrapper.ApRewardItem
import java.util.concurrent.CopyOnWriteArrayList

internal class NativeAdCallback {
    private val listAdCallback: CopyOnWriteArrayList<BBLAdCallback> = CopyOnWriteArrayList()

    fun registerAdListener(adCallback: BBLAdCallback) {
        this.listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: BBLAdCallback) {
        this.listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        this.listAdCallback.clear()
    }

    private fun invokeAdListener(action: (adCallback: BBLAdCallback) -> Unit) {
        listAdCallback.forEach(action)
    }

    fun invokeListenerAdCallback(internalAdCallback: BBLAdCallback? = null): BBLAdCallback {
        return object : BBLAdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                internalAdCallback?.onNextAction()
                invokeAdListener { it.onNextAction() }
            }

            override fun onAdClosed() {
                super.onAdClosed()
                internalAdCallback?.onAdClosed()
                invokeAdListener { it.onAdClosed() }
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                internalAdCallback?.onAdFailedToLoad(adError)
                invokeAdListener { it.onAdFailedToLoad(adError) }
            }

            override fun onAdFailedToShow(adError: ApAdError?) {
                super.onAdFailedToShow(adError)
                internalAdCallback?.onAdFailedToShow(adError)
                invokeAdListener { it.onAdFailedToShow(adError) }
            }

            override fun onAdLeftApplication() {
                super.onAdLeftApplication()
                internalAdCallback?.onAdLeftApplication()
                invokeAdListener { it.onAdLeftApplication() }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                internalAdCallback?.onAdLoaded()
                invokeAdListener { it.onAdLoaded() }
            }

            override fun onAdSplashReady() {
                super.onAdSplashReady()
                internalAdCallback?.onAdSplashReady()
                invokeAdListener { it.onAdSplashReady() }
            }

            override fun onInterstitialLoad(interstitialAd: ApInterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                internalAdCallback?.onInterstitialLoad(interstitialAd)
                invokeAdListener { it.onInterstitialLoad(interstitialAd) }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                internalAdCallback?.onAdClicked()
                invokeAdListener { it.onAdClicked() }
            }

            override fun onAdImpression() {
                super.onAdImpression()
                internalAdCallback?.onAdImpression()
                invokeAdListener { it.onAdImpression() }
            }

            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                internalAdCallback?.onNativeAdLoaded(nativeAd)
                invokeAdListener { it.onNativeAdLoaded(nativeAd) }
            }

            override fun onUserEarnedReward(rewardItem: ApRewardItem) {
                super.onUserEarnedReward(rewardItem)
                internalAdCallback?.onUserEarnedReward(rewardItem)
                invokeAdListener { it.onUserEarnedReward(rewardItem) }
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                internalAdCallback?.onInterstitialShow()
                invokeAdListener { it.onInterstitialShow() }
            }
        }
    }
}