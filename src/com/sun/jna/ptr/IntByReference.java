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

import com.sun.jna.Pointer;

import java.lang.foreign.Arena;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public class IntByReference extends ByReference { // ByReference {

    @Deprecated
    public IntByReference() {
        this(0);
//        arena = Arena.ofAuto();
//        segment = arena.allocate(JAVA_INT);
    }

    public IntByReference(Arena arena) {
        this(arena, 0);
//        this.arena = arena;
//        segment = arena.allocate(JAVA_INT);
    }

    @Deprecated
    public IntByReference(int value) {
        super(4);
        setValue(value);
//        arena = Arena.ofAuto();
//        segment = arena.allocateFrom(JAVA_INT, value);
    }

    public IntByReference(Arena arena, int value) {
        super(arena, 4);
        setValue(value);
//        this.arena = arena;
//        segment = arena.allocateFrom(JAVA_INT, value);
    }

    public void setValue(int value) {
        getPointer().setInt(0, value);
//        segment.set(JAVA_INT, 0, value);
    }

    public int getValue() {
        return getPointer().getInt(0);
//        return segment.get(JAVA_INT, 0);
    }

    @Override
    public String toString() {
        return String.format("int@0x%1$x=0x%2$x (%2$d)", Pointer.nativeValue(getPointer()), getValue());
//        return String.format("int@0x%1$x=0x%2$x (%2$d)", segment.address(), getValue());
    }
}
