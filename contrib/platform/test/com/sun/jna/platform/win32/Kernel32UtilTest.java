/* Copyright (c) 2010, 2013 Daniel Doubrovkine, Markus Karg, All Rights Reserved
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
package com.sun.jna.platform.win32;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Tlhelp32.MODULEENTRY32W;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.CACHE_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.GROUP_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.WinNT.LARGE_INTEGER;
import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.NUMA_NODE_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.PROCESSOR_CACHE_TYPE;
import com.sun.jna.platform.win32.WinNT.PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author dblock[at]dblock[dot]org
 * @author markus[at]headcrashing[dot]eu
 */
public class Kernel32UtilTest {

//    public static void main(String[] args) throws Exception {
//        System.out.println("Computer name: " + Kernel32Util.getComputerName());
//        System.out.println("Temp path: " + Kernel32Util.getTempPath());
//        // logical drives
//        System.out.println("Logical drives: ");
//        Collection<String> logicalDrives = Kernel32Util.getLogicalDriveStrings();
//        for(String logicalDrive : logicalDrives) {
//            // drive type
//            System.out.println(" " + logicalDrive + " ("
//                    + Kernel32.INSTANCE.GetDriveType(logicalDrive) + ")");
//            // free space
//            LARGE_INTEGER.ByReference lpFreeBytesAvailable = new LARGE_INTEGER.ByReference();
//            LARGE_INTEGER.ByReference lpTotalNumberOfBytes = new LARGE_INTEGER.ByReference();
//            LARGE_INTEGER.ByReference lpTotalNumberOfFreeBytes = new LARGE_INTEGER.ByReference();
//            if (Kernel32.INSTANCE.GetDiskFreeSpaceEx(logicalDrive, lpFreeBytesAvailable, lpTotalNumberOfBytes, lpTotalNumberOfFreeBytes)) {
//                System.out.println("  Total: " + formatBytes(lpTotalNumberOfBytes.getValue()));
//                System.out.println("   Free: " + formatBytes(lpTotalNumberOfFreeBytes.getValue()));
//            }
//        }
//
//        junit.textui.TestRunner.run(Kernel32UtilTest.class);
//    }

    /**
     * Format bytes.
     * @param bytes
     *  Bytes.
     * @return
     *  Rounded string representation of the byte size.
     */
    private static String formatBytes(long bytes) {
        if (bytes == 1) { // bytes
            return String.format("%d byte", bytes);
        } else if (bytes < 1024) { // bytes
            return String.format("%d bytes", bytes);
        } else if (bytes < 1048576 && bytes % 1024 == 0) { // Kb
            return String.format("%.0f KB", (double) bytes / 1024);
        } else if (bytes < 1048576) { // Kb
            return String.format("%.1f KB", (double) bytes / 1024);
        } else if (bytes % 1048576 == 0 && bytes < 1073741824) { // Mb
            return String.format("%.0f MB", (double) bytes / 1048576);
        } else if (bytes < 1073741824) { // Mb
            return String.format("%.1f MB", (double) bytes / 1048576);
        } else if (bytes % 1073741824 == 0 && bytes < 1099511627776L) { // GB
            return String.format("%.0f GB", (double) bytes / 1073741824);
        } else if (bytes < 1099511627776L ) {
            return String.format("%.1f GB", (double) bytes / 1073741824);
        } else if (bytes % 1099511627776L == 0 && bytes < 1125899906842624L) { // TB
            return String.format("%.0f TB", (double) bytes / 1099511627776L);
        } else if (bytes < 1125899906842624L ) {
            return String.format("%.1f TB", (double) bytes / 1099511627776L);
        } else {
            return String.format("%d bytes", bytes);
        }
    }

    @Test
    public void testFreeLocalMemory() {
        try {
            Pointer ptr = new Pointer(0xFFFFFFFFFFFFFFFFL);
            Kernel32Util.freeLocalMemory(ptr);
            Assert.fail("Unexpected success to free bad local memory");
        } catch(Win32Exception e) {
            HRESULT hr = e.getHR();
            int code = W32Errors.HRESULT_CODE(hr.intValue());
            Assert.assertEquals("Mismatched failure reason code", WinError.ERROR_INVALID_HANDLE, code);
        }
    }

