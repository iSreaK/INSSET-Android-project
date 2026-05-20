package com.insset.jvbench.core.theme;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Adds the bottom system inset (3-button nav bar height or gesture pill
 * height) to a view's bottom padding so its content stays clear of the
 * system navigation. The view's initial bottom padding is preserved.
 *
 * The activity root listener handles the top inset; each fragment chooses
 * the right child to consume the bottom inset (BottomNavigationView when
 * present, otherwise the scrolling content container).
 */
public final class WindowInsetsHelper {
    private WindowInsetsHelper() {
    }

    public static void addBottomSystemInset(View view) {
        final int basePaddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    basePaddingBottom + bars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
