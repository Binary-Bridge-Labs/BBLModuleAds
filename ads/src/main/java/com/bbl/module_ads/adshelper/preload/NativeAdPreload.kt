package com.bbl.module_ads.adshelper.preload

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.annotation.LayoutRes
import com.bbl.module_ads.ads.BBLAdCallback
import com.bbl.module_ads.ads.wrapper.ApNativeAd
import com.bbl.module_ads.adshelper.nativeads.NativeAdCallback
import com.bbl.module_ads.billing.AppPurchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NativeAdPreload private constructor() {
    data class KeyPreload(val adId: String, val layoutId: Int)

    private val executors: HashMap<KeyPreload, NativeAdPreloadExecutor> = hashMapOf()
    fun preload(activity: Activity, adId: String, @LayoutRes layoutId: Int, buffer: Int) {
        if (!canRequestLoad(activity)) {
            Log.d("NativeAdPreload", "Do not preload because canRequestLoad = false")
            return
        }
        val keyPreload = KeyPreload(adId, layoutId)
        val executor = executors[keyPreload] ?: NativeAdPreloadExecutor(
            NativeAdPreloadParam(idAd = adId, layoutId = layoutId)
        )
        executors[keyPreload] = executor
        executor.execute(activity, buffer)
    }

    fun canRequestLoad(context: Context): Boolean {
        return !AppPurchase.getInstance().isPurchased
                && isOnline(context)
    }

    fun preload(activity: Activity, adId: String, @LayoutRes layoutId: Int) {
        preload(activity, adId, layoutId, 1)
    }

    fun getAdPreloadState(adId: String, @LayoutRes layoutId: Int): StateFlow<NativePreloadState> {
        return executors[KeyPreload(adId, layoutId)]?.getAdPreloadState()
            ?: MutableStateFlow(NativePreloadState.None)
    }

    fun pollAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? {
        return executors[KeyPreload(adId, layoutId)]?.pollAdNative()
    }

    suspend fun pollOrAwaitAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? {
        return executors[KeyPreload(adId, layoutId)]?.pollOrAwaitAdNative()
    }

    fun getAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? {
        return executors[KeyPreload(adId, layoutId)]?.getAdNative()
    }

    suspend fun getOrAwaitAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? {
        return executors[KeyPreload(adId, layoutId)]?.getOrAwaitAdNative()
    }

    fun isPreloadAvailable(adId: String, @LayoutRes layoutId: Int): Boolean {
        return executors[KeyPreload(adId, layoutId)]?.isPreloadAvailable() == true
    }

    fun isPreloadInProcess(adId: String, @LayoutRes layoutId: Int): Boolean {
        return executors[KeyPreload(adId, layoutId)]?.isInProgress() == true
    }

    fun getNativeAdBuffer(adId: String, @LayoutRes layoutId: Int): List<ApNativeAd> {
        return executors[KeyPreload(adId, layoutId)]?.getNativeAdBuffer() ?: emptyList()
    }

    fun registerAdCallback(adId: String,@LayoutRes layoutId: Int, adCallback: BBLAdCallback) {
        executors[KeyPreload(adId, layoutId)]?.registerAdCallback(adCallback)
    }

    fun unRegisterAdCallback(adId: String,@LayoutRes layoutId: Int, adCallback: BBLAdCallback) {
        executors[KeyPreload(adId, layoutId)]?.unregisterAdCallback(adCallback)
    }

    internal fun getNativeAdCallback(adId: String, @LayoutRes layoutId: Int): NativeAdCallback? {
        return executors[KeyPreload(adId, layoutId)]?.getNativeAdCallback()
    }

    private fun isOnline(context: Context): Boolean {
        val netInfo = kotlin.runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        }.getOrNull()
        return netInfo != null && netInfo.isConnected
    }

    companion object {
        @Volatile
        private var _instance: NativeAdPreload? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): NativeAdPreload {
            return synchronized(this) {
                _instance ?: NativeAdPreload().also { _instance = it }
            }
        }
    }
}