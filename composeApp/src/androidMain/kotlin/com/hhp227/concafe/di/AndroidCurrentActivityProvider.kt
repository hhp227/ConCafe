package com.hhp227.concafe.di

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

internal class AndroidCurrentActivityProvider(
    application: Application
) : Application.ActivityLifecycleCallbacks {
    private var currentActivityRef: WeakReference<Activity>? = null

    private fun updateCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    private fun clearCurrentActivity(activity: Activity) {
        val current = currentActivityRef?.get()

        if (current === activity) {
            currentActivityRef = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        updateCurrentActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        updateCurrentActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        clearCurrentActivity(activity)
    }

    fun getCurrentActivity(): Activity? {
        return currentActivityRef?.get()
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
    }
}
