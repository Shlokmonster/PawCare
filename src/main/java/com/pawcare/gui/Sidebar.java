package com.pawcare.gui;

import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The navigation rail down the left-hand side.
 *
 * <p>It holds the product mark at the top, the main sections in the middle and the two
 * utility entries at the bottom. The active entry is highlighted with a tinted background
 * and a short accent bar, and is the only one drawn in the accent colour, so the current
 * screen is obvious at a glance.</p>
 */
public class Sidebar extends JPanel {

    /** Keys the main window understands. "about" opens a dialog rather than a page. */
    public static final String DASHBOARD = "dashboard";
    public static final String PETS = "pets";
    public static final String OWNERS = "owners";
    public static final String APPOINTMENTS = "appointments";
    public static final String TREATMENTS = "treatments";
    public static final String VACCINATIONS = "vaccinations";
    public static final String SEARCH = "search";
    public static final String REPORTS = "reports";
    public static final String SETTINGS = "settings";
    public static final String ABOUT = "about";

    /** key, label and icon of each main entry, in display order. */
    private static final String[][] MAIN_ITEMS = {
            {DASHBOARD, "Dashboard", "dashboard"},
            {PETS, "Pets", "paw"},
            {OWNERS, "Owners", "owners"},
            {APPOINTMENTS, "Appointments", "appointments"},
            {TREATMENTS, "Treatments", "treatments"},
            {VACCINATIONS, "Vaccinations", "vaccinations"},
            {SEARCH, "Search", "search"},
            {REPORTS, "Reports", "reports"},
    };

    private static final String[][] UTILITY_ITEMS = {
            {SETTINGS, "Settings", "settings"},
            {ABOUT, "About", "about"},
    };

    private final Consumer<String> onNavigate;
    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private String activeKey = DASHBOARD;

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;

        setOpaque(true);
        setBackground(Theme.c().sidebar);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_MD, Theme.SPACE_LG, Theme.SPACE_MD));

        add(buildBrand(), BorderLayout.NORTH);
        add(buildNavigation(), BorderLayout.CENTER);
        add(buildUtility(), BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------

    private JPanel buildBrand() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_SM, Theme.SPACE_XL, Theme.SPACE_SM));

        JPanel mark = new JPanel(new BorderLayout(Theme.SPACE_SM, 0));
        mark.setOpaque(false);

        JLabel paw = new JLabel(IconFactory.of("paw", 26, Theme.c().primary));
        mark.add(paw, BorderLayout.WEST);

        JLabel name = new JLabel("PAWCARE");
        name.setFont(Theme.FONT_LOGO);
        name.setForeground(Theme.c().textPrimary);
        mark.add(name, BorderLayout.CENTER);

        mark.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(mark);

        brand.add(Box.createVerticalStrut(Theme.SPACE_XS));

        JLabel tagline = new JLabel("Pet Health & Clinic");
        tagline.setFont(Theme.FONT_TINY);
        tagline.setForeground(Theme.c().textMuted);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 34, 0, 0));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(tagline);

        return brand;
    }

    private JPanel buildNavigation() {
        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));

        JLabel sectionLabel = new JLabel("CLINIC");
        sectionLabel.setFont(Theme.FONT_TINY);
        sectionLabel.setForeground(Theme.c().textMuted);
        sectionLabel.setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_SM, Theme.SPACE_SM, 0));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        navigation.add(sectionLabel);

        for (String[] item : MAIN_ITEMS) {
            navigation.add(createItem(item[0], item[1], item[2]));
            navigation.add(Box.createVerticalStrut(2));
        }

        return navigation;
    }

    private JPanel buildUtility() {
        JPanel utility = new JPanel();
        utility.setOpaque(false);
        utility.setLayout(new BoxLayout(utility, BoxLayout.Y_AXIS));

        JPanel rule = new JPanel();
        rule.setBackground(Theme.c().divider);
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rule.setPreferredSize(new Dimension(0, 1));
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);
        utility.add(rule);
        utility.add(Box.createVerticalStrut(Theme.SPACE_SM));

        for (String[] item : UTILITY_ITEMS) {
            utility.add(createItem(item[0], item[1], item[2]));
            utility.add(Box.createVerticalStrut(2));
        }
        return utility;
    }

    private NavItem createItem(String key, String label, String iconName) {
        NavItem item = new NavItem(key, label, iconName);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.addActionListener(e -> {
            setActive(key);
            onNavigate.accept(key);
        });
        items.put(key, item);
        return item;
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /** Highlights one entry, clearing the previous one. */
    public void setActive(String key) {
        activeKey = key;
        for (Map.Entry<String, NavItem> entry : items.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(key));
        }
    }

    public String getActiveKey() {
        return activeKey;
    }

    /**
     * One entry in the rail.
     *
     * <p>It is a {@link JButton} so keyboard focus and accessibility come for free, but its
     * chrome is switched off and the background is painted here instead.</p>
     */
    private static final class NavItem extends JButton {

        private final String key;
        private final String iconName;
        private boolean active;

        NavItem(String key, String label, String iconName) {
            super(label);
            this.key = key;
            this.iconName = iconName;

            setFont(Theme.FONT_BODY_MEDIUM);
            setHorizontalAlignment(LEFT);
            setIconTextGap(Theme.SPACE_MD);
            setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_MD, 0, Theme.SPACE_MD));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(0, 40));
            applyIcon();
        }

        /** @return the navigation key this entry stands for. */
        String key() {
            return key;
        }

        void setActive(boolean active) {
            this.active = active;
            applyIcon();
            repaint();
        }

        /** Recreates the glyph in the colour the current state calls for. */
        private void applyIcon() {
            setIcon(IconFactory.of(iconName, 18, active
                    ? Theme.c().primary : Theme.c().textOnSidebar));
            setForeground(active ? Theme.c().primary : Theme.c().textOnSidebar);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            if (active) {
                g2.setColor(Theme.c().sidebarActive);
                g2.fillRoundRect(0, 0, width, height, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);

                // A short accent bar marks the current screen.
                g2.setColor(Theme.c().primary);
                g2.fillRoundRect(0, height / 2 - 9, 3, 18, 3, 3);
            } else if (getModel().isRollover()) {
                g2.setColor(Theme.c().sidebarHover);
                g2.fillRoundRect(0, 0, width, height, Theme.RADIUS_CONTROL, Theme.RADIUS_CONTROL);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Adds a hover-reveal scrollbar-free feel: the rail itself never scrolls. */
    @Override
    public void addNotify() {
        super.addNotify();
        // A mouse listener on the rail clears a stuck rollover when the pointer leaves it.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                for (NavItem item : items.values()) {
                    item.getModel().setRollover(false);
                }
            }
        });
    }
}
