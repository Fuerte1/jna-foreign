/* Copyright (c) 2007-2013 Timothy Wall, All Rights Reserved
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
package com.sun.jna.win32;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.*;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class W32APIMapperTest {
    // Unicode Character 'SINGLE RIGHT-POINTING ANGLE QUOTATION MARK': ›
    //
    // byte encoding in CP1250-CP1258 is 155
    //
    // The requirement is, that the encoding is present in many native windows
    // encodings and outside the ASCII range
    final String UNICODE = "[\u203a]";
    final String MAGIC = "magic" + UNICODE;

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(W32APIMapperTest.class);
//    }

    public interface UnicodeLibrary extends Library {
        public static class TestStructure extends Structure {
            public static final List<String> FIELDS = createFieldsOrder("string", "string2", "bool", "bool2");
            public String string;
            public String string2;
            public boolean bool;
            public boolean bool2;
            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }
        String returnWStringArgument(String arg);
        boolean returnInt32Argument(boolean arg);
        String returnWideStringArrayElement(String[] args, int which);
    }
    public interface ASCIILibrary extends Library {
        public static class TestStructure extends Structure {
            public static final List<String> FIELDS = Arrays.asList("string", "string2", "bool", "bool2");
            public String string;
            public String string2;
            public boolean bool;
            public boolean bool2;
            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }
        String returnStringArgument(String arg);
        boolean returnInt32Argument(boolean arg);
    }

    UnicodeLibrary unicode;
    ASCIILibrary ascii;

    @Before
    public void setUp() {
        unicode = Native.load("testlib", UnicodeLibrary.class, W32APIOptions.UNICODE_OPTIONS);
        ascii = Native.load("testlib", ASCIILibrary.class, W32APIOptions.ASCII_OPTIONS);
    }

    @After
    public void tearDown() {
        unicode = null;
        ascii = null;
    }

    @Test
    public void testInvalidHandleValue() {
        String EXPECTED = "@0xffffffff";
        if (Native.POINTER_SIZE == 8) {
            EXPECTED += "ffffffff";
        }
        Pointer p = Pointer.createConstant(Native.POINTER_SIZE == 8 ? -1 : 0xFFFFFFFFL);
        Assert.assertTrue("Wrong value: " + p, p.toString().endsWith(EXPECTED));

    }

    @Test
    public void testBooleanArgumentConversion() {
        Assert.assertTrue("Wrong boolean TRUE argument conversion (unicode)", unicode.returnInt32Argument(true));
        Assert.assertFalse("Wrong boolean FALSE argument conversion (unicode)", unicode.returnInt32Argument(false));

        Assert.assertTrue("Wrong boolean TRUE argument conversion (ASCII)", ascii.returnInt32Argument(true));
        Assert.assertFalse("Wrong boolean FALSE argument conversion (ASCII)", ascii.returnInt32Argument(false));
    }

    @Test
    public void testUnicodeMapping() {
        Assert.assertEquals("Strings should correspond to wide strings", MAGIC, unicode.returnWStringArgument(MAGIC));
        String[] args = { "one", "two" };
        Assert.assertEquals("String arrays should be converted to wchar_t*[] and back", args[0], unicode.returnWideStringArrayElement(args, 0));
    }

    @Test
    public void testASCIIMapping() {
        Assert.assertEquals("Strings should correspond to C strings", MAGIC, ascii.returnStringArgument(MAGIC));
    }

    @Test
    public void testUnicodeStructureSize() {
        UnicodeLibrary.TestStructure s = new UnicodeLibrary.TestStructure();
        Assert.assertEquals("Wrong structure size", Native.POINTER_SIZE*2+8, s.size());
    }

    @Test
    public void testASCIIStructureSize() {
        ASCIILibrary.TestStructure s = new ASCIILibrary.TestStructure();
        Assert.assertEquals("Wrong structure size", Native.POINTER_SIZE*2+8, s.size());
    }

    @Test
    public void testUnicodeStructureWriteBoolean() {
        UnicodeLibrary.TestStructure s = new UnicodeLibrary.TestStructure();
        s.bool2 = true;
        s.write();
        Assert.assertEquals("Wrong value written for FALSE", 0, s.getPointer().getInt(Native.POINTER_SIZE*2));
        Assert.assertEquals("Wrong value written for TRUE", 1, s.getPointer().getInt(Native.POINTER_SIZE*2+4));
    }

    @Test
    public void testASCIIStructureWriteBoolean() {
        ASCIILibrary.TestStructure s = new ASCIILibrary.TestStructure();
        s.bool2 = true;
        s.write();
        Assert.assertEquals("Wrong value written for FALSE", 0, s.getPointer().getInt(Native.POINTER_SIZE*2));
        Assert.assertEquals("Wrong value written for TRUE", 1, s.getPointer().getInt(Native.POINTER_SIZE*2+4));
    }

    @Test
    public void testUnicodeStructureReadBoolean() {
        UnicodeLibrary.TestStructure s = new UnicodeLibrary.TestStructure();
        s.getPointer().setInt(Native.POINTER_SIZE*2, 1);
        s.getPointer().setInt(Native.POINTER_SIZE*2+4, 0);
        s.read();
        Assert.assertTrue("Wrong value read for TRUE", s.bool);
        Assert.assertFalse("Wrong value read for FALSE", s.bool2);
    }

    @Test
    public void testASCIIStructureReadBoolean() {
        ASCIILibrary.TestStructure s = new ASCIILibrary.TestStructure();
        s.getPointer().setInt(Native.POINTER_SIZE*2, 1);
        s.getPointer().setInt(Native.POINTER_SIZE*2+4, 0);
        s.read();
        Assert.assertTrue("Wrong value read for TRUE", s.bool);
        Assert.assertFalse("Wrong value read for FALSE", s.bool2);
    }

    @Test
    public void testUnicodeStructureWriteString() {
        UnicodeLibrary.TestStructure s = new UnicodeLibrary.TestStructure();
        s.string = null;
        s.string2 = MAGIC;
        s.write();
        Assert.assertEquals("Improper null write", null, s.getPointer().getPointer(0));
        Assert.assertEquals("Improper string write", MAGIC, s.getPointer().getPointer(Native.POINTER_SIZE).getWideString(0));
    }

    @Test
    public void testASCIIStructureWriteString() {
        ASCIILibrary.TestStructure s = new ASCIILibrary.TestStructure();
        s.string = null;
        s.string2 = MAGIC;
        s.write();
        Assert.assertEquals("Improper null write", null, s.getPointer().getPointer(0));
        Assert.assertEquals("Improper string write", MAGIC, s.getPointer().getPointer(Native.POINTER_SIZE).getString(0));
    }

    @Test
    public void testUnicodeStructureReadString() {
        UnicodeLibrary.TestStructure s = new UnicodeLibrary.TestStructure();
        s.string = MAGIC;
        s.string2 = null;
        s.write();
        s.read();
        Assert.assertEquals("Improper string read", MAGIC, s.string);
        Assert.assertEquals("Improper null string read", null, s.string2);
    }

    @Test
    public void testASCIIStructureReadString() {
        ASCIILibrary.TestStructure s = new ASCIILibrary.TestStructure();
        s.string = MAGIC;
        s.string2 = null;
        s.write();
        s.read();
        Assert.assertEquals("Improper string read", MAGIC, s.string);
        Assert.assertEquals("Improper null string read: " + s, null, s.string2);
    }
}
