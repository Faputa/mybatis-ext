package io.github.mybatisext.idgenerator;

import java.util.UUID;

public class UuidGenerator implements IdGenerator<String> {

    @Override
    public String getId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
