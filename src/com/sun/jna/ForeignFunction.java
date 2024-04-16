package com.sun.jna;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;

public class ForeignFunction {

    private final Supplier<SymbolLookup> supplier;
    private final String name;
    private final FunctionDescriptor fd;
    private MethodHandle mh;

    public ForeignFunction(Library library, String name, FunctionDescriptor fd) {
        this.supplier = () -> Native.getSymbolLookup(library);
        this.name = name;
        this.fd = fd;
    }

    public ForeignFunction(Supplier<SymbolLookup> supplier, String name, FunctionDescriptor fd) {
        this.supplier = supplier;
        this.name = name;
        this.fd = fd;
    }

    public MethodHandle get() {
        if (mh == null) {
            SymbolLookup symbolLookup = supplier.get();
            mh = Linker.nativeLinker()
                    .downcallHandle(
                            symbolLookup.find(name).orElseThrow(),
                            fd);
        }
        return mh;
    }

}
