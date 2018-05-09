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
