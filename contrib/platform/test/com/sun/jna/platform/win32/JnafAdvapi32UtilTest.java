package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static org.junit.jupiter.api.Assertions.*;

class JnafAdvapi32UtilTest {

    @BeforeAll
    public static void beforeAll() {
        assertEquals(0, Kernel32.INSTANCE.GetLastError());
    }

    @Test
    public void readAll() {
        Map<String, Object> values = Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER,
                "Software\\Jnaf\\UnitTest");
        assertNotNull(values);
        if (values.isEmpty()) {
            return;
        }
        assertEquals("Jnaf Unit Test", values.get("")); // "" = jna, "@" = REG
        assertEquals(1234567890, values.get("DWORD"));
        assertEquals(1234567890123456789L, values.get("QWORD"));
        assertEquals("String value", values.get("String"));
        assertEquals("Unicode åäö€", values.get("String åäö€"));
        assertEquals("åäö€", values.get("Expandable String Value"));
        assertArrayEquals(new String[]{"åäö€", "1", "23", "456", "7890"}, (Object[]) values.get("Multi-String Value"));
        assertArrayEquals(new byte[]{(byte) 0xa1, (byte) 0xb2, (byte) 0xc3, (byte) 0xd4, (byte) 0xe5, (byte) 0xf6, 0x07},
                (byte[]) values.get("Binary"));
    }

    @Test
    public void readOne() {
        assertEquals("Unicode åäö€",
                Advapi32Util.registryGetStringValue(
                        HKEY_CURRENT_USER, "Software\\Jnaf\\UnitTest", "String åäö€")
        );
    }

    @Test
    public void readMissingKey() {
        try {
            Advapi32Util.registryGetValues(WinReg.HKEY_CURRENT_USER,
                    "Software\\Jnaf\\UnitTest\\MISSING");
            fail();
        } catch (Exception e) {
            if (!e.getMessage().startsWith("The system cannot find the file specified.")) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void readKnownValues() {
        Map<String, Object> values = Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE,
                "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion");
        assertNotNull(values);
        String value = (String) values.get("ProgramFilesDir");
        assertNotNull(value);
        assertEquals("C:\\Program Files (x86)", value);
    }

    @Test
    public void getKeys() {
        String[] keys = Advapi32Util.registryGetKeys(HKEY_CURRENT_USER, "Software\\Jnaf\\UnitTest");
        assertEquals(2, keys.length);
        assertEquals("Sub key", keys[0]);
        assertEquals("Sub åäö€", keys[1]);
        keys = Advapi32Util.registryGetKeys(HKEY_CURRENT_USER, "Software\\Jnaf\\UnitTest\\Sub åäö€");
        assertEquals(0, keys.length);
    }

    @Test
    public void getLastError() {
        assertEquals(0, Kernel32.INSTANCE.GetLastError());
        assertEquals("The system cannot find the path specified.",
                Kernel32Util.formatMessage(3));
        assertEquals(0, Kernel32.INSTANCE.GetLastError());
        assertEquals(0, Kernel32.INSTANCE.GetLastError());
        assertEquals(0, Native.getLastErrorFfm());
    }

    @Test
    public void getUserName() {
        assertEquals("harry", Advapi32Util.getUserName());
    }

    @Test
    public void getAccountByName() {
        Advapi32Util.Account account = Advapi32Util.getAccountByName("harry");
        assertNotNull(account);
        assertEquals(1, account.accountType);
        assertEquals("harry", account.name);
        assertEquals("DESKTOP-B8CLTBS", account.domain);
        assertEquals("DESKTOP-B8CLTBS\\harry", account.fqn);
        assertEquals(28, account.sid.length);
        assertEquals("S-1-5-21-4160134898-1683524527-1113683188-1001", account.sidString);
        account = Advapi32Util.getAccountBySid(account.sidString); // EXCEPTION_ACCESS_VIOLATION when debugging
        assertNotNull(account);
        assertEquals("harry", account.name);
        for (int i = WinNT.WELL_KNOWN_SID_TYPE.WinNullSid;
             i <= WinNT.WELL_KNOWN_SID_TYPE.WinBuiltinEventLogReadersGroup; i++) {
            assertFalse(Advapi32Util.isWellKnownSid(account.sidString, i));
            assertFalse(Advapi32Util.isWellKnownSid(account.sid, i));
        }
    }

    @Test
    public void getAceSize() {
        assertEquals(8, Advapi32Util.getAceSize(0));
        assertEquals(9, Advapi32Util.getAceSize(1));
        assertEquals(10, Advapi32Util.getAceSize(2));
    }

    @Test
    public void getTokenGroups() {
        WinNT.HANDLE threadHandle = Kernel32.INSTANCE.GetCurrentThread();
        assertNotNull(threadHandle);
        Advapi32Util.Account[] tokenGroups = Advapi32Util.getTokenGroups(threadHandle);
        assertNull(tokenGroups);
    }

    @Test
    public void getCurrentUserGroups() {
        Advapi32Util.Account[] currentUserGroups = Advapi32Util.getCurrentUserGroups();
        assertNotNull(currentUserGroups);
        assertEquals(0, currentUserGroups.length);
    }

    @Test
    public void registryKeyExists() {
        assertTrue(Advapi32Util.registryKeyExists(HKEY_CURRENT_USER, "Software\\Jnaf\\UnitTest"));
        assertFalse(Advapi32Util.registryKeyExists(HKEY_CURRENT_USER, "Software\\Jnaf\\UnitTestX"));
    }

}