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

package com.teefourteen.glideplayer;

import android.support.annotation.Nullable;

import java.util.ArrayList;

public abstract class StateListener<Listener, State> {
    protected ArrayList<Listener> listeners;
    protected State state;

    public StateListener(@Nullable ArrayList<Listener> listeners, State state) {
        this.state = state;
        if(listeners == null) {
            this.listeners = new ArrayList<>();
        } else {
            this.listeners = listeners;
        }
    }

    public void addListener(Listener listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    protected void updateState(State state) {
        this.state = state;
        for(Listener listener : listeners) {
            notifyListener(listener, state);
        }
    }

    protected abstract void notifyListener(Listener listener, State state);
}
