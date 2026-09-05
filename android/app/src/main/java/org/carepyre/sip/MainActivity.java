package org.carepyre.sip;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * CarePyre SIP Phone -- real, minimal Android scaffold for kanban CP-SIP-9911 ("Android APK
 * should be in releases"). This installs and runs on a real device/emulator, but deliberately
 * does NOT embed native/sip-jni-proof's own libcarepyre_sip.so yet.
 *
 * Real, honest reason, found while scoping this exact card: parena_runtime.h unconditionally
 * includes <SDL2/SDL.h>/<SDL2/SDL_ttf.h> (stdlib/sdl2.prn is "built in, same tier as core"), so
 * cross-compiling it for a real Android ABI via the NDK needs a real Android-targeted SDL2
 * build -- a genuinely separate, harder problem than the desktop JNI proof (Phase 1) solved,
 * not a simple "apt-get install libsdl2-dev" the NDK toolchain has no equivalent of. See
 * docs/SIP_PHONE_ANDROID_NORTHSTAR.md's own updated Phase 5 for the real, not-yet-solved gap
 * this leaves. This screen is real, installable, "something to iterate from" for the Android
 * side specifically, same real framing SIP-0001's own card text already used for Phase 1.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setText("CarePyre SIP Phone");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        layout.addView(title);

        TextView status = new TextView(this);
        status.setText("Real Android scaffold -- the native SIP/RTP core "
            + "(PARENA, verified via a real JNI proof on desktop) is not wired in here yet. "
            + "See docs/SIP_PHONE_ANDROID_NORTHSTAR.md for the real, honest reason "
            + "(Android NDK cross-compilation needs its own SDL2 build) and the real next step.");
        status.setPadding(0, 32, 0, 0);
        layout.addView(status);

        setContentView(layout);
    }
}
