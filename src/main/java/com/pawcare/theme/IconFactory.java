package com.pawcare.theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Draws the application's icons with Java2D instead of loading image files.
 *
 * <p>Every icon is authored on a 24x24 grid and scaled to the requested size, so the
 * same code produces a crisp nav icon and a large empty-state illustration. Drawing
 * them avoids shipping binary assets and keeps them perfectly sharp on retina
 * displays.</p>
 */
public final class IconFactory {

    private IconFactory() {
    }

    /** Returns an icon drawn in the given colour. */
    public static Icon of(String name, int size, Color color) {
        return new VectorIcon(name, size, color);
    }

    /** Convenience overload using the current theme's secondary text colour. */
    public static Icon muted(String name, int size) {
        return new VectorIcon(name, size, Theme.c().textSecondary);
    }

    /** Renders the paw mark into an image, used as the window icon. */
    public static BufferedImage pawImage(int size, Color background, Color foreground) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(background);
        g.fillRoundRect(0, 0, size, size, (int) (size * 0.28), (int) (size * 0.28));
        g.translate(size / 2.0 - size / 2.0, 0);
        drawPaw(g, size / 24.0, foreground, true);
        g.dispose();
        return image;
    }

    // ------------------------------------------------------------------

    private static final class VectorIcon implements Icon {

        private final String name;
        private final int size;
        private final Color color;

        VectorIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(name, g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Draws one icon on the 24x24 grid. */
    private static void draw(String name, Graphics2D g) {
        switch (name) {
            case "paw":
                drawPaw(g, 1.0, g.getColor(), true);
                break;

            case "dashboard":
                g.draw(new RoundRectangle2D.Double(3, 3, 7.5, 7.5, 2.5, 2.5));
                g.draw(new RoundRectangle2D.Double(13.5, 3, 7.5, 7.5, 2.5, 2.5));
                g.draw(new RoundRectangle2D.Double(3, 13.5, 7.5, 7.5, 2.5, 2.5));
                g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7.5, 7.5, 2.5, 2.5));
                break;

            case "owners":
                g.draw(new Ellipse2D.Double(8.4, 4.6, 7.2, 7.2));
                g.draw(new Arc2D.Double(4.4, 12.6, 15.2, 16, 0, 180, Arc2D.OPEN));
                break;

            case "appointments":
                g.draw(new RoundRectangle2D.Double(3.2, 5, 17.6, 15.6, 3, 3));
                g.draw(new java.awt.geom.Line2D.Double(3.2, 9.6, 20.8, 9.6));
                g.draw(new java.awt.geom.Line2D.Double(8, 3.2, 8, 6.6));
                g.draw(new java.awt.geom.Line2D.Double(16, 3.2, 16, 6.6));
                g.fill(new Ellipse2D.Double(7, 12.4, 2.2, 2.2));
                g.fill(new Ellipse2D.Double(14.8, 12.4, 2.2, 2.2));
                g.draw(new java.awt.geom.Line2D.Double(7.4, 17.2, 16.6, 17.2));
                break;

            case "treatments":
                g.draw(new RoundRectangle2D.Double(3.2, 3.2, 17.6, 17.6, 5, 5));
                g.draw(new java.awt.geom.Line2D.Double(12, 7.6, 12, 16.4));
                g.draw(new java.awt.geom.Line2D.Double(7.6, 12, 16.4, 12));
                break;

            case "vaccinations":
                g.rotate(Math.toRadians(-35), 12, 12);
                g.draw(new RoundRectangle2D.Double(9.4, 7.4, 5.2, 8.6, 1.6, 1.6));
                g.draw(new java.awt.geom.Line2D.Double(12, 7.4, 12, 5.4));
                g.draw(new java.awt.geom.Line2D.Double(10.2, 5.4, 13.8, 5.4));
                g.draw(new java.awt.geom.Line2D.Double(12, 16, 12, 19.6));
                g.draw(new java.awt.geom.Line2D.Double(10, 10.6, 14, 10.6));
                break;

            case "search":
                g.draw(new Ellipse2D.Double(4.4, 4.4, 11.4, 11.4));
                g.draw(new java.awt.geom.Line2D.Double(15.4, 15.4, 20, 20));
                break;

            case "reports":
                g.draw(new RoundRectangle2D.Double(4, 13.2, 4, 7.4, 1.4, 1.4));
                g.draw(new RoundRectangle2D.Double(10, 8.6, 4, 12, 1.4, 1.4));
                g.draw(new RoundRectangle2D.Double(16, 4.4, 4, 16.2, 1.4, 1.4));
                break;

            case "settings":
                g.draw(new Ellipse2D.Double(5.6, 5.6, 12.8, 12.8));
                g.draw(new Ellipse2D.Double(9.6, 9.6, 4.8, 4.8));
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    double x1 = 12 + Math.cos(angle) * 6.4;
                    double y1 = 12 + Math.sin(angle) * 6.4;
                    double x2 = 12 + Math.cos(angle) * 9.4;
                    double y2 = 12 + Math.sin(angle) * 9.4;
                    g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
                }
                break;

            case "about":
                g.draw(new Ellipse2D.Double(3.4, 3.4, 17.2, 17.2));
                g.fill(new Ellipse2D.Double(11, 7, 2, 2));
                g.draw(new java.awt.geom.Line2D.Double(12, 11, 12, 17));
                break;

            case "bell":
                g.draw(new Arc2D.Double(5.6, 4.6, 12.8, 13, 0, 180, Arc2D.OPEN));
                g.draw(new java.awt.geom.Line2D.Double(5.6, 11.1, 4.4, 16.6));
                g.draw(new java.awt.geom.Line2D.Double(18.4, 11.1, 19.6, 16.6));
                g.draw(new java.awt.geom.Line2D.Double(4.4, 16.6, 19.6, 16.6));
                g.draw(new Arc2D.Double(9.8, 16.4, 4.4, 3.6, 180, 180, Arc2D.OPEN));
                break;

            case "clock":
                g.draw(new Ellipse2D.Double(3.4, 3.4, 17.2, 17.2));
                g.draw(new java.awt.geom.Line2D.Double(12, 12, 12, 7.2));
                g.draw(new java.awt.geom.Line2D.Double(12, 12, 15.6, 13.6));
                break;

            case "plus":
                g.draw(new java.awt.geom.Line2D.Double(12, 5.6, 12, 18.4));
                g.draw(new java.awt.geom.Line2D.Double(5.6, 12, 18.4, 12));
                break;

            case "edit":
                Path2D pencil = new Path2D.Double();
                pencil.moveTo(4.6, 19.4);
                pencil.lineTo(5.7, 15.2);
                pencil.lineTo(15.9, 5.0);
                pencil.lineTo(19.0, 8.1);
                pencil.lineTo(8.8, 18.3);
                pencil.closePath();
                g.draw(pencil);
                g.draw(new java.awt.geom.Line2D.Double(14.2, 6.7, 17.3, 9.8));
                break;

            case "trash":
                g.draw(new java.awt.geom.Line2D.Double(4, 6.8, 20, 6.8));
                g.draw(new RoundRectangle2D.Double(9.4, 3.6, 5.2, 3.2, 1.4, 1.4));
                g.draw(new java.awt.geom.Line2D.Double(6.6, 6.8, 7.8, 20.2));
                g.draw(new java.awt.geom.Line2D.Double(17.4, 6.8, 16.2, 20.2));
                g.draw(new java.awt.geom.Line2D.Double(7.8, 20.2, 16.2, 20.2));
                g.draw(new java.awt.geom.Line2D.Double(10.4, 10, 10.9, 17));
                g.draw(new java.awt.geom.Line2D.Double(13.6, 10, 13.1, 17));
                break;

            case "eye":
                Path2D eye = new Path2D.Double();
                eye.moveTo(2.6, 12);
                eye.curveTo(6, 6.4, 18, 6.4, 21.4, 12);
                eye.curveTo(18, 17.6, 6, 17.6, 2.6, 12);
                eye.closePath();
                g.draw(eye);
                g.fill(new Ellipse2D.Double(9.6, 9.6, 4.8, 4.8));
                break;

            case "check":
                g.draw(new java.awt.geom.Line2D.Double(5, 12.6, 10, 17.6));
                g.draw(new java.awt.geom.Line2D.Double(10, 17.6, 19, 7));
                break;

            case "close":
                g.draw(new java.awt.geom.Line2D.Double(6.4, 6.4, 17.6, 17.6));
                g.draw(new java.awt.geom.Line2D.Double(17.6, 6.4, 6.4, 17.6));
                break;

            case "chevron":
                g.draw(new java.awt.geom.Line2D.Double(9.6, 6.4, 15.2, 12));
                g.draw(new java.awt.geom.Line2D.Double(15.2, 12, 9.6, 17.6));
                break;

            case "filter":
                g.draw(new java.awt.geom.Line2D.Double(3.6, 6.4, 20.4, 6.4));
                g.draw(new java.awt.geom.Line2D.Double(6.4, 12, 17.6, 12));
                g.draw(new java.awt.geom.Line2D.Double(9.6, 17.6, 14.4, 17.6));
                break;

            case "sun":
                g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    double x1 = 12 + Math.cos(angle) * 6.2;
                    double y1 = 12 + Math.sin(angle) * 6.2;
                    double x2 = 12 + Math.cos(angle) * 8.6;
                    double y2 = 12 + Math.sin(angle) * 8.6;
                    g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
                }
                break;

            case "moon":
                Path2D moon = new Path2D.Double();
                moon.moveTo(20, 14.8);
                moon.curveTo(13.4, 16.6, 8.2, 11.4, 10, 4.6);
                moon.curveTo(4.2, 6.6, 3.4, 14, 7.6, 18);
                moon.curveTo(11.4, 21.4, 17, 20, 20, 14.8);
                moon.closePath();
                g.draw(moon);
                break;

            case "refresh":
                g.draw(new Arc2D.Double(4, 4, 16, 16, 40, 280, Arc2D.OPEN));
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(17.4, 3.4);
                arrow.lineTo(20.4, 8.0);
                arrow.lineTo(15.2, 8.6);
                arrow.closePath();
                g.fill(arrow);
                break;

            case "info":
                g.draw(new RoundRectangle2D.Double(3.2, 3.2, 17.6, 17.6, 5, 5));
                g.fill(new Ellipse2D.Double(11, 7.2, 2, 2));
                g.draw(new java.awt.geom.Line2D.Double(12, 11.4, 12, 16.8));
                break;

            case "folder":
                Path2D folder = new Path2D.Double();
                folder.moveTo(3, 19.4);
                folder.lineTo(3, 5.4);
                folder.lineTo(9.6, 5.4);
                folder.lineTo(11.6, 8.2);
                folder.lineTo(21, 8.2);
                folder.lineTo(21, 19.4);
                folder.closePath();
                g.draw(folder);
                break;

            default:
                // Unknown icon name: draw a neutral placeholder rather than throwing at paint time.
                g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 4, 4));
                break;
        }
    }

    /** The paw mark, drawn to a scale so it can serve as both icon and logo. */
    private static void drawPaw(Graphics2D g, double scale, Color color, boolean filled) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.scale(scale, scale);
        g2.setColor(color);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D pad = new Ellipse2D.Double(7.0, 11.6, 10.0, 8.4);
        Ellipse2D[] toes = {
                new Ellipse2D.Double(3.4, 6.6, 4.4, 4.4),
                new Ellipse2D.Double(7.9, 3.9, 4.6, 4.6),
                new Ellipse2D.Double(12.9, 4.1, 4.5, 4.5),
                new Ellipse2D.Double(16.9, 7.2, 4.2, 4.2)
        };

        if (filled) {
            g2.fill(pad);
            for (Ellipse2D toe : toes) {
                g2.fill(toe);
            }
        } else {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(pad);
            for (Ellipse2D toe : toes) {
                g2.draw(toe);
            }
        }
        g2.dispose();
    }
}
