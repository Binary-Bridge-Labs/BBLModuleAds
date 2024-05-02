package com.bbl.module_ads.adshelper.nativeads

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bbl.module_ads.ads.BBLAd
import com.bbl.module_ads.ads.BBLAdCallback
import com.bbl.module_ads.ads.wrapper.ApAdError
import com.bbl.module_ads.ads.wrapper.ApNativeAd
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.bbl.module_ads.adshelper.params.AdNativeState
import com.bbl.module_ads.adshelper.params.NativeAdParam
import com.bbl.module_ads.adshelper.preload.NativeAdPreload
import com.bbl.module_ads.adshelper.preload.NativeAdPreloadClientOption
import java.util.concurrent.atomic.AtomicInteger

class NativeAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: NativeAdConfig
) : AdsHelper<NativeAdConfig, NativeAdParam>(activity, lifecycleOwner, config) {
    companion object {
        const val TAG = "NativeAdHelper"
    }

    private val nativeAdCallback = NativeAdCallback()
    private val preload = NativeAdPreload.getInstance()
    private val adNativeState: MutableStateFlow<AdNativeState> =
        MutableStateFlow(if (canRequestAds()) AdNativeState.None else AdNativeState.Fail)
    private val resumeCount: AtomicInteger = AtomicInteger(0)
    private var shimmerLayoutView: ShimmerFrameLayout? = null
    private var nativeContentView: FrameLayout? = null
    private val lifecycleNativeCallback = nativeAdCallback.invokeListenerAdCallback()
    var adVisibility: AdOptionVisibility = AdOptionVisibility.INVISIBLE
    var nativeAd: ApNativeAd? = null
        private set
    var isEnablePreload: Boolean = false
        private set
    var preloadClientOption: NativeAdPreloadClientOption = NativeAdPreloadClientOption()
        private set

    init {
        lifecycleEventState.onEach {
            when (it) {
                Lifecycle.Event.ON_START -> {
                    preload.getNativeAdCallback(config.idAds, config.layoutId)
                        ?.registerAdListener(lifecycleNativeCallback)
                }

                Lifecycle.Event.ON_STOP -> {
                    preload.getNativeAdCallback(config.idAds, config.layoutId)
                        ?.unregisterAdListener(lifecycleNativeCallback)
                }

                else -> Unit
            }
            if (it == Lifecycle.Event.ON_CREATE) {
                if (!canRequestAds()) {
                    nativeContentView?.checkAdVisibility(false)
                    shimmerLayoutView?.checkAdVisibility(false)
                }
            }
            if (it == Lifecycle.Event.ON_RESUME) {
                if (!canShowAds() && isActiveState()) {
                    cancel()
                }
            }
        }.launchIn(lifecycleOwner.lifecycleScope)
        //Request when resume
        lifecycleEventState.debounce(300).onEach { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount.incrementAndGet()
                logZ("Resume repeat ${resumeCount.get()} times")
            }
            if (event == Lifecycle.Event.ON_RESUME
                && resumeCount.get() > 1
                && nativeAd != null
                && canRequestAds()
                && canReloadAd()
                && isActiveState()
            ) {
                requestAds(NativeAdParam.Request.ResumeRequest)
            }
        }.launchIn(lifecycleOwner.lifecycleScope)
        //for action resume or init
        adNativeState
            .onEach { logZ("adNativeState(${it::class.java.simpleName})") }
            .launchIn(lifecycleOwner.lifecycleScope)

        adNativeState.onEach { adsParam ->
            handleShowAds(adsParam)
        }.launchIn(lifecycleOwner.lifecycleScope)
    }

    fun setShimmerLayoutView(shimmerLayoutView: ShimmerFrameLayout) = apply {
        kotlin.runCatching {
            this.shimmerLayoutView = shimmerLayoutView
            if (lifecycleOwner.lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    shimmerLayoutView.checkAdVisibility(false)
                }
            }
        }
    }

    fun setNativeContentView(nativeContentView: FrameLayout) = apply {
        kotlin.runCatching {
            this.nativeContentView = nativeContentView
            if (lifecycleOwner.lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    nativeContentView.checkAdVisibility(false)
                }
            }
        }
    }

    fun setEnablePreload(isEnable: Boolean) = apply {
        isEnablePreload = isEnable
    }

    fun setPreloadAdOption(option: NativeAdPreloadClientOption) = apply {
        this.preloadClientOption = option
    }

    private fun handleShowAds(adsParam: AdNativeState) {
        nativeContentView?.checkAdVisibility(adsParam !is AdNativeState.Cancel && canShowAds())
        shimmerLayoutView?.checkAdVisibility(adsParam is AdNativeState.Loading && nativeAd == null)
        when (adsParam) {
            is AdNativeState.Loaded -> {
                if (nativeContentView != null && shimmerLayoutView != null) {
                    BBLAd.getInstance().populateNativeAdView(
                        activity,
                        adsParam.adNative,
                        nativeContentView,
                        shimmerLayoutView
                    )
                }
                ensureEnablePreload { option ->
                    if (option.preloadAfterShow) {
                        if (preload.getNativeAdBuffer(config.idAds, config.layoutId).isEmpty()) {
                            preload.preload(
                                activity,
                                config.idAds,
                                config.layoutId,
                                option.preloadBuffer
                            )
                        }
                    }
                }
            }

            else -> Unit
        }
    }

    private fun ensureEnablePreload(block: (option: NativeAdPreloadClientOption) -> Unit) {
        if (isEnablePreload) {
            block(preloadClientOption)
        }
    }

    @Deprecated("Using cancel()")
    fun resetState() {
        logZ("resetState()")
        cancel()
    }

    fun getAdNativeState(): StateFlow<AdNativeState> {
        return adNativeState.asStateFlow()
    }

    fun getNativeAdConfig(): NativeAdConfig {
        return config
    }

    private fun createNativeAds(activity: Activity) {
        if (canRequestAds()) {
            adNativeState.update { AdNativeState.Loading }
            lifecycleOwner.lifecycleScope.launch {
                BBLAd.getInstance().loadNativeAdResultCallback(
                    activity,
                    config.idAds,
                    config.layoutId,
                    nativeAdCallback.invokeListenerAdCallback(getDefaultCallback())
                )
                logZ("createNativeAds")
            }
        }
    }

    private fun createOrGetAdPreload(activity: Activity) {
        if (canRequestAds()) {
            if (isEnablePreload && preload.isPreloadAvailable(config.idAds, config.layoutId)) {
                lifecycleOwner.lifecycleScope.launch {
                    val nativeAd = preload.pollOrAwaitAdNative(config.idAds, config.layoutId)
                    if (nativeAd != null) {
                        logZ("pollOrAwaitAdNative")
                        if (preload.getNativeAdBuffer(config.idAds, config.layoutId).isEmpty()) {
                            adNativeState.update { AdNativeState.Loading }
                        }
                        setAndUpdateNativeLoaded(nativeAd)
                    } else {
                        createNativeAds(activity)
                    }
                }
            } else {
                createNativeAds(activity)
            }
        }
    }

    /**
     * Set and update the state when Native Ads are loaded.
     *
     * @param nativeAd The loaded [ApNativeAd] instance.
     */
    private fun setAndUpdateNativeLoaded(nativeAd: ApNativeAd) {
        nativeAd.layoutCustomNative = config.getLayoutIdByMediationNativeAd(nativeAd.admobNativeAd)
        this@NativeAdHelper.nativeAd = nativeAd
        lifecycleOwner.lifecycleScope.launch {
            adNativeState.emit(AdNativeState.Loaded(nativeAd))
        }
    }

    private fun getDefaultCallback(): BBLAdCallback {
        return object : BBLAdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (isActiveState()) {
                    setAndUpdateNativeLoaded(nativeAd)
                    logZ("onNativeAdLoaded")
                } else {
                    logInterruptExecute("onNativeAdLoaded")
                }
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                Log.v(TAG, "onAdFailedToLoad : ${config.idAds}")
                if (isActiveState()) {
                    if (nativeAd == null) {
                        lifecycleOwner.lifecycleScope.launch {
                            adNativeState.emit(AdNativeState.Fail)
                        }
                    }
                    logZ("onAdFailedToLoad")
                } else {
                    logInterruptExecute("onAdFailedToLoad")
                }
            }

            override fun onAdImpression() {
                super.onAdImpression()
                Log.v(TAG, "onAdImpression : ${config.idAds}")
            }
        }
    }

    override fun requestAds(param: NativeAdParam) {
        lifecycleOwner.lifecycleScope.launch {
            if (canRequestAds()) {
                logZ("requestAds($param)")
                when (param) {
                    is NativeAdParam.Request -> {
                        flagActive.compareAndSet(false, true)
                        when (param) {
                            is NativeAdParam.Request.CreateRequest -> {
                                createOrGetAdPreload(activity)
                            }

                            is NativeAdParam.Request.ResumeRequest -> {
                                if (isEnablePreload && preloadClientOption.preloadOnResume) {
                                    createOrGetAdPreload(activity)
                                } else {
                                    createNativeAds(activity)
                                }
                            }
                        }

                    }

                    is NativeAdParam.Ready -> {
                        flagActive.compareAndSet(false, true)
                        adNativeState.update { AdNativeState.Loading }
                        setAndUpdateNativeLoaded(param.nativeAd)
                    }
                }
            } else {
                if (!isOnline() && nativeAd == null) {
                    cancel()
                }
            }
        }
    }

    override fun cancel() {
        logZ("cancel() called")
        flagActive.compareAndSet(true, false)
        lifecycleOwner.lifecycleScope.launch {
            adNativeState.emit(AdNativeState.Cancel)
        }
    }

    fun registerAdListener(adCallback: BBLAdCallback) {
        nativeAdCallback.registerAdListener(adCallback)
    }

    fun unregisterAdListener(adCallback: BBLAdCallback) {
        nativeAdCallback.unregisterAdListener(adCallback)
    }

    fun unregisterAllAdListener() {
        nativeAdCallback.unregisterAllAdListener()
    }

    private fun View.checkAdVisibility(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE
        else when (adVisibility) {
            AdOptionVisibility.GONE -> View.GONE
            AdOptionVisibility.INVISIBLE -> View.INVISIBLE
        }
    }
}
