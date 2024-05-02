package com.bbl.module_ads.adshelper.nativeads

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.bbl.module_ads.adshelper.IAdsParam
import com.bbl.module_ads.billing.AppPurchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

abstract class AdsHelper<C : IAdsConfig, P : IAdsParam>(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: C
) {
    private var tag: String = context::class.java.simpleName
    internal val flagActive: AtomicBoolean = AtomicBoolean(false)
    internal val lifecycleEventState = MutableStateFlow(Lifecycle.Event.ON_ANY)
    var flagUserEnableReload = true
        set(value) {
            field = value
            logZ("setFlagUserEnableReload($field)")
        }

    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                lifecycleEventState.update { event }
                when (event) {
                    Lifecycle.Event.ON_DESTROY -> {
                        lifecycleOwner.lifecycle.removeObserver(this)
                    }

                    else -> Unit
                }
            }
        })
    }

    /**
     * Determines whether ads can be shown based on purchase status, configuration, and consent.
     *
     * @return True if ads can be shown, false otherwise.
     */
    open fun canShowAds(): Boolean {
        return !AppPurchase.getInstance().isPurchased && config.canShowAds
    }

    /**
     * Determines whether ads can be requested based on the ability to show ads and network connectivity.
     *
     * @return True if ads can be requested, false otherwise.
     */
    open fun canRequestAds(): Boolean {
        return canShowAds() && isOnline()
    }

    /**
     * Abstract method to be implemented by subclasses for requesting ads.
     *
     * @param param The parameters for the ad request, implementing [IAdsParam].
     */
    abstract fun requestAds(param: P)

    /**
     * Abstract method to be implemented by subclasses for canceling the ad request.
     */
    abstract fun cancel()

    fun setTagForDebug(tag: String) {
        this.tag = tag
    }

    /**
     * Checks whether the AdsHelper is in an active state.
     *
     * @return True if the AdsHelper is active, false otherwise.
     */
    fun isActiveState(): Boolean {
        return flagActive.get()
    }

    /**
     * Checks whether ad reloading is allowed based on configuration and user preferences.
     *
     * @return True if ad reloading is allowed, false otherwise.
     */
    fun canReloadAd(): Boolean {
        return config.canReloadAds && flagUserEnableReload
    }

    internal fun logZ(message: String) {
        Log.d(this::class.java.simpleName, "${tag}: $message")
    }

    /**
     * Internal method for logging interruptions due to cancellation.
     *
     * @param message The message indicating that the operation was not executed due to cancellation.
     */
    internal fun logInterruptExecute(message: String) {
        logZ("$message not execute because has called cancel()")
    }

    internal fun isOnline(): Boolean {
        val netInfo = kotlin.runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        }.getOrNull()
        return netInfo != null && netInfo.isConnected
    }
}

/**
 * Interface representing the configuration for handling advertisements in the application.
 */
interface IAdsConfig {
    /**
     * A unique identifier for the advertisements.
     * This identifier should correspond to the specific ad content or source.
     */
    val idAds: String

    /**
     * A boolean flag indicating whether the application is allowed to display advertisements.
     * If set to `true`, the application can show ads; otherwise, it should refrain from displaying them.
     */
    val canShowAds: Boolean

    /**
     * A boolean flag specifying whether the application is permitted to reload or refresh advertisements.
     * If set to `true`, the application can request a reload of ad content as needed.
     */
    val canReloadAds: Boolean
}

/**
 * Enum representing the visibility options for advertising elements.
 */
enum class AdOptionVisibility {
    /**
     * The advertising element is not visible and does not occupy any space in the layout.
     */
    GONE,

    /**
     * The advertising element is invisible but still occupies space in the layout.
     */
    INVISIBLE
}