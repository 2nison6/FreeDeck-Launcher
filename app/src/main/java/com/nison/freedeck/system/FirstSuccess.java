package com.nison.freedeck.system;

import java.util.List;

/** Runs a list of ways of doing something, in order, and stops at the first that works. */
final class FirstSuccess {

    interface Attempt {
        /** @return true if it worked */
        boolean run();
    }

    private FirstSuccess() {
    }

    /** @return true if one of the attempts worked; the ones after it are not run */
    static boolean of(List<Attempt> attempts) {
        for (Attempt attempt : attempts) {
            if (attempt.run()) {
                return true;
            }
        }
        return false;
    }
}
