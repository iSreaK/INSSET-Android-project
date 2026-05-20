package com.insset.jvbench.core.navigation;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.insset.jvbench.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Wires the bottom navigation bar of every top-level fragment.
 *
 * <p>Centralising this logic ensures consistent behaviour and works around
 * two subtle Material 3 / Navigation Component pitfalls:</p>
 * <ul>
 *   <li><b>Wrong tab gets the highlight on return.</b> Returning {@code true}
 *       from the item-selected listener tells the BottomNavigationView to
 *       update its visual selection. If we did that BEFORE the navigation
 *       tears the fragment down, the View's saved state would record the
 *       wrong item as selected and re-apply it on restore. We therefore
 *       return {@code false} for the navigating branches and let the next
 *       fragment's own {@code bind()} set the right highlight from scratch.</li>
 *   <li><b>0.5 s indicator flash on tab switch.</b> Material 3 animates the
 *       active-indicator between the previously-selected and newly-selected
 *       items. On fragment recreation that animation runs from "default
 *       first item" to "currentItemId", which the user perceives as a
 *       flash on Map. {@link android.view.View#jumpDrawablesToCurrentState()}
 *       skips that initial animation.</li>
 *   <li><b>Admin tab hides the highlight.</b> Toggling
 *       {@code MenuItem.setVisible()} rebuilds the menu items and can drop
 *       the visual selection; visibility is therefore applied BEFORE
 *       {@code setSelectedItemId()}, and
 *       {@link #updateAdminVisibility} re-asserts it afterwards.</li>
 * </ul>
 */
public final class BottomNavBinder {

    private BottomNavBinder() {
    }

    /**
     * Bind the bottom nav for the given fragment.
     *
     * @param view          the BottomNavigationView living inside the fragment's layout
     * @param fragment      the host fragment (used to resolve its NavController)
     * @param currentItemId the menu item id of the tab corresponding to the host fragment
     *                      (e.g. {@code R.id.navMapItem} for {@code MapFragment})
     * @param isAdmin       whether to expose the admin tab
     */
    public static void bind(@NonNull BottomNavigationView view,
                            @NonNull Fragment fragment,
                            int currentItemId,
                            boolean isAdmin) {
        // Visibility MUST be applied before setSelectedItemId(): toggling
        // setVisible() resets the visual selection on some Material 3 builds.
        view.getMenu().findItem(R.id.navAdminItem).setVisible(isAdmin);

        NavController navController = NavHostFragment.findNavController(fragment);
        view.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) {
                // Tapping the tab the user is already on: keep the highlight.
                // Also covers the programmatic setSelectedItemId() below.
                return true;
            }
            int target = destinationFor(id);
            if (target == 0) {
                return false;
            }
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    // popUpTo the start destination so the back stack does
                    // not grow as the user hops between tabs. No saveState /
                    // restoreState: those re-apply the previous BottomNav's
                    // saved selection on the new view, which caused the
                    // "wrong tab highlighted" bug.
                    .setPopUpTo(R.id.mapFragment, false)
                    .build();
            navController.navigate(target, null, opts);
            // Returning false leaves the current view's highlight alone
            // while we navigate away — the destination fragment will set
                    // its own.
            return false;
        });
        view.setSelectedItemId(currentItemId);
        // setSelectedItemId on a freshly inflated BottomNavigationView would
        // otherwise animate the active indicator from item 0 (Map) to
        // currentItemId, which the user perceives as the Map icon
        // briefly lighting up before the right tab takes over.
        view.jumpDrawablesToCurrentState();
    }

    /**
     * Re-apply the admin tab visibility without losing the visual selection.
     * Call this when the cached "is admin" state changes after {@link #bind}
     * (e.g. when the auth observer updates after the screen is already
     * displayed).
     */
    public static void updateAdminVisibility(@NonNull BottomNavigationView view,
                                             int currentItemId,
                                             boolean isAdmin) {
        MenuItem admin = view.getMenu().findItem(R.id.navAdminItem);
        if (admin.isVisible() == isAdmin) {
            return;
        }
        admin.setVisible(isAdmin);
        // setVisible() rebuilds the menu items and can drop the highlight;
        // re-asserting the selection keeps the right tab highlighted.
        view.setSelectedItemId(currentItemId);
        view.jumpDrawablesToCurrentState();
    }

    private static int destinationFor(int menuItemId) {
        if (menuItemId == R.id.navMapItem) {
            return R.id.mapFragment;
        }
        if (menuItemId == R.id.navAccountItem) {
            return R.id.accountFragment;
        }
        if (menuItemId == R.id.navSettingsItem) {
            return R.id.settingsFragment;
        }
        if (menuItemId == R.id.navAdminItem) {
            return R.id.adminFragment;
        }
        return 0;
    }
}
