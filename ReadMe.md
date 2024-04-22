Java Native Access Foreign (JNAF)
=================================

This is a fork of JNA project using JDK 22 Foreign Functions and Memory (FFM) API (java.lang.foreign).

JNI is not used at all, unless system property jna.jni = true. 
This can be used to run unit tests and compare functionality with pure Java version (jna.jni = false).

Status of JNA unit tests:
Tests failed: 206, passed: 431, ignored: 24 of 661 tests

Status of Win32 platform unit tests:
Tests failed: 143, passed: 502, ignored: 13 of 658 tests (COM tests skipped)

Projects Using JNA
==================
None.
JNAF is hobby project, hopefully useful for someone.

Supported Platforms
===================
Tested on Win32 only.
GetLastError supports only Win32.
The standard JNA makefile can be used to create jnidispatch.dll and other test libraries used in unit tests.

Download
========

JNA: Version 5.14.0
JNAF: 7.0.1 (I don't know why, JNA makefile used this version for jnidispatch.dll)

JNAF
----

This is pure Maven project, contrary to JNA, which is built using Ant.
See: pom-jnaf.xml

Maven: net.java.dev.jnaf:jna.jar

This is the core artifact of JNAF (does not contain or need jnidispatch library). Pure Java 22.

JNAF Platform
-------------

Pure Maven project.
See: contrib/platform/pom-jnaf-platform.xml

Maven: net.java.dev.jnaf:jna-platform.jar

Same as normal JNA Platform but uses JNAF instead. Java 22.

Features
========

Most standard JNA features work.
Does not work:
* By-value structures larger than 8 bytes (8 bytes fit into Java long) 
* Java objects (native libraries normally do not use or return Java objects)
* Varargs

Community and Support
=====================

All questions should be posted to the [jna-users Google group](http://groups.google.com/group/jna-users). 
Issues can be submitted [here on Github](https://github.com/Fuerte1/jna-foreign/issues).

Contributing
============

You're encouraged to contribute to JNAF. :-)

License
=======

This library is licensed under the LGPL, version 2.1 or later, or (from version 4.0 onward) the Apache License, version 2.0. 
