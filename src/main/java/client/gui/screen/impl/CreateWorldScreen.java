package client.gui.screen.impl;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Color;

import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import client.gui.screen.Screen;
import client.gui.screen.components.ButtonComponent;
import client.gui.screen.components.FieldComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@SuppressWarnings("unused")
public class CreateWorldScreen extends Screen {
    private int bg = -1;

    private FieldComponent fWorldName;
    private FieldComponent fSeed;

    private ButtonComponent btnCreate;
    private ButtonComponent btnCancel;

    private boolean initialized = false;

    private long lastBlink = System.currentTimeMillis();
    private boolean cursorVisible = true;

    private boolean worldNameSelected = false;
    private boolean seedSelected = false;

    private static final long BACKSPACE_DELAY = 400L;
    private static final long BACKSPACE_REPEAT = 50L;

    private long backspaceStart = 0L;
    private long lastBackspace = 0L;

    private int heldKey = 0;
    private char heldChar = 0;
    private long heldStart = 0L;
    private long lastHeld = 0L;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void render(FontRenderer font, int width, int height) {
        if (bg == -1) {
            bg = Textures.loadTexture("/client/textures/background.png", GL_NEAREST);
        }

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);

        glColor4f(1f, 1f, 1f, 1f);
        Textures.bind(bg);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(0, 0);
        glTexCoord2f(1, 0); glVertex2f(width, 0);
        glTexCoord2f(1, 1); glVertex2f(width, height);
        glTexCoord2f(0, 1); glVertex2f(0, height);
        glEnd();

        String title = "Create New World";
        int titleX = (width - font.getStringWidth(title) * 2) / 2;
        int titleY = height / 7;

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(titleX, titleY, 0);
        glScalef(2f, 2f, 1f);
        glEnable(GL_TEXTURE_2D);
        font.drawString(title, 0, 0, Color.WHITE, true);
        glPopMatrix();

        int fieldW = 320;
        int fieldH = 28;
        int x = (width - fieldW) / 2;

        int labelH = font.getStringHeight();
        int worldLabelY = titleY + 56;
        int worldFieldY = worldLabelY + labelH + 4;

        int seedLabelY = worldFieldY + fieldH + 34;
        int seedFieldY = seedLabelY + labelH + 4;

        if (!initialized) {
            fWorldName = new FieldComponent("World Name", x, worldFieldY, fieldW, fieldH);
            fWorldName.value = new StringBuilder("New World");

            fSeed = new FieldComponent("Seed for the World Generator", x, seedFieldY, fieldW, fieldH);
            fSeed.value = new StringBuilder();

            initialized = true;
        } else {
            fWorldName.x = x;
            fWorldName.y = worldFieldY;
            fWorldName.w = fieldW;
            fWorldName.h = fieldH;

            fSeed.x = x;
            fSeed.y = seedFieldY;
            fSeed.w = fieldW;
            fSeed.h = fieldH;
        }

        long now = System.currentTimeMillis();
        if (now - lastBlink > 530L) {
            cursorVisible = !cursorVisible;
            lastBlink = now;
        }

        handleBackspace();
        handleKeyRepeat();

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                fWorldName.focused = fWorldName.contains(mx, my);
                fSeed.focused = fSeed.contains(mx, my);

                if (fWorldName.focused) {
                    fSeed.focused = false;
                } else if (fSeed.focused) {
                    fWorldName.focused = false;
                }

                worldNameSelected = false;
                seedSelected = false;
                heldKey = 0;
                heldChar = 0;
                heldStart = 0L;