    @Test
    public void testFreeGlobalMemory() {
        try {
            Pointer ptr = new Pointer(0xFFFFFFFFFFFFFFFFL);
            Kernel32Util.freeGlobalMemory(ptr);
            Assert.fail("Unexpected success to free bad global memory");
        } catch(Win32Exception e) {
            HRESULT hr = e.getHR();
            int code = W32Errors.HRESULT_CODE(hr.intValue());
            Assert.assertEquals("Mismatched failure reason code", WinError.ERROR_INVALID_HANDLE, code);
        }
    }

    @Test
    public void testGetComputerName() {
        Assert.assertTrue(Kernel32Util.getComputerName().length() > 0);
    }

    @Test
    public void testFormatMessageFromLastErrorCode() {
        if (AbstractWin32TestSupport.isEnglishLocale) {
            Assert.assertEquals("The remote server has been paused or is in the process of being started.", Kernel32Util.formatMessageFromLastErrorCode(W32Errors.ERROR_SHARING_PAUSED));
        } else {
            System.out.println("testFormatMessageFromLastErrorCode Test can only be run on english locale");
        }
    }

    @Test
    public void testFormatMessageFromHR() {
        if(AbstractWin32TestSupport.isEnglishLocale) {
            Assert.assertEquals("The operation completed successfully.", Kernel32Util.formatMessage(W32Errors.S_OK));
        } else {
            System.out.println("testFormatMessageFromHR Test can only be run on english locale");
        }
    }

    @Test
    public void testFormatMessageFromErrorCodeWithNonEnglishLocale() {
        int errorCode = W32Errors.S_OK.intValue();
        String formattedMsgInDefaultLocale = Kernel32Util.formatMessage(errorCode);
        // primary and sub languages id's of the english locale, because it is present on most machines
        String formattedMsgInEnglishLocale = Kernel32Util.formatMessage(errorCode, 9, 1);
        if(AbstractWin32TestSupport.isEnglishLocale) {
            Assert.assertEquals(formattedMsgInDefaultLocale, formattedMsgInEnglishLocale);
        } else {
            Assert.assertNotSame(formattedMsgInDefaultLocale, formattedMsgInEnglishLocale);
        }
    }

    @Test
    public void testGetTempPath() {
        Assert.assertTrue(Kernel32Util.getTempPath().length() > 0);
    }

    @Test
    public void testGetLogicalDriveStrings() {
        Collection<String> logicalDrives = Kernel32Util.getLogicalDriveStrings();
        Assert.assertTrue("No logical drives found", logicalDrives.size() > 0);
        for(String logicalDrive : logicalDrives) {
            Assert.assertTrue("Empty logical drive name in list", logicalDrive.length() > 0);
        }
    }

    @Test
    public void testDeleteFile() throws IOException {
        String filename = Kernel32Util.getTempPath() + "\\FileDoesNotExist.jna";
        File f = new File(filename);
        f.createNewFile();
        Kernel32Util.deleteFile(filename);
    }

    @Test
    public void testGetFileAttributes() throws IOException {
        String filename = Kernel32Util.getTempPath();
        int fileAttributes = Kernel32Util.getFileAttributes(filename);
        Assert.assertEquals(WinNT.FILE_ATTRIBUTE_DIRECTORY, fileAttributes & WinNT.FILE_ATTRIBUTE_DIRECTORY);
        File tempFile = File.createTempFile("jna", "tmp");
        tempFile.deleteOnExit();
        int fileAttributes2 = Kernel32Util.getFileAttributes(tempFile.getAbsolutePath());
        tempFile.delete();
        Assert.assertEquals(0, fileAttributes2 & WinNT.FILE_ATTRIBUTE_DIRECTORY);
    }

    @Test
    public void testGetEnvironmentVariable() {
        Assert.assertEquals(null, Kernel32Util.getEnvironmentVariable("jna-getenvironment-test"));
        Kernel32.INSTANCE.SetEnvironmentVariable("jna-getenvironment-test", "42");
        Assert.assertEquals("42", Kernel32Util.getEnvironmentVariable("jna-getenvironment-test"));
    }

