/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.audio.ISound
 *  net.minecraft.client.audio.PositionedSoundRecord
 *  net.minecraft.client.gui.GuiMainMenu
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiTextField
 *  net.minecraft.client.gui.ScaledResolution
 *  net.minecraft.util.EnumChatFormatting
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.Session
 */
package cc.unknown.ui;

import cc.unknown.mixin.interfaces.IMinecraft;
import cc.unknown.utils.client.RenderUtil;
import cc.unknown.utils.network.credential.CookieUtil;
import cc.unknown.utils.network.credential.LoginData;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;
import java.awt.Color;
import java.io.IOException;
import java.util.Random;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

public class AltLoginScreen
extends GuiScreen {
    private GuiTextField email;
    private GuiTextField password;
    private final Button[] buttons = new Button[]{new Button("Login"), new Button("Cookie Login"), new Button("Random Username"), new Button("Back")};
    private String status;

    public void func_73866_w_() {
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        int buttonHeight = 20;
        int totalHeight = buttonHeight * this.buttons.length;
        int y = Math.max(sr.func_78328_b() / 2 - totalHeight / 2 - 50, 75);
        this.email = new GuiTextField(0, this.field_146297_k.field_71466_p, sr.func_78326_a() / 2 - 80, y, 160, 20);
        this.password = new GuiTextField(1, this.field_146297_k.field_71466_p, sr.func_78326_a() / 2 - 80, y + 30, 160, 20);
        for (Button button : this.buttons) {
            button.updateState(false);
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        RenderUtil.drawRect(0.0, 0.0, this.field_146294_l, this.field_146295_m, new Color(0).getRGB());
        super.func_73863_a(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        this.email.func_146194_f();
        this.password.func_146194_f();
        int buttonWidth = 120;
        int buttonHeight = 20;
        int totalHeight = buttonHeight * this.buttons.length;
        double y = Math.max((double)(sr.func_78328_b() / 2) - (double)totalHeight * 0.2, 140.0);
        double titleY = Math.max(sr.func_78328_b() / 2 - totalHeight / 2 - 110, 20);
        String altLogin = "Alt login";
        this.field_146297_k.field_71466_p.func_175063_a(altLogin, (float)(sr.func_78326_a() / 2 - this.field_146297_k.field_71466_p.func_78256_a(altLogin) / 2), (float)titleY, -1);
        this.field_146297_k.field_71466_p.func_175063_a(this.status, (float)(sr.func_78326_a() / 2 - this.field_146297_k.field_71466_p.func_78256_a(this.status) / 2), (float)(titleY + 25.0), -1);
        int startX = sr.func_78326_a() / 2 - buttonWidth / 2;
        int endX = sr.func_78326_a() / 2 + buttonWidth / 2;
        for (Button button : this.buttons) {
            RenderUtil.drawRect(startX, y, endX, y + (double)buttonHeight, 0x50000000);
            button.updateState(mouseX > startX && mouseX < endX && (double)mouseY > y && (double)mouseY < y + (double)buttonHeight);
            if (button.isHovered()) {
                double scale = 1.0;
                RenderUtil.drawRect(startX, y, (double)startX + (double)buttonWidth * scale, y + (double)buttonHeight, new Color(0, 0, 0).getRGB());
            }
            String buttonName = button.getName();
            this.field_146297_k.field_71466_p.func_175063_a(buttonName, (float)(sr.func_78326_a() / 2 - this.field_146297_k.field_71466_p.func_78256_a(buttonName) / 2), (float)(y + 6.0), new Color(220, 220, 220).getRGB());
            y += (double)buttonHeight;
        }
    }

    public void func_73869_a(char typedChar, int keyCode) {
        try {
            super.func_73869_a(typedChar, keyCode);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.email.func_146201_a(typedChar, keyCode);
        this.password.func_146201_a(typedChar, keyCode);
    }

    public void func_73864_a(int mouseX, int mouseY, int mouseButton) {
        try {
            super.func_73864_a(mouseX, mouseY, mouseButton);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.email.func_146192_a(mouseX, mouseY, mouseButton);
        this.password.func_146192_a(mouseX, mouseY, mouseButton);
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        int buttonWidth = 120;
        int buttonHeight = 20;
        int totalHeight = buttonHeight * this.buttons.length;
        double y = Math.max((double)(sr.func_78328_b() / 2) - (double)totalHeight * 0.2, 140.0);
        int startX = sr.func_78326_a() / 2 - buttonWidth / 2;
        int endX = sr.func_78326_a() / 2 + buttonWidth / 2;
        for (Button button : this.buttons) {
            if (mouseX > startX && mouseX < endX && (double)mouseY > y && (double)mouseY < y + (double)buttonHeight) {
                switch (button.getName()) {
                    case "Login": {
                        new Thread(() -> {
                            if (this.password.func_146179_b().isEmpty()) {
                                ((IMinecraft)this.field_146297_k).setSession(new Session(this.email.func_146179_b(), "none", "none", "mojang"));
                                this.status = "Logged into " + this.email.func_146179_b() + " - cracked account";
                            } else {
                                this.status = EnumChatFormatting.YELLOW + "Waiting for login...";
                                MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
                                MicrosoftAuthResult result = null;
                                try {
                                    result = authenticator.loginWithCredentials(this.email.func_146179_b(), this.password.func_146179_b());
                                    MinecraftProfile profile = result.getProfile();
                                    ((IMinecraft)this.field_146297_k).setSession(new Session(profile.getName(), profile.getId(), result.getAccessToken(), "microsoft"));
                                    this.status = "Logged in to " + this.field_146297_k.func_110432_I().func_111285_a();
                                }
                                catch (MicrosoftAuthenticationException e) {
                                    e.printStackTrace();
                                    this.status = EnumChatFormatting.RED + "Login failed !";
                                }
                            }
                        }).start();
                        break;
                    }
                    case "Cookie Login": {
                        new Thread(() -> {
                            this.status = EnumChatFormatting.YELLOW + "Waiting for login...";
                            try {
                                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }
                            JDialog dialog = new JDialog();
                            dialog.setAlwaysOnTop(true);
                            JFileChooser chooser = new JFileChooser();
                            FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");
                            chooser.setFileFilter(filter);
                            dialog.add(chooser);
                            int returnVal = chooser.showOpenDialog(null);
                            if (returnVal == 0) {
                                try {
                                    this.status = EnumChatFormatting.YELLOW + "Logging in...";
                                    LoginData loginData = CookieUtil.instance.loginWithCookie(chooser.getSelectedFile());
                                    if (loginData == null) {
                                        this.status = EnumChatFormatting.RED + "Failed to login with cookie!";
                                        return;
                                    }
                                    this.status = EnumChatFormatting.GREEN + "Logged in to " + loginData.username;
                                    ((IMinecraft)this.field_146297_k).setSession(new Session(loginData.username, loginData.uuid, loginData.mcToken, "legacy"));
                                }
                                catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                finally {
                                    dialog.dispose();
                                }
                            }
                        }).start();
                        break;
                    }
                    case "Random Username": {
                        String chars = "abcdefghijklmnopqrstuvwxyz1234567890";
                        StringBuilder salt = new StringBuilder();
                        Random rnd = new Random();
                        int saltLength = this.getRandomInRange(8, 16);
                        while (salt.length() < saltLength) {
                            int index = (int)(rnd.nextFloat() * (float)chars.length());
                            salt.append(chars.charAt(index));
                        }
                        ((IMinecraft)this.field_146297_k).setSession(new Session(salt.toString(), "none", "none", "mojang"));
                        this.status = "Logged into " + salt.toString() + " - cracked account";
                        break;
                    }
                    case "Back": {
                        this.field_146297_k.func_147108_a((GuiScreen)new GuiMainMenu());
                    }
                }
                this.field_146297_k.func_147118_V().func_147682_a((ISound)PositionedSoundRecord.func_147674_a((ResourceLocation)new ResourceLocation("gui.button.press"), (float)1.0f));
            }
            y += (double)buttonHeight;
        }
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private int getRandomInRange(int min, int max) {
        return (int)(Math.random() * (double)(max - min) + (double)min);
    }

    final class Button {
        private String name;
        private boolean hovered;

        public Button(String name) {
            this.name = name;
            this.hovered = false;
        }

        public void updateState(boolean state) {
            if (this.hovered != state) {
                this.hovered = state;
            }
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public void setHovered(boolean hovered) {
            this.hovered = hovered;
        }
    }
}