                if (btnCreate != null && btnCreate.contains(mx, my)) {
                    onCreate();
                } else if (btnCancel != null && btnCancel.contains(mx, my)) {
                    onCancel();
                }
            }
        }

        FieldComponent focused = getFocusedField();

        while (Keyboard.next()) {
            if (!Keyboard.getEventKeyState() || focused == null) continue;

            int key = Keyboard.getEventKey();
            char ch = Keyboard.getEventCharacter();
            boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

            if (ctrl) {
                heldKey = 0;
                heldChar = 0;
                heldStart = 0L;

                if (key == Keyboard.KEY_A) {
                    if (focused == fWorldName) worldNameSelected = true;
                    if (focused == fSeed) seedSelected = true;
                } else if (key == Keyboard.KEY_C) {
                    try {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                .setContents(new java.awt.datatransfer.StringSelection(focused.value.toString()), null);
                    } catch (Exception ignored) {}
                } else if (key == Keyboard.KEY_V) {
                    try {
                        String clip = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                .getData(java.awt.datatransfer.DataFlavor.stringFlavor);

                        if (focused == fWorldName) {
                            if (worldNameSelected) {
                                focused.value.setLength(0);
                                worldNameSelected = false;
                            }
                            focused.value.append(clip);
                        } else if (focused == fSeed) {
                            if (seedSelected) {
                                focused.value.setLength(0);
                                seedSelected = false;
                            }
                            focused.value.append(clip);
                        }
                    } catch (Exception ignored) {}
                }
            } else if (key == Keyboard.KEY_BACK) {
                heldKey = 0;
                heldChar = 0;
                heldStart = 0L;

                if (focused == fWorldName && worldNameSelected) {
                    focused.value.setLength(0);
                    worldNameSelected = false;
                } else if (focused == fSeed && seedSelected) {
                    focused.value.setLength(0);
                    seedSelected = false;
                } else if (focused.value.length() > 0) {
                    focused.value.deleteCharAt(focused.value.length() - 1);
                }

                backspaceStart = System.currentTimeMillis();
                lastBackspace = backspaceStart;
            } else if (key == Keyboard.KEY_TAB) {
                heldKey = 0;
                heldChar = 0;
                heldStart = 0L;

                fWorldName.focused = false;
                fSeed.focused = false;
                worldNameSelected = false;
                seedSelected = false;

                if (focused == fWorldName) {
                    fSeed.focused = true;
                } else {
                    fWorldName.focused = true;
                }
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                heldKey = 0;
                heldChar = 0;
                heldStart = 0L;
                onCreate();
            } else if (ch >= 32 && ch != 127) {
                if (focused == fWorldName && worldNameSelected) {
                    focused.value.setLength(0);
                    worldNameSelected = false;
                }
                if (focused == fSeed && seedSelected) {
                    focused.value.setLength(0);
                    seedSelected = false;
                }

                focused.value.append(ch);
                heldKey = key;
                heldChar = ch;
                heldStart = System.currentTimeMillis();
                lastHeld = heldStart;
            }
        }

        drawLabel(font, "World Name", x, worldLabelY);
        drawLabel(font, "Seed for the World Generator", x, seedLabelY);

        drawField(font, fWorldName, mx, my, cursorVisible, worldNameSelected);

        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
        String saveText = "Will be saved in: " + fWorldName.value.toString();
        font.drawString(saveText, x, fWorldName.y + fieldH + 8, new Color(170, 170, 170), true);

        drawField(font, fSeed, mx, my, cursorVisible, seedSelected);

        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
        font.drawString("Leave blank for a random seed", x, fSeed.y + fieldH + 8, new Color(170, 170, 170), true);

        int btnW = fieldW;
        int btnH = 28;
        int btnY1 = fSeed.y + 70;
        int btnY2 = btnY1 + btnH + 8;

        btnCreate = new ButtonComponent("Create New World", x, btnY1, btnW, btnH);
        btnCancel = new ButtonComponent("Cancel", x, btnY2, btnW, btnH);

        drawButton(font, btnCreate, btnCreate.contains(mx, my));
        drawButton(font, btnCancel, btnCancel.contains(mx, my));

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void drawLabel(FontRenderer font, String text, int x, int y) {
        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
        font.drawString(text, x, y, Color.LIGHT_GRAY, true);
    }

    private FieldComponent getFocusedField() {
        if (fWorldName != null && fWorldName.focused) return fWorldName;
        if (fSeed != null && fSeed.focused) return fSeed;
        return null;
    }

    private void handleKeyRepeat() {
        FieldComponent focused = getFocusedField();
        if (focused == null || heldKey == 0 || !Keyboard.isKeyDown(heldKey)) {
            heldKey = 0;
            heldChar = 0;
            heldStart = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (heldStart == 0L || now - heldStart < BACKSPACE_DELAY) return;

        if (now - lastHeld >= BACKSPACE_REPEAT) {
            focused.value.append(heldChar);
            lastHeld = now;
        }
    }

    private void handleBackspace() {
        FieldComponent focused = getFocusedField();
        if (focused == null || !Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
            backspaceStart = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (backspaceStart == 0L || now - backspaceStart < BACKSPACE_DELAY) return;

        if (now - lastBackspace >= BACKSPACE_REPEAT) {
            if (focused.value.length() > 0) {
                focused.value.deleteCharAt(focused.value.length() - 1);
            }
            lastBackspace = now;
        }
    }

    private void drawField(FontRenderer font, FieldComponent f, int mx, int my, boolean cursor, boolean selected) {
        boolean hov = f.contains(mx, my);

        glDisable(GL_TEXTURE_2D);
        glColor4f(0f, 0f, 0f, 1f);
        glBegin(GL_QUADS);
        glVertex2f(f.x, f.y);
        glVertex2f(f.x + f.w, f.y);
        glVertex2f(f.x + f.w, f.y + f.h);
        glVertex2f(f.x, f.y + f.h);
        glEnd();

        if (f.focused) {
            glColor4f(1f, 1f, 1f, 1f);
        } else if (hov) {
            glColor4f(0.7f, 0.7f, 0.7f, 1f);
        } else {
            glColor4f(0.45f, 0.45f, 0.45f, 1f);
        }

        glBegin(GL_LINE_LOOP);
        glVertex2f(f.x, f.y);
        glVertex2f(f.x + f.w, f.y);
        glVertex2f(f.x + f.w, f.y + f.h);
        glVertex2f(f.x, f.y + f.h);
        glEnd();

        if (f.focused && selected && f.value.length() > 0) {
            int lh = font.getStringHeight();
            int selX = f.x + 6;
            int selW = font.getStringWidth(f.value.toString());
            int selY = f.y + (f.h - lh) / 2;

            glColor4f(0.2f, 0.6f, 1f, 0.5f);
            glBegin(GL_QUADS);
            glVertex2f(selX, selY);
            glVertex2f(selX + selW, selY);
            glVertex2f(selX + selW, selY + lh);
            glVertex2f(selX, selY + lh);
            glEnd();
        }

        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);

        int lh = font.getStringHeight();
        String text = f.value.toString();
        if (f.focused && cursor && !selected) {
            text += "|";
        }
        font.drawString(text, f.x + 6, f.y + (f.h - lh) / 2, Color.WHITE, true);
    }

    private void drawButton(FontRenderer font, ButtonComponent button, boolean hovered) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(hovered ? 0.55f : 0.20f, hovered ? 0.55f : 0.20f, hovered ? 0.55f : 0.20f, 0.85f);
        glBegin(GL_QUADS);
        glVertex2f(button.x, button.y);
        glVertex2f(button.x + button.w, button.y);
        glVertex2f(button.x + button.w, button.y + button.h);
        glVertex2f(button.x, button.y + button.h);
        glEnd();

        glColor4f(0.85f, 0.85f, 0.85f, 0.9f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(button.x, button.y);
        glVertex2f(button.x + button.w, button.y);
        glVertex2f(button.x + button.w, button.y + button.h);
        glVertex2f(button.x, button.y + button.h);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
        font.drawString(
                button.label,
                button.x + (button.w - font.getStringWidth(button.label)) / 2,
                button.y + (button.h - font.getStringHeight()) / 2,
                hovered ? Color.YELLOW : Color.WHITE,
                true
        );
    }

    private void onCreate() {
        String worldName = fWorldName.value.toString().trim();
        String seed = fSeed.value.toString().trim();

        if (worldName.isEmpty()) {
            worldName = "New World";
        }

        System.out.println("Singleplayer is still in developement, Please wait for the new version");

        // TODO: World gen
    }

    private void onCancel() {
        Minecraft.mc.setScreen(new MenuScreen());
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}