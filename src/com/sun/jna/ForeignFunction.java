package com.sun.jna;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static java.lang.foreign.ValueLayout.*;

public class ForeignFunction {

    private final Supplier<SymbolLookup> symbolLookupSupplier;
    private final String name;
    MemorySegment symbolAddress;
    FunctionDescriptor fd;
    MethodHandle callHandle;

//    private final MemorySegment symbolAddress;
//    private MethodHandle downcallHandle;
    Class<?> lastReturnType; // some functions are called with different parameters and return types
    java.util.function.Function<Object,Object> resultConverter;
//    private FunctionDescriptor functionDescriptor;

    public ForeignFunction(String name) {
        this.symbolLookupSupplier = null;
        this.name = name;
    }

    public ForeignFunction(NativeLibrary library, String name, MemorySegment symbolAddress) {
        this.symbolLookupSupplier = library::getSymbolLookup;
        this.name = name;
        this.symbolAddress = symbolAddress;
    }

    public ForeignFunction(Library library, String name, FunctionDescriptor fd) {
        this.symbolLookupSupplier = () -> Native.getSymbolLookup(library);
        this.name = name;
        this.fd = fd;
    }

    public ForeignFunction(Supplier<SymbolLookup> supplier, String name, FunctionDescriptor fd) {
        this.symbolLookupSupplier = supplier;
        this.name = name;
        this.fd = fd;
    }

    public MethodHandle get() {
        if (callHandle == null) {
            SymbolLookup symbolLookup = symbolLookupSupplier.get();
            if (symbolAddress == null) {
                symbolAddress = symbolLookup.find(name).orElseThrow();
            }
            callHandle = Linker.nativeLinker()
                    .downcallHandle(symbolAddress, fd);
        }
        return callHandle;
    }

