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
    private ButtonComponent btnBack;
    private final int panelW = 520;
    private final int panelH = 430;

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

        String title = "Singleplayer";
        int titleX = (width - font.getStringWidth(title) * 2) / 2;
        int titleY = height / 10;

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(titleX, titleY, 0);
        glScalef(2f, 2f, 1f);
        glEnable(GL_TEXTURE_2D);
        font.drawString(title, 0, 0, Color.WHITE, true);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);

        int panelX = (width - panelW) / 2;
        int panelY = height / 8 + 28;

        bgpanel(panelX, panelY, panelW, panelH);

        String msg = "Coming Soon..";
        int msgX = panelX + (panelW - font.getStringWidth(msg) * 2) / 2;
        int msgY = panelY + (panelH / 2) - 10;

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(msgX, msgY, 0);
        glScalef(2f, 2f, 1f);
        glEnable(GL_TEXTURE_2D);
        font.drawString(msg, 0, 0, Color.WHITE, true);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);

        int btnW = 180;
        int btnH = 28;
        int btnX = panelX + (panelW - btnW) / 2;
        int btnY = panelY + panelH + 16;

        btnBack = new ButtonComponent("Back", btnX, btnY, btnW, btnH);

        int mx = Mouse.getX();
        int my = height - Mouse.getY() - 1;

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                if (btnBack.contains(mx, my)) {
                    onBack();
                }
            }
        }

        drawButton(font, btnBack, btnBack.contains(mx, my));

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
        glColor4f(hovered ? 0.55f : 0.20f,hovered ? 0.55f : 0.20f,hovered ? 0.55f : 0.20f,0.85f);

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
        font.drawString(button.label,button.x + (button.w - font.getStringWidth(button.label)) / 2,button.y + (button.h - font.getStringHeight()) / 2,hovered ? Color.YELLOW : Color.WHITE,true);
    }

    private void bgpanel(int x, int y, int w, int h) {
        glDisable(GL_TEXTURE_2D);

        glColor4f(0.00f, 0.00f, 0.00f, 0.50f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();

        glColor4f(1.00f, 1.00f, 1.00f, 0.9f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();

        glEnable(GL_TEXTURE_2D);
    }

    private void onBack() {
        Minecraft.mc.setScreen(new MenuScreen());
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}