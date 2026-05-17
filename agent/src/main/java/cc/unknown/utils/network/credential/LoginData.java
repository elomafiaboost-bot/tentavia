/*
 * Decompiled with CFR 0.152.
 */
package cc.unknown.utils.network.credential;

public class LoginData {
    public String mcToken;
    public String newRefreshToken;
    public String uuid;
    public String username;

    public LoginData(String mcToken, String newRefreshToken, String uuid, String username) {
        this.mcToken = mcToken;
        this.newRefreshToken = newRefreshToken;
        this.uuid = uuid;
        this.username = username;
    }
}

