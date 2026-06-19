// ─────────────────────────────────────────────────────────────────────
        // Hook 8 — xo/C$c.onPageFinished(WebView, String)
        //
        // JavaScript fetch override — kills the initial pre-roll countdown ad.
        //
        // The SPA at ott-androidtv.tubitv.com makes an XHR/fetch call to
        // rainmaker.production-public.tubi.io to fetch the VAST ad manifest.
        // Hook 7 (shouldInterceptRequest) catches this for subsequent sessions
        // but the very first request can slip through before the intercept
        // layer is fully active.
        //
        // This hook fires after the SPA finishes loading (before any content
        // is selected). It patches window.fetch in the SPA's JS execution
        // context to intercept and nullify requests to ad orchestration
        // endpoints. When the user later selects content and the SPA calls
        // fetch("https://rainmaker..."), our override returns an empty
        // Response(200) immediately — the SPA receives no ad manifest, never
        // sets the <video> src, and no ad plays.
        //
        // Blocked in JS fetch layer:
        //   rainmaker.production-public.tubi.io  (VAST manifest)
        //   ads.production-public.tubi.io        (ad config)
        //
        // window.__tubiAdBlockInstalled guard prevents double-patching
        // if onPageFinished fires multiple times.
        //
        // p1 = WebView (the evaluateJavascript target)
        // Registers: v0 = JS string, v1 = null callback
        // ─────────────────────────────────────────────────────────────────────
        TubiWebClientPageFinishedFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "(function(){if(window.__tubiAdBlockInstalled)return;window.__tubiAdBlockInstalled=1;var f=window.fetch;if(!f)return;window.fetch=function(u,o){var s=String(u);if(s.indexOf('rainmaker.production-public.tubi.io')>=0||s.indexOf('ads.production-public.tubi.io')>=0)return Promise.resolve(new Response('',{status:200}));return f.call(this,u,o)}})()"
                const/4 v1, 0x0
                invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
            """
        )

        // ─────────────────────────────────────────────────────────────────────
        // Hook 9 — qf/c.suspendGetAdBreaks(String, String, String, Map, Continuation)
        //
        // NATIVE OKHTTP PRE-ROLL ROOT CAUSE — the final piece.
        //
        // This is the R8-obfuscated RainmakerAdsFetcher.suspendGetAdBreaks()
        // implementation. It makes a direct OkHttp HTTP request to:
        //   https://rainmaker.production-public.tubi.io/api/v2/rev/vod/ANDROID
        //
        // This call bypasses shouldInterceptRequest (WebView-only) entirely.
        // Hook 7 caught subsequent WebView ad requests; this native call is why
        // the FIRST pre-roll survived — it fires before the WebView is even active.
        //
        // Return the COROUTINE_SUSPENDED sentinel at index 0:
        //   Lai/q; = R8-minified CoroutineSingletons
        //   Lai/q;.a = COROUTINE_SUSPENDED field
        //
        // The coroutine caller suspends indefinitely. The ad manifest never
        // arrives. Tubi's built-in timeout fires and content plays ad-free.
        //
        // No extension needed — pure smali, straight-line, no labels.
        // ─────────────────────────────────────────────────────────────────────
        RainmakerSuspendGetAdBreaksFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lai/q;->a:Lai/q;
                return-object v0
            """
        )
