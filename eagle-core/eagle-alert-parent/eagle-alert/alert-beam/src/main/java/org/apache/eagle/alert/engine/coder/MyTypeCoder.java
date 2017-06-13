package org.apache.eagle.alert.engine.coder;

import org.apache.beam.sdk.coders.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MyTypeCoder extends AtomicCoder<MyType> {

    private static final StringUtf8Coder STRING_CODER = StringUtf8Coder.of();
    private static final ByteArrayCoder BYTE_ARRAY_CODER = ByteArrayCoder.of();
    private static final InstantCoder INSTANT_CODER = InstantCoder.of();
    private static final VarLongCoder VAR_LONG_CODER = VarLongCoder.of();

    public static MyTypeCoder of() {
        return new MyTypeCoder();
    }

    @Override
    public void encode(MyType myType, OutputStream outputStream) throws IOException {
        STRING_CODER.encode(myType.getType(), outputStream);
        VAR_LONG_CODER.encode(myType.getValue(), outputStream);
    }

    @Override
    public MyType decode(InputStream inputStream) throws IOException {
        String type = STRING_CODER.decode(inputStream);
        long value = VAR_LONG_CODER.decode(inputStream);
        MyType mytype = new MyType();
        mytype.setType(type);
        mytype.setValue(value);
        return mytype;
    }
}