    public void initFunctionDescriptor(Class<?> returnType, boolean allowObjects,
                                       Object[] args, Class<?>[] paramTypes, String encoding)  {
        lastReturnType = returnType;
        resultConverter = null;
        MemoryLayout resLayout;
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            resLayout = null;
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            resLayout = JAVA_BOOLEAN;
        } else if (returnType == byte.class || returnType == Byte.class) {
            resLayout = JAVA_BYTE;
        } else if (returnType == short.class || returnType == Short.class) {
            resLayout = JAVA_SHORT;
        } else if (returnType == char.class || returnType == Character.class) {
            resLayout = JAVA_CHAR;
        } else if (returnType == int.class || returnType == Integer.class) {
            resLayout = JAVA_INT;
        } else if (returnType == long.class || returnType == Long.class) {
            resLayout = JAVA_LONG;
        } else if (returnType == float.class || returnType == Float.class) {
            resLayout = JAVA_FLOAT;
        } else if (returnType == double.class || returnType == Double.class) {
            resLayout = JAVA_DOUBLE;
        } else if (returnType == String.class) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        return Pointer.reinterpret(seg, -1)
                                .getString(0, Charset.forName(encoding));
                    }
                }
                return null;
            };
        } else if (returnType == WString.class) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        String s = Pointer.reinterpret(seg, -1)
                                .getString(0, StandardCharsets.UTF_16LE);
                        if (s != null) {
                            return new WString(s);
                        }
                    }
                }
                return null;
            };
        } else if (Pointer.class.isAssignableFrom(returnType)) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        return new Pointer(null, seg);
                    }
                }
                return null;
            };
        } else if (Structure.class.isAssignableFrom(returnType)) {
            resLayout = ADDRESS;
            if (Structure.ByValue.class.isAssignableFrom(returnType)) {
                Structure structure = Structure.newInstance((Class<? extends Structure>) returnType);
                if (structure.size() > JAVA_LONG.byteSize()) {
//                    resLayout = ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(structure.size(), JAVA_BYTE));
                    throw new UnsupportedOperationException("Structure size " + structure.size()
                            + " > " + JAVA_LONG.byteSize());
                }
                resultConverter = o -> {
                    if (o instanceof MemorySegment seg) {
//                            Pointer p = new Pointer(null, seg.reinterpret(1024 * 1024));
//                            Native.invokeStructure(this, this.peer, callFlags, args,
//                                    Structure.newInstance((Class<? extends Structure>) returnType));
                        long address = seg.address();// address is the value it seems
                        int byteSize = (int) JAVA_LONG.byteSize();
                        byte[] bytes = new byte[byteSize];
                        for (int i = 0; i < byteSize; i++) {
                            bytes[i] = (byte) address;
                            address >>= 8;
                        }
                        seg = MemorySegment.ofArray(bytes);
                        Pointer p = new Pointer(null, seg);
                        Structure s = Structure.newInstance((Class<? extends Structure>) returnType, p);
                        s.useMemory(p);
                        s.autoRead();
                        return s;
                    }
                    return null;
                };
            } else {
                resultConverter = o -> {
                    if (o instanceof MemorySegment seg) {
                        if (seg.address() != 0) {
                            Pointer p = new Pointer(null, Pointer.reinterpret(seg, -1));
                            Structure s = Structure.newInstance((Class<? extends Structure>) returnType, p);
                            s.conditionalAutoRead();
                            return s;
                        }
                    }
                    return null;
                };
            }
        } else if (Callback.class.isAssignableFrom(returnType)) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        Pointer p = new Pointer(null, Pointer.reinterpret(seg, ADDRESS.byteSize()));
                        return CallbackReference.getCallback(returnType, p);
                    }
                }
                return null;
            };
        } else if (returnType == String[].class) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        Pointer p = new Pointer(null, Pointer.reinterpret(seg, -1));
                        return p.getStringArray(0, encoding);
                    }
                }
                return null;
            };
        } else if (returnType == WString[].class) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        Pointer p = new Pointer(null, seg);
                        String[] arr = p.getWideStringArray(0);
                        WString[] warr = new WString[arr.length];
                        for (int i = 0; i < arr.length; i++) {
                            warr[i] = new WString(arr[i]);
                        }
                        return warr;
                    }
                }
                return null;
            };
        } else if (returnType == Pointer[].class) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        Pointer p = new Pointer(null, seg);
                        return p.getPointerArray(0);
                    }
                }
                return null;
            };
        } else if (allowObjects) {
            resLayout = ADDRESS;
            resultConverter = o -> {
                if (o instanceof MemorySegment seg) {
                    if (seg.address() != 0) {
                        if (!returnType.isAssignableFrom(o.getClass())) {
                            throw new ClassCastException("Return type " + returnType
                                    + " does not match result "
                                    + o.getClass());
                        }
                    }
                }
                return null;
            };
        } else {
            throw new IllegalArgumentException("Unsupported return type " + returnType + " in function " + name);
        }
        MemoryLayout[] argLayouts = null;
        if (paramTypes != null) {
            argLayouts = new MemoryLayout[paramTypes.length];
            for (int j = 0; j < paramTypes.length; j++) {
                Class<?> paramType = paramTypes[j];
                ValueLayout valueLayout = ADDRESS;
                if (paramType != null) {
                    if (Byte.class.equals(paramType) || byte.class.equals(paramType)) {
                        valueLayout = JAVA_BYTE;
                    } else if (Character.class.equals(paramType) || char.class.equals(paramType)) {
                        valueLayout = JAVA_CHAR;
                    } else if (Short.class.equals(paramType) || short.class.equals(paramType)) {
                        valueLayout = JAVA_SHORT;
                    } else if (Integer.class.equals(paramType) || int.class.equals(paramType)) {
                        valueLayout = JAVA_INT;
                    } else if (Long.class.equals(paramType) || long.class.equals(paramType)) {
                        valueLayout = JAVA_LONG;
                    } else if (Float.class.equals(paramType) || float.class.equals(paramType)) {
                        valueLayout = JAVA_FLOAT;
                    } else if (Double.class.equals(paramType) || double.class.equals(paramType)) {
                        valueLayout = JAVA_DOUBLE;
                    } else if (Structure.ByValue.class.isAssignableFrom(paramType)) {
                        valueLayout = JAVA_LONG; // TODO
                    }
                }
                argLayouts[j] = valueLayout;
            }
        }
        if (args != null) {
            argLayouts = new MemoryLayout[args.length];
            for (int j = 0; j < args.length; j++) {
                Object a = args[j];
                argLayouts[j] = switch (a) {
                    case null -> ADDRESS;
                    case Byte _ -> JAVA_BYTE;
                    case Character _ -> JAVA_CHAR;
                    case Short _ -> JAVA_SHORT;
                    case Integer _ -> JAVA_INT;
                    case Long _ -> JAVA_LONG;
                    case Float _ -> JAVA_FLOAT;
                    case Double _ -> JAVA_DOUBLE;
//                        case MemorySegmentReference ref -> {
//                            if (ref.segment != null) {
//                                yield ADDRESS;
//                            } else {
//                                MemorySegment.ofAddress(1234);
//                                yield JAVA_LONG;
//                            }
//                        }
                    case Structure.ByValue _ -> JAVA_LONG; // TODO
                    default -> ADDRESS;
                };
            }
        }
        if (argLayouts != null) {
            if (resLayout == null) {
                fd = FunctionDescriptor.ofVoid(argLayouts);
            } else {
                fd = FunctionDescriptor.of(resLayout, argLayouts);
            }
        } else {
            if (resLayout == null) {
                fd = FunctionDescriptor.ofVoid();
            } else {
                fd = FunctionDescriptor.of(resLayout);
            }
        }
    }

    void initDownCallHandle() {
        if (symbolAddress == null) {
            SymbolLookup symbolLookup = symbolLookupSupplier.get();
            symbolAddress = symbolLookup.find(name).orElseThrow();
        }
        callHandle = Linker.nativeLinker().downcallHandle(symbolAddress, fd);
    }

    public void initUpcallStub() {
        symbolAddress = Linker.nativeLinker().upcallStub(callHandle, fd, Native.arenaAuto());
    }

}
