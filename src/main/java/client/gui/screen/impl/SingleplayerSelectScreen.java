package client.gui.screen.impl;

import static org.lwjgl.opengl.GL11.*;

import client.FontRenderer;
import client.Minecraft;
import client.Textures;
import client.gui.screen.Screen;
import client.gui.screen.components.ButtonComponent;
import java.awt.Color;
import org.lwjgl.input.Mouse;

public class SingleplayerSelectScreen extends Screen {
    private int bg = -1;

    private ButtonComponent btnPlay, btnCreate, btnRename, btnDelete, btnCancel;

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

        String title = "Select World (in dev)";
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

        int panelX = 0;
        int panelY = 60;
        int footerH = 82;
        int panelW = width;
        int panelH = height - panelY - footerH;

        drawWorldList(panelX, panelY, panelW, panelH);

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
        btnDelete = new ButtonComponent("Delete", row2X + smallW + spacing, footerY2, smallW, btnH);
        btnCancel = new ButtonComponent("Cancel", row2X + (smallW + spacing) * 2, footerY2, bigW, btnH);

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                if (btnPlay.contains(mx, my)) {
                    onPlaySelectedWorld();
                } else if (btnCreate.contains(mx, my)) {
                    onCreateNewWorld();
                } else if (btnRename.contains(mx, my)) {
                    onRename();
                } else if (btnDelete.contains(mx, my)) {
                    onDelete();
                } else if (btnCancel.contains(mx, my)) {
                    onCancel();
                }
            }
        }

        drawButton(font, btnPlay, btnPlay.contains(mx, my));
        drawButton(font, btnCreate, btnCreate.contains(mx, my));
        drawButton(font, btnRename, btnRename.contains(mx, my));
        drawButton(font, btnDelete, btnDelete.contains(mx, my));
        drawButton(font, btnCancel, btnCancel.contains(mx, my));

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void drawButton(FontRenderer font, ButtonComponent button, boolean hovered) {
        glDisable(GL_TEXTURE_2D);

        glColor4f(
                hovered ? 0.55f : 0.20f,
                hovered ? 0.55f : 0.20f,
                hovered ? 0.55f : 0.20f,
                0.85f
        );

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
        font.drawString(
                button.label,
                button.x + (button.w - font.getStringWidth(button.label)) / 2,
                button.y + (button.h - font.getStringHeight()) / 2,
                hovered ? Color.YELLOW : Color.WHITE,
                true
        );
    }

    private void drawWorldList(int x, int y, int w, int h) {
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

        glEnable(GL_TEXTURE_2D);
    }

    // TODO: make these actually function
    private void onPlaySelectedWorld() {}

    private void onCreateNewWorld() {
        Minecraft.mc.setScreen(new CreateWorldScreen());
    }

    private void onRename() {}

    private void onDelete() {}

    private void onCancel() {
        Minecraft.mc.setScreen(new MenuScreen());
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
