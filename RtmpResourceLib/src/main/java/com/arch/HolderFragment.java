package com.arch;


import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.arch.lifecycle.ViewModelStore;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide  https://developer.android.google.cn/jetpack/androidx/releases/lifecycle#1.1.0
 */
//@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HolderFragment extends Fragment {
    private static final String LOG_TAG = "ViewModelStores";

    private static final HolderFragmentManager sHolderFragmentManager = new HolderFragmentManager();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String HOLDER_TAG =
            "android.arch.lifecycle.state.StateProviderHolderFragment";

    private ViewModelStore mViewModelStore = new ViewModelStore();

    public HolderFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sHolderFragmentManager.holderFragmentCreated(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModelStore.clear();
    }

    public ViewModelStore getViewModelStore() {
        return mViewModelStore;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static HolderFragment holderFragmentFor(FragmentActivity activity) {
        return sHolderFragmentManager.holderFragmentFor(activity);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static HolderFragment holderFragmentFor(Fragment fragment) {
        return sHolderFragmentManager.holderFragmentFor(fragment);
    }

    @SuppressWarnings("WeakerAccess")
    static class HolderFragmentManager {
        private Map<Activity, HolderFragment> mNotCommittedActivityHolders = new HashMap<>();
        private Map<Fragment, HolderFragment> mNotCommittedFragmentHolders = new HashMap<>();

        private ActivityLifecycleCallbacks mActivityCallbacks =
                new EmptyActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        HolderFragment fragment = mNotCommittedActivityHolders.remove(activity);
                        if (fragment != null) {
                            Log.e(LOG_TAG, "Failed to save a ViewModel for " + activity);
                        }
                    }
                };

        private boolean mActivityCallbacksIsAdded = false;

        private FragmentLifecycleCallbacks mParentDestroyedCallback =
                new FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentDestroyed(FragmentManager fm, Fragment parentFragment) {
                        super.onFragmentDestroyed(fm, parentFragment);
                        HolderFragment fragment = mNotCommittedFragmentHolders.remove(
                                parentFragment);
                        if (fragment != null) {
                            Log.e(LOG_TAG, "Failed to save a ViewModel for " + parentFragment);
                        }
                    }
                };

        void holderFragmentCreated(Fragment holderFragment) {
            Fragment parentFragment = holderFragment.getParentFragment();
            if (parentFragment != null) {
                mNotCommittedFragmentHolders.remove(parentFragment);
                parentFragment.getFragmentManager().unregisterFragmentLifecycleCallbacks(
                        mParentDestroyedCallback);
            } else {
                mNotCommittedActivityHolders.remove(holderFragment.getActivity());
            }
        }

        private static HolderFragment findHolderFragment(FragmentManager manager) {
            if (manager.isDestroyed()) {
                throw new IllegalStateException("Can't access ViewModels from onDestroy");
            }

            Fragment fragmentByTag = manager.findFragmentByTag(HOLDER_TAG);
            if (fragmentByTag != null && !(fragmentByTag instanceof HolderFragment)) {
                throw new IllegalStateException("Unexpected "
                        + "fragment instance was returned by HOLDER_TAG");
            }
            return (HolderFragment) fragmentByTag;
        }

        private static HolderFragment createHolderFragment(FragmentManager fragmentManager) {
            HolderFragment holder = new HolderFragment();
            fragmentManager.beginTransaction().add(holder, HOLDER_TAG).commitAllowingStateLoss();
            return holder;
        }

        HolderFragment holderFragmentFor(FragmentActivity activity) {
            FragmentManager fm = activity.getSupportFragmentManager();
            HolderFragment holder = findHolderFragment(fm);
            if (holder != null) {
                return holder;
            }
            holder = mNotCommittedActivityHolders.get(activity);
            if (holder != null) {
                return holder;
            }

            if (!mActivityCallbacksIsAdded) {
                mActivityCallbacksIsAdded = true;
                activity.getApplication().registerActivityLifecycleCallbacks(mActivityCallbacks);
            }
            holder = createHolderFragment(fm);
            mNotCommittedActivityHolders.put(activity, holder);
            return holder;
        }

        HolderFragment holderFragmentFor(Fragment parentFragment) {
            FragmentManager fm = parentFragment.getChildFragmentManager();
            HolderFragment holder = findHolderFragment(fm);
            if (holder != null) {
                return holder;
            }
            holder = mNotCommittedFragmentHolders.get(parentFragment);
            if (holder != null) {
                return holder;
            }

            parentFragment.getFragmentManager()
                    .registerFragmentLifecycleCallbacks(mParentDestroyedCallback, false);
            holder = createHolderFragment(fm);
            mNotCommittedFragmentHolders.put(parentFragment, holder);
            return holder;
        }
    }
}
