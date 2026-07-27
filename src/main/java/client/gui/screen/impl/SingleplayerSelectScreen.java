package client.gui.screen.impl;

import static org.lwjgl.opengl.GL11.*;

import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import client.gui.screen.Screen;
import client.gui.screen.components.ButtonComponent;
import client.gui.screen.components.FieldComponent;
import client.singleplayer.Singleplayer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class SingleplayerSelectScreen extends Screen {
    private int bg = -1;

    private ButtonComponent btnPlay, btnCreate, btnRename, btnDelete, btnCancel;
    private ButtonComponent btnConfirmRename, btnCancelRename;

    private List<String> worlds = new ArrayList<>();
    private String selectedWorld = null;
    private int scrollRow = 0;

    private static final int ROW_H = 22;

    private boolean renaming = false;
    private FieldComponent fRename;

    private boolean deleteArmed = false;
    private long deleteArmedAt = 0L;
    private static final long DELETE_ARM_TIMEOUT_MS = 4000L;

    private boolean worldsLoaded = false;

    @Override
    public void init() {
        super.init();
    }

    private void refreshWorlds() {
        worlds = Singleplayer.listWorlds();
        if (selectedWorld != null && !worlds.contains(selectedWorld)) {
            selectedWorld = null;
        }
        deleteArmed = false;
    }

    @Override
    public void render(FontRenderer font, int width, int height) {
        if (bg == -1) {
            bg = Textures.loadTexture("/client/textures/background.png", GL_NEAREST);
        }

        if (!worldsLoaded) {
            refreshWorlds();
            worldsLoaded = true;
        }

        if (deleteArmed && System.currentTimeMillis() - deleteArmedAt > DELETE_ARM_TIMEOUT_MS) {
            deleteArmed = false;
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

        String title = renaming ? "Rename World" : "Select World";
        int titleX = (width - font.getStringWidth(title) * 2) / 2;
        int titleY = 12;

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(titleX, titleY, 0);
        glScalef(2f, 2f, 1f);
        glEnable(GL_TEXTURE_2D);
        font.drawString(title, 0, 0, Color.WHITE, true);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);

        if (renaming) {
            renderRenameMode(font, width, height);
        } else {
            renderListMode(font, width, height);
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void renderListMode(FontRenderer font, int width, int height) {
        int panelX = 0;
        int panelY = 60;
        int footerH = 82;
        int panelW = width;
        int panelH = height - panelY - footerH;

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        int visibleRows = Math.max(1, panelH / ROW_H);
        int maxScroll = Math.max(0, worlds.size() - visibleRows);
        if (scrollRow > maxScroll) scrollRow = maxScroll;
        if (scrollRow < 0) scrollRow = 0;

        int spacing = 18;
        int btnH = 28;
        int footerY1 = panelY + panelH + 26;
        int footerY2 = footerY1 + btnH + 10;

        int bigW = 300;
        int smallW = 140;

        int row1Width = (bigW * 2) + spacing;
        int row1X = (width - row1Width) / 2;

        btnPlay = new ButtonComponent("Play Selected World", row1X, footerY1, bigW, btnH);
        btnCreate = new ButtonComponent("Create New World", row1X + bigW + spacing, footerY1, bigW, btnH);

        int row2Width = (smallW * 2) + bigW + (spacing * 2);
        int row2X = (width - row2Width) / 2;

        btnRename = new ButtonComponent("Rename", row2X, footerY2, smallW, btnH);
        btnDelete = new ButtonComponent(deleteArmed ? "Confirm Delete?" : "Delete", row2X + smallW + spacing, footerY2, smallW, btnH);
        btnCancel = new ButtonComponent("Cancel", row2X + (smallW + spacing) * 2, footerY2, bigW, btnH);

        boolean hasSelection = selectedWorld != null;

        int dwheel = Mouse.getDWheel();
        if (dwheel != 0 && mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
            scrollRow -= Integer.signum(dwheel);
            if (scrollRow < 0) scrollRow = 0;
            if (scrollRow > maxScroll) scrollRow = maxScroll;
        }

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                boolean rowClicked = false;
                for (int i = 0; i < visibleRows; i++) {
                    int idx = scrollRow + i;
                    if (idx >= worlds.size()) break;
                    int rowY = panelY + 2 + i * ROW_H;
                    if (mx >= panelX && mx <= panelX + panelW && my >= rowY && my <= rowY + ROW_H) {
                        selectedWorld = worlds.get(idx);
                        deleteArmed = false;
                        rowClicked = true;
                        break;
                    }
                }

                if (!rowClicked) {
                    if (hasSelection && btnPlay.contains(mx, my)) {
                        onPlaySelectedWorld();
                    } else if (btnCreate.contains(mx, my)) {
                        onCreateNewWorld();
                    } else if (hasSelection && btnRename.contains(mx, my)) {
                        onRename();
                    } else if (hasSelection && btnDelete.contains(mx, my)) {
                        onDelete();
                    } else if (btnCancel.contains(mx, my)) {
                        onCancel();
                    }
                }
            }
        }

        drawWorldList(font, panelX, panelY, panelW, panelH, visibleRows, mx, my);

        drawButton(font, btnPlay, hasSelection && btnPlay.contains(mx, my), hasSelection);
        drawButton(font, btnCreate, btnCreate.contains(mx, my), true);
        drawButton(font, btnRename, hasSelection && btnRename.contains(mx, my), hasSelection);
        drawButton(font, btnDelete, hasSelection && btnDelete.contains(mx, my), hasSelection);
        drawButton(font, btnCancel, btnCancel.contains(mx, my), true);
    }

    private void renderRenameMode(FontRenderer font, int width, int height) {
        int fieldW = 320;
        int fieldH = 28;
        int x = (width - fieldW) / 2;
        int fieldY = height / 2 - fieldH;

        if (fRename == null) {
            fRename = new FieldComponent("New name", x, fieldY, fieldW, fieldH);
            fRename.value = new StringBuilder(selectedWorld == null ? "" : selectedWorld);
            fRename.focused = true;
        } else {
            fRename.x = x;
            fRename.y = fieldY;
            fRename.w = fieldW;
            fRename.h = fieldH;
        }

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        while (Keyboard.next()) {
            if (!Keyboard.getEventKeyState()) continue;
            int key = Keyboard.getEventKey();
            char ch = Keyboard.getEventCharacter();

            if (key == Keyboard.KEY_BACK) {
                if (fRename.value.length() > 0) fRename.value.deleteCharAt(fRename.value.length() - 1);
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                confirmRename();
            } else if (key == Keyboard.KEY_ESCAPE) {
                cancelRename();
            } else if (ch >= 32 && ch != 127) {
                fRename.value.append(ch);
            }
        }

        drawField(font, fRename, mx, my);

        int btnW = fieldW;
        int btnH = 28;
        int btnY1 = fieldY + fieldH + 24;
        int btnY2 = btnY1 + btnH + 8;

        btnConfirmRename = new ButtonComponent("Confirm Rename", x, btnY1, btnW, btnH);
        btnCancelRename = new ButtonComponent("Cancel", x, btnY2, btnW, btnH);

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                if (btnConfirmRename.contains(mx, my)) {
                    confirmRename();
                } else if (btnCancelRename.contains(mx, my)) {
                    cancelRename();
                }
            }
        }

        drawButton(font, btnConfirmRename, btnConfirmRename.contains(mx, my), true);
        drawButton(font, btnCancelRename, btnCancelRename.contains(mx, my), true);
    }

    private void confirmRename() {
        String newName = fRename.value.toString().trim();
        if (!newName.isEmpty() && Singleplayer.renameWorld(selectedWorld, newName)) {
            selectedWorld = Singleplayer.sanitize(newName);
        }
        fRename = null;
        renaming = false;
        refreshWorlds();
    }

    private void cancelRename() {
        fRename = null;
        renaming = false;
    }

    private void drawField(FontRenderer font, FieldComponent f, int mx, int my) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0f, 0f, 0f, 1f);
        glBegin(GL_QUADS);
        glVertex2f(f.x, f.y);
        glVertex2f(f.x + f.w, f.y);
        glVertex2f(f.x + f.w, f.y + f.h);
        glVertex2f(f.x, f.y + f.h);
        glEnd();

        glColor4f(1f, 1f, 1f, 1f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(f.x, f.y);
        glVertex2f(f.x + f.w, f.y);
        glVertex2f(f.x + f.w, f.y + f.h);
        glVertex2f(f.x, f.y + f.h);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
        int lh = font.getStringHeight();
        font.drawString(f.value.toString() + "|", f.x + 6, f.y + (f.h - lh) / 2, Color.WHITE, true);
    }

    private void drawButton(FontRenderer font, ButtonComponent button, boolean hovered, boolean enabled) {
        glDisable(GL_TEXTURE_2D);

        float shade = enabled ? (hovered ? 0.55f : 0.20f) : 0.10f;
        glColor4f(shade, shade, shade, enabled ? 0.85f : 0.5f);

        glBegin(GL_QUADS);
        glVertex2f(button.x, button.y);
        glVertex2f(button.x + button.w, button.y);
        glVertex2f(button.x + button.w, button.y + button.h);
        glVertex2f(button.x, button.y + button.h);
        glEnd();

        glColor4f(0.85f, 0.85f, 0.85f, enabled ? 0.9f : 0.4f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(button.x, button.y);
        glVertex2f(button.x + button.w, button.y);
        glVertex2f(button.x + button.w, button.y + button.h);
        glVertex2f(button.x, button.y + button.h);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        Color textColor = !enabled ? Color.GRAY : (hovered ? Color.YELLOW : Color.WHITE);
        font.drawString(
                button.label,
                button.x + (button.w - font.getStringWidth(button.label)) / 2,
                button.y + (button.h - font.getStringHeight()) / 2,
                textColor,
                true
        );
    }

    private void drawWorldList(FontRenderer font, int x, int y, int w, int h, int visibleRows, int mx, int my) {
        glDisable(GL_TEXTURE_2D);

        glColor4f(0f, 0f, 0f, 0.58f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();

        glColor4f(0.18f, 0.18f, 0.18f, 1f);
        glBegin(GL_LINES);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x, y + h);
        glVertex2f(x + w, y + h);
        glEnd();

        if (worlds.isEmpty()) {
            glEnable(GL_TEXTURE_2D);
            String msg = "No worlds are here :(";
            font.drawString(msg, x + (w - font.getStringWidth(msg)) / 2, y + 10, Color.LIGHT_GRAY, true);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int idx = scrollRow + i;
            if (idx >= worlds.size()) break;

            String name = worlds.get(idx);
            int rowY = y + 2 + i * ROW_H;
            boolean selected = name.equals(selectedWorld);
            boolean hovered = mx >= x && mx <= x + w && my >= rowY && my <= rowY + ROW_H;

            if (selected || hovered) {
                glColor4f(selected ? 0.25f : 0.15f, selected ? 0.5f : 0.15f, selected ? 0.25f : 0.15f, 0.7f);
                glBegin(GL_QUADS);
                glVertex2f(x + 2, rowY);
                glVertex2f(x + w - 2, rowY);
                glVertex2f(x + w - 2, rowY + ROW_H);
                glVertex2f(x + 2, rowY + ROW_H);
                glEnd();
            }

            glEnable(GL_TEXTURE_2D);
            int lh = font.getStringHeight();
            font.drawString(name, x + 10, rowY + (ROW_H - lh) / 2, selected ? Color.YELLOW : Color.WHITE, true);
            glDisable(GL_TEXTURE_2D);
        }

        glEnable(GL_TEXTURE_2D);
    }

    private void onPlaySelectedWorld() {
        if (selectedWorld == null) return;
        Singleplayer.start(selectedWorld);
    }

    private void onCreateNewWorld() {
        Minecraft.mc.setScreen(new CreateWorldScreen());
    }

    private void onRename() {
        if (selectedWorld == null) return;
        renaming = true;
        fRename = null;
    }

    private void onDelete() {
        if (selectedWorld == null) return;
        if (!deleteArmed) {
            deleteArmed = true;
            deleteArmedAt = System.currentTimeMillis();
            return;
        }
        Singleplayer.deleteWorld(selectedWorld);
        selectedWorld = null;
        deleteArmed = false;
        refreshWorlds();
    }

    private void onCancel() {
        Minecraft.mc.setScreen(new MenuScreen());
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
