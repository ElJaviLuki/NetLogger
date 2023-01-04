package com.eljaviluki.netlogger

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook
import java.lang.StringBuilder

class Hooker : IXposedHookLoadPackage {
    private inner class RequestInfo internal constructor(
        var packageName: String,
        var method: String,
        var url: String,
        var headers: Map<String, List<String>>
    ) {
        fun print() {
            // Log the request information
            StringBuilder()
                .append("\nNetLogger - REQUEST:\n")
                .append("Package: $packageName\n")
                .append("Method: $method\n")
                .append("URL: $url\n")
                .append("Headers: $headers\n").apply {
                    printStackTrace(this)
                    XposedBridge.log(this.toString())
                }
        }
    }

    private fun printStackTrace(sb: StringBuilder) {
        /*sb.append("\nNetLogger - STACKTRACE:\n");
        for (StackTraceElement element : Thread.currentThread().getStackTrace())
            sb.append(element.toString() + '\n');*/
    }

    private inner class ResponseInfo internal constructor(
        var packageName: String,
        var status: Int,
        var headers: Map<String, List<String>>,
        var responseBody: String
    ) {
        fun print() {
            // Log the response information
            XposedBridge.log((StringBuilder()
                .append("\nNetLogger - RESPONSE:\n")
                .append("Package: $packageName\n")
                .append("Status: $status\n"))
                .append("Headers: $headers\n")
                .append("Response body: $responseBody\n").toString())
        }
    }

    private fun sniffOkHttp3(lpparam: LoadPackageParam, pkgName: String) {
        val class_Chain = XposedHelpers.findClass("okhttp3.Interceptor.Chain", lpparam.classLoader)
        val class_CallServerInterceptor =
            XposedHelpers.findClass("okhttp3.internal.http.CallServerInterceptor",
                lpparam.classLoader)
        XposedHelpers.findAndHookMethod(class_CallServerInterceptor,
            "intercept",
            class_Chain,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val req = XposedHelpers.getObjectField(param.args[0], "request")
                    val reqInfo = RequestInfo(pkgName,
                        XposedHelpers.getObjectField(req, "method").toString(),
                        XposedHelpers.getObjectField(req, "url").toString(),
                        XposedHelpers.callMethod(XposedHelpers.getObjectField(XposedHelpers.getObjectField(
                            param.args[0], "request"), "headers"),
                            "toMultimap") as Map<String, List<String>>)
                    reqInfo.print()
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    val res = param.result
                    val resInfo = ResponseInfo(pkgName,
                        XposedHelpers.getIntField(res, "code"),
                        XposedHelpers.getObjectField(res,
                            "headers") as Map<String, List<String>>,
                        XposedHelpers.callMethod(XposedHelpers.getObjectField(res, "body"),
                            "string").toString())
                    resInfo.print()
                }
            })
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val pkgName = lpparam.packageName

        //TODO: Implement Java Built-in HTTP Library HTTP Sniffer
        sniffOkHttp3(lpparam, pkgName)
        //TODO: Implement Apache Library Sniffer
    }
}