    @Test
    public final void testGetPrivateProfileInt() throws IOException {
        final File tmp = File.createTempFile("testGetPrivateProfileInt", "ini");
        tmp.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp)))) {
            writer.println("[Section]");
            writer.println("existingKey = 123");
        }

        Assert.assertEquals(123, Kernel32Util.getPrivateProfileInt("Section", "existingKey", 456, tmp.getCanonicalPath()));
        Assert.assertEquals(456, Kernel32Util.getPrivateProfileInt("Section", "missingKey", 456, tmp.getCanonicalPath()));
    }

    @Test
    public final void testGetPrivateProfileString() throws IOException {
        final File tmp = File.createTempFile("testGetPrivateProfileString", "ini");
        tmp.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp)))) {
            writer.println("[Section]");
            writer.println("existingKey = ABC");
        }

        Assert.assertEquals("ABC", Kernel32Util.getPrivateProfileString("Section", "existingKey", "DEF", tmp.getCanonicalPath()));
        Assert.assertEquals("DEF", Kernel32Util.getPrivateProfileString("Section", "missingKey", "DEF", tmp.getCanonicalPath()));
    }

    @Test
    public final void testWritePrivateProfileString() throws IOException {
        final File tmp = File.createTempFile("testWritePrivateProfileString", "ini");
        tmp.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp)))) {
            writer.println("[Section]");
            writer.println("existingKey = ABC");
            writer.println("removedKey = JKL");
        }

        Kernel32Util.writePrivateProfileString("Section", "existingKey", "DEF", tmp.getCanonicalPath());
        Kernel32Util.writePrivateProfileString("Section", "addedKey", "GHI", tmp.getCanonicalPath());
        Kernel32Util.writePrivateProfileString("Section", "removedKey", null, tmp.getCanonicalPath());

        try (BufferedReader reader = new BufferedReader(new FileReader(tmp))) {
            Assert.assertEquals(reader.readLine(), "[Section]");
            Assert.assertTrue(reader.readLine().matches("existingKey\\s*=\\s*DEF"));
            Assert.assertTrue(reader.readLine().matches("addedKey\\s*=\\s*GHI"));
            Assert.assertEquals(reader.readLine(), null);
        }
    }

    @Test
    public final void testGetPrivateProfileSection() throws IOException {
        final File tmp = File.createTempFile("testGetPrivateProfileSection", ".ini");
        tmp.deleteOnExit();

        try (PrintWriter writer0 = new PrintWriter(new BufferedWriter(new FileWriter(tmp)))) {
            writer0.println("[X]");
        }

        final String[] lines0 = Kernel32Util.getPrivateProfileSection("X", tmp.getCanonicalPath());
        Assert.assertEquals(lines0.length, 0);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp, true)))) {
            writer.println("A=1");
            writer.println("foo=bar");
        }

        final String[] lines = Kernel32Util.getPrivateProfileSection("X", tmp.getCanonicalPath());
        Assert.assertEquals(lines.length, 2);
        Assert.assertEquals(lines[0], "A=1");
        Assert.assertEquals(lines[1], "foo=bar");
    }

    @Test
    public final void testGetPrivateProfileSectionNames() throws IOException {
        final File tmp = File.createTempFile("testGetPrivateProfileSectionNames", "ini");
        tmp.deleteOnExit();

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp)))) {
            writer.println("[S1]");
            writer.println("A=1");
            writer.println("B=X");
            writer.println("[S2]");
            writer.println("C=2");
            writer.println("D=Y");
        }

        String[] sectionNames = Kernel32Util.getPrivateProfileSectionNames(tmp.getCanonicalPath());
        Assert.assertEquals(sectionNames.length, 2);
        Assert.assertEquals(sectionNames[0], "S1");
        Assert.assertEquals(sectionNames[1], "S2");
    }

    @Test
    public final void testWritePrivateProfileSection() throws IOException {
        final File tmp = File.createTempFile("testWritePrivateProfileSecion", "ini");
        tmp.deleteOnExit();

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tmp)))) {
            writer.println("[S1]");
            writer.println("A=1");
            writer.println("B=X");
            writer.println("[S2]");
            writer.println("C=2");
            writer.println("foo=bar");
        }

        Kernel32Util.writePrivateProfileSection("S1", new String[] { "A=3", "E=Z" }, tmp.getCanonicalPath());

        try (BufferedReader reader = new BufferedReader(new FileReader(tmp))) {
            Assert.assertEquals(reader.readLine(), "[S1]");
            Assert.assertEquals(reader.readLine(), "A=3");
            Assert.assertEquals(reader.readLine(), "E=Z");
            Assert.assertEquals(reader.readLine(), "[S2]");
            Assert.assertEquals(reader.readLine(), "C=2");
            Assert.assertEquals(reader.readLine(), "foo=bar");
        }
    }

    @Test
    public final void testQueryFullProcessImageName() {
        int pid = Kernel32.INSTANCE.GetCurrentProcessId();

        HANDLE h = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, pid);
        Assert.assertNotNull("Failed (" + Kernel32.INSTANCE.GetLastError() + ") to get process handle", h);
        try {
            String name = Kernel32Util.QueryFullProcessImageName(h, 0);
            Assert.assertNotNull("Failed to query process image name, null path returned", name);
            Assert.assertTrue("Failed to query process image name, empty path returned", name.length() > 0);
        } finally {
            Kernel32Util.closeHandle(h);
        }

        String name = Kernel32Util.QueryFullProcessImageName(pid, 0);
        Assert.assertNotNull("Failed to query process image name, null path returned", name);
        Assert.assertTrue("Failed to query process image name, empty path returned", name.length() > 0);

        try {
            Kernel32Util.QueryFullProcessImageName(0, 0); // the system process
            Assert.fail("Should never reach here");
        } catch (Win32Exception expected) {
            Assert.assertEquals("Should get Invalid Parameter error", Kernel32.ERROR_INVALID_PARAMETER, expected.getErrorCode());
        }
    }

    @Test
    public void testGetResource() {
        String winDir = Kernel32Util.getEnvironmentVariable("WINDIR");
        Assert.assertNotNull("No WINDIR value returned", winDir);
        Assert.assertTrue("Specified WINDIR does not exist: " + winDir, new File(winDir).exists());

        // On Windows 7, "14" is the type assigned to the "My Computer" icon
        // (which is named "ICO_MYCOMPUTER")
        byte[] results = Kernel32Util.getResource(new File(winDir, "explorer.exe").getAbsolutePath(), "14",
                "ICO_MYCOMPUTER");
        Assert.assertNotNull("The 'ICO_MYCOMPUTER' resource in explorer.exe should have some content.", results);
        Assert.assertTrue("The 'ICO_MYCOMPUTER' resource in explorer.exe should have some content.", results.length > 0);
    }

    @Test
    public void testGetResourceNames() {
        String winDir = Kernel32Util.getEnvironmentVariable("WINDIR");
        Assert.assertNotNull("No WINDIR value returned", winDir);
        Assert.assertTrue("Specified WINDIR does not exist: " + winDir, new File(winDir).exists());

        // On Windows 7, "14" is the type assigned to the "My Computer" icon
        // (which is named "ICO_MYCOMPUTER")
        Map<String, List<String>> names = Kernel32Util.getResourceNames(new File(winDir, "explorer.exe").getAbsolutePath());

        Assert.assertNotNull("explorer.exe should contain some resources in it.", names);
        Assert.assertTrue("explorer.exe should contain some resource types in it.", names.size() > 0);
        Assert.assertTrue("explorer.exe should contain a resource of type '14' in it.", names.containsKey("14"));
        Assert.assertTrue("resource type 14 should have a name named ICO_MYCOMPUTER associated with it.", names.get("14").contains("ICO_MYCOMPUTER"));
    }

    @Test
    public void testGetModules() {
        List<MODULEENTRY32W> results = Kernel32Util.getModules(Kernel32.INSTANCE.GetCurrentProcessId());

        // not sure if this will be run against java.exe or javaw.exe but these checks should work with both
        Assert.assertNotNull("There should be some modules returned from this helper", results);
        Assert.assertTrue("The first module in this process should be java.exe or javaw.exe", results.get(0).szModule().startsWith("java"));

        // since this is supposed to return all the modules in a process, there should be an EXE and at least 1 Windows DLL
        // so assert total count is at least two
        Assert.assertTrue("This is supposed to return all the modules in a process, so there should be an EXE and at least 1 Windows API DLL.", results.size() > 2);
    }

    @Test
    public void testExpandEnvironmentStrings() {
        Kernel32.INSTANCE.SetEnvironmentVariable("DemoVariable", "DemoValue");
        Assert.assertEquals("DemoValue", Kernel32Util.expandEnvironmentStrings("%DemoVariable%"));
    }

    @Test
    public void testGetLogicalProcessorInformation() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] procInfo = Kernel32Util
                .getLogicalProcessorInformation();
        Assert.assertTrue(procInfo.length > 0);
    }

    @Test
    @Ignore("java.lang.IllegalStateException: Unmapped relationship.")
    public void testGetLogicalProcessorInformationEx() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        List<GROUP_RELATIONSHIP> groups = new ArrayList<>();
        List<PROCESSOR_RELATIONSHIP> packages = new ArrayList<>();
        List<NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
        List<CACHE_RELATIONSHIP> caches = new ArrayList<>();
        List<PROCESSOR_RELATIONSHIP> cores = new ArrayList<>();

        for (int i = 0; i < procInfo.length; i++) {
            // Build list from relationship
            switch (procInfo[i].relationship) {
                case LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                    groups.add((GROUP_RELATIONSHIP) procInfo[i]);
                    break;
                case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                    packages.add((PROCESSOR_RELATIONSHIP) procInfo[i]);
                    break;
                case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                    numaNodes.add((NUMA_NODE_RELATIONSHIP) procInfo[i]);
                    break;
                case LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                    caches.add((CACHE_RELATIONSHIP) procInfo[i]);
                    break;
                case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                    cores.add((PROCESSOR_RELATIONSHIP) procInfo[i]);
                    break;
                default:
                    throw new IllegalStateException("Unmapped relationship.");
            }
            // Test that native provided size matches JNA structure size
            Assert.assertEquals(procInfo[i].size, procInfo[i].size());
        }

        // Test that getting all relations matches the same totals as
        // individuals.
        Assert.assertEquals(groups.size(), Kernel32Util
                .getLogicalProcessorInformationEx(LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup).length);
        Assert.assertEquals(packages.size(), Kernel32Util.getLogicalProcessorInformationEx(
                LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage).length);
        Assert.assertEquals(numaNodes.size(), Kernel32Util
                .getLogicalProcessorInformationEx(LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode).length);
        Assert.assertEquals(caches.size(), Kernel32Util
                .getLogicalProcessorInformationEx(LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache).length);
        Assert.assertEquals(cores.size(), Kernel32Util
                .getLogicalProcessorInformationEx(LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore).length);

        // Test GROUP_RELATIONSHIP
        Assert.assertEquals(1, groups.size()); // Should only be one group structure
        for (GROUP_RELATIONSHIP group : groups) {
            Assert.assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup, group.relationship);
            Assert.assertTrue(group.activeGroupCount <= group.maximumGroupCount);
            Assert.assertEquals(group.activeGroupCount, group.groupInfo.length);
            for (int j = 0; j < group.activeGroupCount; j++) {
                Assert.assertTrue(group.groupInfo[j].activeProcessorCount <= group.groupInfo[j].maximumProcessorCount);
                Assert.assertEquals(group.groupInfo[j].activeProcessorCount, Long.bitCount(group.groupInfo[j].activeProcessorMask.longValue()));
                Assert.assertTrue(group.groupInfo[j].maximumProcessorCount <= 64);
            }
        }

        // Test PROCESSOR_RELATIONSHIP packages
        Assert.assertTrue(cores.size() >= packages.size());
        for (PROCESSOR_RELATIONSHIP pkg : packages) {
            Assert.assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage, pkg.relationship);
            Assert.assertEquals(0, pkg.flags); // packages have 0 flags
            Assert.assertEquals(0, pkg.efficiencyClass); // packages have 0 efficiency
            Assert.assertEquals(pkg.groupCount, pkg.groupMask.length);
        }

        // Test PROCESSOR_RELATIONSHIP cores
        for (PROCESSOR_RELATIONSHIP core : cores) {
            Assert.assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore, core.relationship);
            // Hyperthreading flag set if at least 2 logical processors
            Assert.assertTrue(Long.bitCount(core.groupMask[0].mask.longValue()) > 0);
            if (Long.bitCount(core.groupMask[0].mask.longValue()) > 1) {
                Assert.assertEquals(WinNT.LTP_PC_SMT, core.flags);
            } else {
                Assert.assertEquals(0, core.flags);
            }
            // Cores are always in one group
            Assert.assertEquals(1, core.groupCount);
            Assert.assertEquals(1, core.groupMask.length);
        }

        // Test NUMA_NODE_RELATIONSHIP
        for (NUMA_NODE_RELATIONSHIP numaNode : numaNodes) {
            Assert.assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode, numaNode.relationship);
            Assert.assertTrue(numaNode.nodeNumber >= 0);
        }

        // Test CACHE_RELATIONSHIP
        for (CACHE_RELATIONSHIP cache : caches) {
            Assert.assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache, cache.relationship);
            Assert.assertTrue(cache.level >= 1);
            Assert.assertTrue(cache.level <= 4);
            Assert.assertTrue(cache.cacheSize > 0);
            Assert.assertTrue(cache.lineSize > 0);
            Assert.assertTrue(cache.type == PROCESSOR_CACHE_TYPE.CacheUnified
                    || cache.type == PROCESSOR_CACHE_TYPE.CacheInstruction
                    || cache.type == PROCESSOR_CACHE_TYPE.CacheData || cache.type == PROCESSOR_CACHE_TYPE.CacheTrace);
            Assert.assertTrue(cache.associativity == WinNT.CACHE_FULLY_ASSOCIATIVE || cache.associativity > 0);
        }
    }

    @Test
    public void testGetCurrentProcessPriority() {
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32Util.getCurrentProcessPriority()));
    }

    @Test
    public void testSetCurrentProcessPriority() {
        Kernel32Util.setCurrentProcessPriority(Kernel32.HIGH_PRIORITY_CLASS);
    }

    @Test
    public void testSetCurrentProcessBackgroundMode() {
        try {
            Kernel32Util.setCurrentProcessBackgroundMode(true);
        } finally {
            try {
                Kernel32Util.setCurrentProcessBackgroundMode(false); // Reset the "background" mode!
            } catch (Exception e) { }
        }
    }

    @Test
    public void testGetCurrentThreadPriority() {
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32Util.getCurrentThreadPriority()));
    }

    @Test
    public void testSetCurrentThreadPriority() {
        Kernel32Util.setCurrentThreadPriority(Kernel32.THREAD_PRIORITY_ABOVE_NORMAL);
    }

    @Test
    public void testSetCurrentThreadBackgroundMode() {
        try {
            Kernel32Util.setCurrentThreadBackgroundMode(true);
        } finally {
            try {
                Kernel32Util.setCurrentThreadBackgroundMode(false); // Reset the "background" mode!
            } catch (Exception e) { }
        }
    }

    @Test
    public void testGetProcessPriority() {
        final int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32Util.getProcessPriority(pid)));
    }

    @Test
    public void testSetProcessPriority() {
        final int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        Kernel32Util.setProcessPriority(pid, Kernel32.HIGH_PRIORITY_CLASS);
    }

    @Test
    public void testGetThreadPriority() {
        final int tid = Kernel32.INSTANCE.GetCurrentThreadId();
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32Util.getThreadPriority(tid)));
    }

    @Test
    public void testSetThreadPriority() {
        final int tid = Kernel32.INSTANCE.GetCurrentThreadId();
        Kernel32Util.setThreadPriority(tid, Kernel32.THREAD_PRIORITY_ABOVE_NORMAL);
    }

    @Test
    public void testIsValidPriorityClass() {
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32.NORMAL_PRIORITY_CLASS));
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32.IDLE_PRIORITY_CLASS));
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32.HIGH_PRIORITY_CLASS));
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32.REALTIME_PRIORITY_CLASS));
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32.BELOW_NORMAL_PRIORITY_CLASS));
        Assert.assertTrue(Kernel32Util.isValidPriorityClass(Kernel32.ABOVE_NORMAL_PRIORITY_CLASS));
        Assert.assertFalse(Kernel32Util.isValidPriorityClass(new DWORD(0L)));
        Assert.assertFalse(Kernel32Util.isValidPriorityClass(new DWORD(1L)));
        Assert.assertFalse(Kernel32Util.isValidPriorityClass(new DWORD(0xFFFFFFFF)));
        Assert.assertFalse(Kernel32Util.isValidPriorityClass(Kernel32.PROCESS_MODE_BACKGROUND_BEGIN));
        Assert.assertFalse(Kernel32Util.isValidPriorityClass(Kernel32.PROCESS_MODE_BACKGROUND_END));
    }

    @Test
    public void testIsValidThreadPriority() {
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_IDLE));
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_LOWEST));
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_BELOW_NORMAL));
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_NORMAL));
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_ABOVE_NORMAL));
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_HIGHEST));
        Assert.assertTrue(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_PRIORITY_TIME_CRITICAL));
        Assert.assertFalse(Kernel32Util.isValidThreadPriority(  3));
        Assert.assertFalse(Kernel32Util.isValidThreadPriority( -3));
        Assert.assertFalse(Kernel32Util.isValidThreadPriority( 16));
        Assert.assertFalse(Kernel32Util.isValidThreadPriority(-16));
        Assert.assertFalse(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_MODE_BACKGROUND_BEGIN));
        Assert.assertFalse(Kernel32Util.isValidThreadPriority(Kernel32.THREAD_MODE_BACKGROUND_END));
    }
}
