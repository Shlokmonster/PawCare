package com.pawcare.theme;

import java.awt.Color;

/**
 * One complete set of colours for the application.
 *
 * <p>A palette is immutable in spirit: {@link Theme} builds the light and the dark
 * version once, and every component reads its colours from whichever palette is
 * currently active. That is what makes the theme switch a one-line change instead of
 * a hunt through dozens of classes.</p>
 *
 * <p>Fields are deliberately public so a component can read
 * {@code Theme.c().primary} directly without a screenful of getters.</p>
 */
public final class Palette {

    // Surfaces -------------------------------------------------------
    public Color background;      // window background behind the cards
    public Color sidebar;         // navigation rail
    public Color sidebarHover;    // navigation item hover
    public Color sidebarActive;   // navigation item selected
    public Color surface;         // cards, panels, tables
    public Color surfaceAlt;      // table alternate row / subtle stripes
    public Color surfaceHover;    // row hover
    public Color field;           // text field and combo box background
    public Color border;          // hairlines around cards and inputs
    public Color divider;         // separators inside a card

    // Text -----------------------------------------------------------
    public Color textPrimary;
    public Color textSecondary;
    public Color textMuted;
    public Color textOnPrimary;   // text drawn on top of the primary colour
    public Color textOnSidebar;

    // Brand ----------------------------------------------------------
    public Color primary;         // deep indigo – buttons, links, active nav
    public Color primaryHover;
    public Color primarySoft;     // tinted background for subtle primary chips
    public Color accent;          // teal – highlights and charts
    public Color accentSoft;

    // Semantic -------------------------------------------------------
    public Color success;
    public Color successSoft;
    public Color warning;
    public Color warningSoft;
    public Color danger;
    public Color dangerSoft;
    public Color info;
    public Color infoSoft;

    // Misc -----------------------------------------------------------
    public Color selection;       // table selection background
    public Color selectionText;
    public Color shadow;          // translucent drop shadow
    public Color scrollThumb;
    public Color chartGrid;
    public Color[] chart;         // categorical series colours for the charts

    private Palette() {
    }

    /** The default light theme. */
    public static Palette light() {
        Palette p = new Palette();

        p.background = new Color(0xF4F6FA);
        p.sidebar = Color.WHITE;
        p.sidebarHover = new Color(0xF1F4FB);
        p.sidebarActive = new Color(0xE8EDFC);
        p.surface = Color.WHITE;
        p.surfaceAlt = new Color(0xF8FAFD);
        p.surfaceHover = new Color(0xF1F5FE);
        p.field = Color.WHITE;
        p.border = new Color(0xE3E8F0);
        p.divider = new Color(0xEDF0F6);

        p.textPrimary = new Color(0x141A24);
        p.textSecondary = new Color(0x5A6478);
        p.textMuted = new Color(0x8A93A6);
        p.textOnPrimary = Color.WHITE;
        p.textOnSidebar = new Color(0x3D4658);

        p.primary = new Color(0x2C46C7);
        p.primaryHover = new Color(0x2237A6);
        p.primarySoft = new Color(0xE8EDFC);
        p.accent = new Color(0x0E9E82);
        p.accentSoft = new Color(0xE1F6F1);

        p.success = new Color(0x17803D);
        p.successSoft = new Color(0xE6F6EC);
        p.warning = new Color(0xB45309);
        p.warningSoft = new Color(0xFDF1E0);
        p.danger = new Color(0xC0261F);
        p.dangerSoft = new Color(0xFCEAE8);
        p.info = new Color(0x1D4ED8);
        p.infoSoft = new Color(0xE7EEFE);

        p.selection = new Color(0xDCE6FB);
        p.selectionText = new Color(0x141A24);
        p.shadow = new Color(0, 0, 0, 18);
        p.scrollThumb = new Color(0xC7CEDC);
        p.chartGrid = new Color(0xE9EDF4);
        p.chart = new Color[]{
                new Color(0x2C46C7),
                new Color(0x0E9E82),
                new Color(0xE08A00),
                new Color(0xC2417F),
                new Color(0x6D5BD0)
        };
        return p;
    }

    /** The dark theme – same structure, inverted surfaces and softened accents. */
    public static Palette dark() {
        Palette p = new Palette();

        p.background = new Color(0x0E1116);
        p.sidebar = new Color(0x12161E);
        p.sidebarHover = new Color(0x1B2231);
        p.sidebarActive = new Color(0x212B44);
        p.surface = new Color(0x171C26);
        p.surfaceAlt = new Color(0x1B212C);
        p.surfaceHover = new Color(0x212938);
        p.field = new Color(0x1B212C);
        p.border = new Color(0x272E3C);
        p.divider = new Color(0x232A36);

        p.textPrimary = new Color(0xE9EDF5);
        p.textSecondary = new Color(0x9AA4B6);
        p.textMuted = new Color(0x77808F);
        p.textOnPrimary = Color.WHITE;
        p.textOnSidebar = new Color(0xAEB7C6);

        p.primary = new Color(0x5B78F6);
        p.primaryHover = new Color(0x7089F8);
        p.primarySoft = new Color(0x1E2740);
        p.accent = new Color(0x1FC3A2);
        p.accentSoft = new Color(0x122A26);

        p.success = new Color(0x4ADE80);
        p.successSoft = new Color(0x122A1C);
        p.warning = new Color(0xFBBF24);
        p.warningSoft = new Color(0x2C2410);
        p.danger = new Color(0xF87171);
        p.dangerSoft = new Color(0x2C1517);
        p.info = new Color(0x60A5FA);
        p.infoSoft = new Color(0x141F33);

        p.selection = new Color(0x25335A);
        p.selectionText = new Color(0xEDF1F7);
        p.shadow = new Color(0, 0, 0, 90);
        p.scrollThumb = new Color(0x394354);
        p.chartGrid = new Color(0x232A36);
        p.chart = new Color[]{
                new Color(0x7B93FF),
                new Color(0x2DD4B4),
                new Color(0xF5B942),
                new Color(0xE879B4),
                new Color(0x9E8CF0)
        };
        return p;
    }
}
