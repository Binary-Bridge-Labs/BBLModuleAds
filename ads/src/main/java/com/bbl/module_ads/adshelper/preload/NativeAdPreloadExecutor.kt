package com.bbl.module_ads.adshelper.preload

import android.app.Activity
import android.util.Log
import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bbl.module_ads.ads.BBLAd
import com.bbl.module_ads.ads.BBLAdCallback
import com.bbl.module_ads.ads.wrapper.ApAdError
import com.bbl.module_ads.ads.wrapper.ApNativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.bbl.module_ads.adshelper.nativeads.NativeAdCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sealed class representing different states of the native ad preloading process.
 */
sealed class NativePreloadState {

    /**
     * Represents the state where no preloaded native ad is available.
     */
    object None : NativePreloadState()

    /**
     * Represents the state where the native ad preloading process has started.
     */
    object Start : NativePreloadState()

    /**
     * Represents the state where a preloaded native ad is ready to be consumed.
     *
     * @param apNativeAd The [ApNativeAd] representing the preloaded native ad.
     */
    data class Consume(val apNativeAd: ApNativeAd) : NativePreloadState()

    /**
     * Represents the state where the native ad preloading process has been completed.
     */
    object Complete : NativePreloadState()

    /**
     * Represents the state where an error occurred during the native ad preloading process.
     */
    object Error : NativePreloadState()
}

/**
 * Data class representing the parameters for preloading a native ad.
 *
 * @param idAd The unique identifier for the native ad.
 * @param layoutId The resource identifier of the layout to be used for the preloaded native ad.
 */
internal data class NativeAdPreloadParam(
    val idAd: String,
    @LayoutRes val layoutId: Int,
)

/**
 * Internal class responsible for preloading native ads.
 *
 * @property param The parameters for preloading native ads.
 */
internal class NativeAdPreloadExecutor(val param: NativeAdPreloadParam) {
    private val nativeAdCallback = NativeAdCallback()

    private val adPreloadState: MutableStateFlow<NativePreloadState> =
        MutableStateFlow(NativePreloadState.None)
    private val adPreloadLiveData: MutableLiveData<NativePreloadState> =
        MutableLiveData(NativePreloadState.None)

    private val queueNativeAd: ArrayDeque<ApNativeAd> = ArrayDeque()
    private val inProgress = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val counter = AtomicInteger(0)
    private var action: (NativePreloadState) -> Unit = {}
    private val totalBuffer: AtomicInteger = AtomicInteger(0)

    /**
     * Polls and removes the first preloaded native ad from the queue.
     *
     * @return The preloaded native ad or null if the queue is empty.
     */
    fun pollAdNative(): ApNativeAd? {
        return queueNativeAd.removeFirstOrNull().also {
            logD("POLL => $it")
            logSizeQueue()
        }
    }

    /**
     * Gets the first preloaded native ad without removing it from the queue.
     *
     * @return The first preloaded native ad or null if the queue is empty.
     */
    fun getAdNative(): ApNativeAd? {
        return queueNativeAd.firstOrNull().also {
            logD("GET => $it")
            logSizeQueue()
        }
    }

    /**
     * Polls or awaits the first preloaded native ad. If the queue is empty,
     * it waits until a native ad is available or the preloading process completes.
     *
     * @return The preloaded native ad or null if no ad is available.
     */
    suspend fun pollOrAwaitAdNative(): ApNativeAd? {
        return if (queueNativeAd.isEmpty()) {
            if (isInProgress()) {
                adPreloadState.map { state ->
                    when (state) {
                        is NativePreloadState.Consume, is NativePreloadState.Complete -> pollAdNative()
                        is NativePreloadState.None, is NativePreloadState.Start, is NativePreloadState.Error -> null
                    }
                }.firstOrNull()
            } else null
        } else {
            pollAdNative()
        }
    }

