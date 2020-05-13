package com.absinthe.anywhere_.utils.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.handler.URLSchemeHandler
import com.absinthe.anywhere_.utils.manager.ActivityStackManager.topActivity
import com.absinthe.anywhere_.utils.manager.DialogManager.showCheckShizukuWorkingDialog
import com.absinthe.anywhere_.utils.manager.DialogManager.showGotoShizukuManagerDialog
import com.blankj.utilcode.util.IntentUtils
import com.blankj.utilcode.util.PermissionUtils
import moe.shizuku.api.ShizukuApiConstants
import moe.shizuku.api.ShizukuClientHelper
import moe.shizuku.api.ShizukuClientHelperPre23
import moe.shizuku.api.ShizukuService
import timber.log.Timber

/**
 * Shizuku Helper
 *
 *
 * Init Shizuku API.
 */
object ShizukuHelper {

    private const val ACTION_SEND_BINDER = "moe.shizuku.client.intent.action.SEND_BINDER"

    private var isShizukuV3Failed = false
    private var isShizukuV3TokenValid = false

    @SuppressLint("LogNotTimber")
    fun bind(context: Context) {
        ShizukuClientHelper.setBinderReceivedListener {
            Log.d("ShizukuHelper", "onBinderReceived")

            if (ShizukuService.getBinder() == null) {
                // ShizukuBinderReceiveProvider started without binder, should never happened
                Log.e("ShizukuHelper", "binder is null")
                isShizukuV3Failed = true
            } else {
                try {
                    // test the binder first
                    ShizukuService.pingBinder()

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        val token = ShizukuClientHelperPre23.loadPre23Token(context)
                        isShizukuV3TokenValid = ShizukuService.setTokenPre23(token)
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_SEND_BINDER))
                } catch (tr: Throwable) {
                    // blocked by SELinux or server dead, should never happened
                    Log.e("ShizukuHelper", "can't contact with remote", tr)
                    isShizukuV3Failed = true
                }
            }
        }
    }

    /**
     * check whether shizuku service is on working
     *
     * @param context to show a dialog
     */
    fun checkShizukuOnWorking(context: Context): Boolean {
        // Shizuku v3 service will send binder via Content Provider to this process when this activity comes foreground.

        // Wait a few seconds here for binder
        if (!ShizukuService.pingBinder()) {
            if (isShizukuV3Failed) {
                // provider started with no binder included, binder calls blocked by SELinux or server dead, should never happened
                // notify user
                ToastUtil.makeText("provider started with no binder included.")
            }

            // Shizuku v3 may not running, notify user
            Timber.e("Shizuku v3 may not running.")
            showCheckShizukuWorkingDialog(context)
            // if your app support Shizuku v2, run old v2 codes here
            // for new apps, recommended to ignore v2
        } else {
            // Shizuku v3 binder received
            return true
        }
        return false
    }

    val isGrantShizukuPermission: Boolean
        get() = PermissionUtils.isGranted(ShizukuApiConstants.PERMISSION)

    fun requestShizukuPermission() {
        if (isGrantShizukuPermission) {
            return
        }
        val activity = topActivity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // on API 23+, Shizuku v3 uses runtime permission
            if (!isGrantShizukuPermission) {
                if (com.absinthe.anywhere_.utils.PermissionUtils.isMIUI) {
                    showPermissionDialog(activity)
                } else {
                    activity.requestPermissions(arrayOf(ShizukuApiConstants.PERMISSION), Const.REQUEST_CODE_SHIZUKU_PERMISSION)
                }
            }
        } else if (!isShizukuV3TokenValid) {
            // on API pre-23, Shizuku v3 uses old token, get token from Shizuku app
            ShizukuClientHelperPre23.createPre23AuthorizationIntent(activity)?.let {
                try {
                    activity.startActivityForResult(it, Const.REQUEST_CODE_SHIZUKU_PERMISSION)
                } catch (tr: Throwable) {
                    // should never happened
                }
            }
        }
    }

    fun requestShizukuPermission(fragment: Fragment?) {
        if (isGrantShizukuPermission || fragment == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // on API 23+, Shizuku v3 uses runtime permission
            if (!isGrantShizukuPermission) {
                if (com.absinthe.anywhere_.utils.PermissionUtils.isMIUI) {
                    showPermissionDialog(fragment)
                } else {
                    fragment.requestPermissions(arrayOf(ShizukuApiConstants.PERMISSION), Const.REQUEST_CODE_SHIZUKU_PERMISSION)
                }
            }
        } else if (!isShizukuV3TokenValid) {
            // on API pre-23, Shizuku v3 uses old token, get token from Shizuku app
            val intent = ShizukuClientHelperPre23.createPre23AuthorizationIntent(topActivity!!)
            if (intent != null) {
                try {
                    fragment.startActivityForResult(intent, Const.REQUEST_CODE_SHIZUKU_PERMISSION)
                } catch (tr: Throwable) {
                    // should never happened
                }
            } else {
                // activity not found
                Timber.d("activity not found.")
            }
        }
    }

    private fun showPermissionDialog(activity: Activity) {
        showGotoShizukuManagerDialog(activity, DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
            val intent = IntentUtils.getLaunchAppIntent("moe.shizuku.privileged.api")
            if (intent != null) {
                activity.startActivityForResult(intent, Const.REQUEST_CODE_SHIZUKU_PERMISSION)
            } else {
                ToastUtil.makeText(R.string.toast_not_install_shizuku)
                URLSchemeHandler.parse(URLManager.SHIZUKU_COOLAPK_DOWNLOAD_PAGE, activity)
            }
        })
    }

    private fun showPermissionDialog(fragment: Fragment) {
        topActivity?.let {
            showGotoShizukuManagerDialog(it, DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                val intent = IntentUtils.getLaunchAppIntent("moe.shizuku.privileged.api")
                if (intent != null) {
                    fragment.startActivityForResult(intent, Const.REQUEST_CODE_SHIZUKU_PERMISSION)
                } else {
                    ToastUtil.makeText(R.string.toast_not_install_shizuku)
                    URLSchemeHandler.parse(URLManager.SHIZUKU_COOLAPK_DOWNLOAD_PAGE, fragment)
                }
            })
        }
    }
}