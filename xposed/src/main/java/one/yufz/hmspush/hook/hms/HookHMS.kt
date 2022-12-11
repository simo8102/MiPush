package one.yufz.hmspush.hook.hms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.CursorWindow
import android.os.Build
import com.huawei.android.app.NotificationManagerEx
import dalvik.system.DexClassLoader
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError
import de.robv.android.xposed.callbacks.XC_LoadPackage
import one.yufz.hmspush.hook.XLog
import one.yufz.hmspush.hook.bridge.HookContentProvider
import one.yufz.xposed.*

class HookHMS {
    companion object {
        private const val TAG = "HookHMS"
    }

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (HookPushNC.canHook(lpparam.classLoader)) {
            HookPushNC.hook(lpparam.classLoader)
        }

        return;
        HookContentProvider().hook(lpparam.classLoader)
        fakeFingerprint(lpparam)
    }

    private fun hookLegacyPush(classLoader: ClassLoader) {
        XLog.d(TAG, "hookLegacyPush() called with: classLoader = $classLoader")

        try {
            classLoader.findClass("com.huawei.hms.pushnc.entity.PushSelfShowMessage")
        } catch (e: ClassNotFoundError) {
            XLog.d(TAG, "PushSelfShowMessage not Found, stop hook")
            return
        }

        PushSignWatcher.watch()

        Class::class.java.hookMethod("forName", String::class.java, Boolean::class.java, ClassLoader::class.java) {
            doBefore {
                if (args[0] == NotificationManagerEx::class.java.name) {
                    result = NotificationManagerEx::class.java
                }
            }
        }
    }

    private fun fakeFingerprint(lpparam: XC_LoadPackage.LoadPackageParam) {
        lpparam.classLoader.findClass("com.huawei.hms.auth.api.CheckFingerprintRequest")
            .hookMethod("parseEntity", String::class.java) {
                doBefore {
                    if (Prefs.isDisableSignature()) {
                        val request = args[0] as String
                        if (request.contains("auth.checkFingerprint")) {
                            val response = """{"header":{"auth_rtnCode":"0"},"body":{}}"""
                            thisObject.callMethod("call", response)
                            result = null
                        }
                    }
                }
            }
    }
}
