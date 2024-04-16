/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package com.sun.jna.ptr;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/** Represents a reference to a pointer to native data.
 * In C notation, <code>void**</code>.
 * @author twall
 */
public class PointerByReference extends ByReference {

    @Deprecated
    public PointerByReference() {
        this((Pointer) null);
//        arena = Arena.ofAuto();
//        segment = arena.allocate(ADDRESS.withTargetLayout(ADDRESS));
    }

    public PointerByReference(Arena arena) {
        this(arena, null);
//        this.arena = arena;
//        segment = arena.allocate(ADDRESS);
    }

    @Deprecated
    public PointerByReference(Pointer value) {
        super(Native.POINTER_SIZE);
//        arena = value.arena;
//        segment = arena.allocate(ADDRESS);
        setValue(value);
    }

    public PointerByReference(Arena arena, Pointer value) {
        super(arena, Native.POINTER_SIZE);
        setValue(value);
    }

    public void setValue(Pointer value) {
        getPointer().setPointer(0, value);
//        segment.set(JAVA_LONG, 0, value.getLong(0)); // TODO ???
    }

    public Pointer getValue() {
        return getPointer().getPointer(0);
//        return new Pointer(arena, segment);
    }

    public Pointer getPointer() {
        return super.getPointer();
//        return new Pointer(arena, segment);
    }

}
