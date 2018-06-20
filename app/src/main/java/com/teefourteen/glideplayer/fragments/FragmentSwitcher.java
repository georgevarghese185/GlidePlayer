/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.fragments;

import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * Created by george on 2/10/16.
 */
public class FragmentSwitcher {
    private Fragment currentFragment;
    private String currentFragmentTag;
    private FragmentManager fragmentManager;
    private @IdRes int containerViewId;

    public FragmentSwitcher(FragmentManager fragmentManager, @IdRes int containerViewId) {
        this.fragmentManager = fragmentManager;
        this.containerViewId = containerViewId;
    }

    public void switchTo(Fragment fragment, String tag) {
        switchTo(fragment, tag, false, false);
    }

    public void switchTo(Fragment fragment, String tag, boolean destroyPreviousFragment) {
        switchTo(fragment, tag, destroyPreviousFragment, false);
    }

    private void switchTo(Fragment fragment, String tag, boolean destroyPreviousFragment,
                         boolean forceReattach) {
        if(fragment == currentFragment && !forceReattach)
            return;

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if(currentFragment != null) {
            if(destroyPreviousFragment)
                transaction.remove(currentFragment);
            else
                transaction.detach(currentFragment);
        }

        if(fragmentManager.findFragmentByTag(tag) == null)
            transaction.add(containerViewId,fragment,tag);
        else
            transaction.attach(fragment);
        currentFragment = fragment;
        currentFragmentTag = tag;
        transaction.commit();
    }

    public void reattach() {
        switchTo(currentFragment, currentFragmentTag, false, true);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }
}