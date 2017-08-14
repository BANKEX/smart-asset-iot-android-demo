package com.demo.bankexdh.model.store;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Data;

@Data
public class UserModel extends RealmObject {
    public static final long DEFAULT_ID = 1L;
    public static final String ID = "id";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";

    @PrimaryKey
    private Long id;
    private String accessToken;
    private String refreshToken;
    private Boolean isEnabled;

}
