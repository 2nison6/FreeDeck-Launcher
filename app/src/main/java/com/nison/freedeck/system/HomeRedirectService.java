package com.nison.freedeck.system;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.ui.main.MainActivity;

/**
 * Fire TV does not let an app become the default home screen, so this works the same way Home on Fire does: while
 * the switch in Settings is on, it notices Fire TV's home screen appearing (the Home button was pressed) and opens
 * this launcher over it. With the switch off it does nothing, and Home goes to Fire TV's own home as usual.
 */
public class HomeRedirectService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        boolean on = AppSettings.from(this).isDefaultLauncher();
        if (!HomeRedirect.shouldTakeOver(on, event.getPackageName(), event.getClassName())) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // Can't open right now (the system may be mid-transition); the next Home press tries again.
        }
    }

    @Override
    public void onInterrupt() {
    }
}
