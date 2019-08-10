/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.scid.android.gamelogic;

/**
 * A small helper class that makes it possible to return two values from a function.
 * @author petero
 */
public final class Pair<T1, T2> {
    public final T1 first;
    public final T2 second;
    
    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }
}