    /**
     * Gets or awaits the first preloaded native ad. If the queue is empty,
     * it waits until a native ad is available or the preloading process completes.
     *
     * @return The preloaded native ad or null if no ad is available.
     */
    suspend fun getOrAwaitAdNative(): ApNativeAd? {
        return if (queueNativeAd.isEmpty()) {
            if (isInProgress()) {
                adPreloadState.map { state ->
                    when (state) {
                        is NativePreloadState.Consume, is NativePreloadState.Complete -> getAdNative()
                        is NativePreloadState.None, is NativePreloadState.Start, is NativePreloadState.Error -> null
                    }
                }.firstOrNull()
            } else null
        } else {
            pollAdNative()
        }
    }

    /**
     * Checks if the native ad preloading process is in progress.
     *
     * @return True if the preloading process is in progress, false otherwise.
     */
    fun isInProgress(): Boolean {
        return inProgress.get()
    }

    /**
     * Checks if there are preloaded native ads available.
     *
     * @return True if there are preloaded native ads or the preloading process is in progress, false otherwise.
     */
    fun isPreloadAvailable(): Boolean {
        return isInProgress() || queueNativeAd.isNotEmpty()
    }

    /**
     * Gets the buffer of preloaded native ads.
     *
     * @return The list of preloaded native ads.
     */
    fun getNativeAdBuffer(): List<ApNativeAd> {
        return queueNativeAd.toList()
    }

    /**
     * Requests native ads for preloading.
     *
     * @param activity The activity where the preloading is initiated.
     * @param buffer The number of native ads to preload.
     */
    private fun requestAd(activity: Activity, buffer: Int) {
        logD("requestAd: adId:${param.idAd} - layoutId:${param.layoutId} - buffer:$buffer")
        totalBuffer.addAndGet(buffer)
        inProgress.set(true)
        if (totalBuffer.get() == 0) {
            action(NativePreloadState.Start)
        }
        repeat(buffer) {
            coroutineScope.launch {
                BBLAd.getInstance().loadNativeAdResultCallback(
                    activity,
                    param.idAd,
                    param.layoutId,
                    nativeAdCallback.invokeListenerAdCallback(getDefaultNativeCallback())
                )
            }
        }
    }

    fun registerAdCallback(adCallback: BBLAdCallback) {
        nativeAdCallback.registerAdListener(adCallback)
    }

    fun unregisterAdCallback(adCallback: BBLAdCallback) {
        nativeAdCallback.unregisterAdListener(adCallback)
    }

    fun getNativeAdCallback(): NativeAdCallback {
        return nativeAdCallback
    }

    private fun getDefaultNativeCallback(): BBLAdCallback {
        return object : BBLAdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (counter.get() <= totalBuffer.get()) {
                    queueNativeAd.addLast(nativeAd)
                    logSizeQueue()
                    action(NativePreloadState.Consume(nativeAd))
                    if (counter.incrementAndGet() == totalBuffer.get()) {
                        action(NativePreloadState.Complete)
                        inProgress.set(false)
                    }
                }
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                if (counter.get() <= totalBuffer.get()) {
                    if (counter.incrementAndGet() == totalBuffer.get()) {
                        action(NativePreloadState.Complete)
                        inProgress.set(false)
                    }
                }
            }
        }
    }

    fun execute(activity: Activity, buffer: Int) {
        action = { state ->
            Log.d("${TAG}_STATE", "state execute =>> $state")
            adPreloadLiveData.postValue(state)
            adPreloadState.update { state }
        }
        requestAd(activity, buffer)
    }

    fun getAdPreloadState(): StateFlow<NativePreloadState> {
        return adPreloadState.asStateFlow()
    }

    fun getAdPreloadLiveData(): LiveData<NativePreloadState> {
        return adPreloadLiveData
    }

    fun release() {
        queueNativeAd.clear()
        adPreloadState.update { NativePreloadState.None }
        adPreloadLiveData.postValue(NativePreloadState.None)
    }

    private fun logD(message: String) {
        Log.d("${TAG}_INFO", messageLog(message))
    }

    private fun logSizeQueue() {
        logD("size of queue: ${queueNativeAd.size}")
    }

    private fun messageLog(message: String): String {
        return "[INFO] $message"
    }

    companion object {
        private const val TAG = "NativeAdPreload"
    }
}