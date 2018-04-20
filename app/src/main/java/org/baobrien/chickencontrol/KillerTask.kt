package org.baobrien.chickencontrol

import android.os.AsyncTask
import android.util.Log

class KillerTask<T>(val task: () -> T, val onSuccess: (T) -> Any = {}, val onFailed: (Exception?) -> Any = {}) : AsyncTask<Void, Void, T>() {

    private var exception: Exception? = null

    companion object {
        private val TAG = "KillerTask"
    }

    /**
     * Override AsyncTask's function doInBackground
     */
    override fun doInBackground(vararg params: Void): T? {
        try {
            return run { task() }
        } catch (e: Exception) {
            exception = e
            return null
        }
    }

    /**
     * Override AsyncTask's function onPostExecute
     */
    override fun onPostExecute(result: T) {
        if (!isCancelled) { // task not cancelled
            if (exception != null) { // fail
                run { onFailed(exception) }
            } else { // success
                run { onSuccess(result) }
            }
        } else { // task cancelled
            run { onFailed(RuntimeException("Task was cancelled")) }
        }
    }

    /**
     * Execute AsyncTask
     */
    fun go() {
        execute()
    }

    /**
     * Cancel AsyncTask
     */
    fun cancel() {
        cancel(true)
    }

}
