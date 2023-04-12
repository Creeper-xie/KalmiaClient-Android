package com.github.cao.awa.kalmia.user;

import com.github.cao.awa.apricot.io.bytes.reader.BytesReader;

import java.util.Arrays;

public abstract class User {
    public abstract byte[] toBytes();

    public static User create(byte[] data) {
        System.out.println(Arrays.toString(data));

        DeletedUser deleted = createDeleted(data);
        if (deleted == null) {
            return createDefault(data);
        }
        return deleted;
    }

    public static DeletedUser createDeleted(byte[] data) {
        return DeletedUser.create(new BytesReader(data));
    }

    public static DefaultUser createDefault(byte[] data) {
        return DefaultUser.create(new BytesReader(data));
    }
}
