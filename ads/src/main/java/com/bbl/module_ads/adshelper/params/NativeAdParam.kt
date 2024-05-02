package com.bbl.module_ads.adshelper.params

import com.bbl.module_ads.ads.wrapper.ApNativeAd
import com.bbl.module_ads.adshelper.IAdsParam

sealed class NativeAdParam : IAdsParam {
    data class Ready(val nativeAd: ApNativeAd) : NativeAdParam()
    sealed class Request : NativeAdParam() {
        object CreateRequest : Request()
        object ResumeRequest : Request()

        companion object {
            @JvmStatic
            fun create(): Request {
                return CreateRequest
            }
        }
    }
}
