package com.bkx.lab.model.store;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Data;

@Data
public class ImageData extends RealmObject {

    public static final long DEFAULT_ID = 1L;
    public static final String ID = "id";
    public static final String ABSOLUTE_PATH = "absolutePath";
    public static final String CURRENT_PATH = "currentPath";

    @PrimaryKey
    private Long id;
    private String absolutePath;
    private String currentPath;
}
