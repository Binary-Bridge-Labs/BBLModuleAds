package com.bbl.module_ads.adshelper.preload

import androidx.annotation.IntRange


/**
 * Represents options for preloading native ads.
 *
 * @property preloadAfterShow Indicates whether preloading should occur after showing an ad.
 *                           Default value: false.
 * @property preloadBuffer    Specifies the preload buffer size, an integer in the range from 1 to 10.
 *                           Default value: 1.
 * @property preloadOnResume  Indicates whether preloading should occur when the application resumes.
 *                           Default value: true.
 *
 * ```
 * val option = NativeAdPreloadClientOption.builder()
 *     .setPreloadAfterShow(true)
 *     .setPreloadBuffer(3)
 *     .setPreloadOnResume(false)
 *     .build()
 *
 * nativeAdHelper.setPreloadAdOption(option)
 *```
 */
data class NativeAdPreloadClientOption(
    val preloadAfterShow: Boolean = false,
    @IntRange(from = 1, to = 10)
    val preloadBuffer: Int = 1,
    val preloadOnResume: Boolean = true
) {
    /**
     * Builder class for constructing instances of [NativeAdPreloadClientOption].
     */
    class Builder {
        private var client = NativeAdPreloadClientOption()
        /**
         * Sets the [preloadAfterShow] property.
         *
         * @param preloadAfterShow Indicates whether preloading should occur after showing an ad.
         * @return the [Builder] instance for method chaining.
         */
        fun setPreloadAfterShow(preloadAfterShow: Boolean) = apply {
            client = client.copy(preloadAfterShow = preloadAfterShow)
        }
        /**
         * Sets the [preloadBuffer] property with the specified integer value.
         *
         * @param preloadBuffer Specifies the preload buffer size (1 to 10).
         * @return the [Builder] instance for method chaining.
         */
        fun setPreloadBuffer(@IntRange(from = 1, to = 10) preloadBuffer: Int) = apply {
            client = client.copy(preloadBuffer = preloadBuffer)
        }
        /**
         * Sets the [preloadOnResume] property.
         *
         * @param preloadOnResume Indicates whether preloading should occur when the application resumes.
         * @return the [Builder] instance for method chaining.
         */
        fun setPreloadOnResume(preloadOnResume: Boolean) = apply {
            client = client.copy(preloadOnResume = preloadOnResume)
        }
        /**
         * Constructs and returns an instance of [NativeAdPreloadClientOption] with the configured values.
         *
         * @return An instance of [NativeAdPreloadClientOption].
         */
        fun build(): NativeAdPreloadClientOption {
            return client
        }
    }

    companion object {
        /**
         * Static method that returns a new instance of the [Builder] class for creating instances
         * of [NativeAdPreloadClientOption].
         *
         * @return A new instance of the [Builder] class.
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
