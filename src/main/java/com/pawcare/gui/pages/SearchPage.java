package com.pawcare.gui.pages;

import com.pawcare.gui.Page;
import com.pawcare.gui.components.ActionColumn;
import com.pawcare.gui.components.BadgeRenderer;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.Column;
import com.pawcare.gui.components.DateRenderer;
import com.pawcare.gui.components.ListCard;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTable;
import com.pawcare.gui.components.SearchField;
import com.pawcare.gui.components.SectionHeader;
import com.pawcare.gui.dialogs.PetDetailsDialog;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.service.ClinicService;
import com.pawcare.service.SearchService;
import com.pawcare.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * One box that searches the whole clinic.
 *
 * <p>The scope control decides which field is matched; on <i>All fields</i> the query is
 * tested against the pet's name, id, breed, species, owner id and owner name at once.
 * Matching is case-insensitive and uses "contains", so typing {@code bru} finds
 * <i>Bruno</i>.</p>
 *
 * <p>The same query is also run against the client book, so one search covers both
 * patients and their owners.</p>
 */
public class SearchPage extends Page {

    /** View index of the column holding the row actions. It is the last of nine columns. */
    private static final int ACTION_COLUMN = 8;
    private static final int TABLE_HEIGHT = 400;
    private static final int OWNERS_HEIGHT = 250;
    /** The client card lists at most this many matches; the rest are on the Owners screen. */
    private static final int MAX_OWNER_ROWS = 8;

    private final SearchField search = new SearchField("Type a pet name, an id, a breed or an owner name", 28);
    private final ModernComboBox<SearchService.Scope> scope =
            ModernComboBox.of(SearchService.Scope.class);
    private final JLabel summary = new JLabel();
    private final ModernTable<Pet> table;
    private final ListCard ownerResults = new ListCard("Matching clients",
            "Owners whose name, id, phone or email contains the query",
            "Nothing to show",
            "Type in the search box above to look through the client book.");

    public SearchPage(ClinicService clinic) {
        super(clinic, "Search",
                "Find any patient by name, id, species, breed or owner, and any client by name.");

        ModernButton clear = new ModernButton("Clear", ModernButton.Variant.SECONDARY, "refresh");
        clear.addActionListener(e -> {
            search.clear();
            applySearch();
        });
        header().addAction(clear);

        table = new ModernTable<>(buildColumns());
        table.setEmptyIcon("search");
        table.setEmptyMessage("No patients match this query");
        ActionColumn.install(table, ACTION_COLUMN, this::onRowAction, "View profile");
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.columnAtPoint(e.getPoint()) != ACTION_COLUMN) {
                    Pet pet = table.getObjectAt(table.rowAtPoint(e.getPoint()));
                    if (pet != null) {
                        new PetDetailsDialog(window(), clinic, pet).setVisible(true);
                    }
                }
            }
        });

        search.onChange(this::applySearch);
        scope.addActionListener(e -> applySearch());

        summary.setFont(Theme.FONT_SMALL);
        summary.setForeground(Theme.c().textSecondary);

        stack(buildSearchCard(), Theme.SPACE_XL);
        stack(new SectionHeader("Patients",
                "Every patient that matches the current query and scope"), Theme.SPACE_MD);
        stack(fixed(buildTableCard(), TABLE_HEIGHT), Theme.SPACE_XL);
        stack(new SectionHeader("Clients",
                "Owners matched on name, id, phone or email"), Theme.SPACE_MD);
        stack(fixed(ownerResults, OWNERS_HEIGHT), 0);
    }

    // ------------------------------------------------------------------
    // Static layout
    // ------------------------------------------------------------------

    private JComponent buildSearchCard() {
        Card card = new Card("Universal search",
                "Matching is case-insensitive, and a partial word is enough.");

        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        row.setOpaque(false);
        row.add(search, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        right.setOpaque(false);

        JLabel scopeLabel = new JLabel("Search in");
        scopeLabel.setFont(Theme.FONT_SMALL_MEDIUM);
        scopeLabel.setForeground(Theme.c().textSecondary);
        right.add(scopeLabel);
        right.add(scope);
        row.add(right, BorderLayout.EAST);

        JPanel holder = new JPanel();
        holder.setOpaque(false);
        holder.setLayout(new javax.swing.BoxLayout(holder, javax.swing.BoxLayout.Y_AXIS));

        // The row is pinned to its preferred height, otherwise the vertical box layout
        // would stretch the search field down the whole card.
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, row.getPreferredSize().height));
        holder.add(row);
        holder.add(javax.swing.Box.createVerticalStrut(Theme.SPACE_MD));

        summary.setAlignmentX(LEFT_ALIGNMENT);
        holder.add(summary);

        // A BorderLayout puts the stack at its natural height instead of the card centre.
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(holder, BorderLayout.NORTH);

        card.content().add(north, BorderLayout.CENTER);
        return card;
    }

    private List<Column<Pet>> buildColumns() {
        List<Column<Pet>> columns = new ArrayList<>();
        columns.add(new Column<>("ID", Pet::getAnimalId, 70));
        columns.add(new Column<>("Name", Pet::getName, 150));
        columns.add(new Column<>("Species", Pet::getSpecies, 90));
        columns.add(new Column<>("Breed", Pet::getBreed, 170));
        columns.add(new Column<>("Age", Pet::getAge, 60));
        columns.add(new Column<>("Owner",
                pet -> clinic.owners().ownerName(pet.getOwnerId()), 150));
        columns.add(new Column<>("Vaccination",
                pet -> clinic.vaccinations().statusForPet(pet.getAnimalId()), 130,
                new BadgeRenderer()));
        columns.add(new Column<>("Registered", Pet::getRegistrationDate, 115,
                new DateRenderer()));
        columns.add(new Column<>("Profile", pet -> "", 120));
        return columns;
    }

    private Card buildTableCard() {
        Card card = new Card("Results", "Double-click a row, or use the row action, to open the profile");
        card.content().add(scroll(table, 340), BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshHeader();
        applySearch();
    }

    /** Runs the query against the patients and the client book in one pass. */
    private void applySearch() {
        String query = search.getText();
        SearchService.Scope selected = (SearchService.Scope) scope.getSelectedItem();

        List<Pet> pets = clinic.search().search(query, selected);
        table.setRows(pets);

        boolean blank = query == null || query.isBlank();
        summary.setText(blank
                ? "Showing all " + clinic.pets().count() + " patients. Type to narrow the list."
                : pets.size() + (pets.size() == 1 ? " patient matches " : " patients match ")
                        + "\"" + query.trim() + "\" in " + selected.getLabel().toLowerCase()
                        + ".");

        ownerResults.clearRows();
        int shown = 0;
        for (Owner owner : clinic.search().searchOwners(query)) {
            if (shown >= MAX_OWNER_ROWS) {
                break;
            }
            ownerResults.addRow("owners", Theme.c().primary,
                    owner.getName(),
                    owner.getPhone() + "  ·  " + owner.getEmail(),
                    clinic.pets().findByOwner(owner.getOwnerId()).size() + " pets",
                    null);
            shown++;
        }
        ownerResults.emptyState().setText(blank ? "Nothing to show" : "No clients match",
                blank ? "Type in the search box above to look through the client book."
                        : "No owner name, id, phone number or email contains \"" + query.trim() + "\".");
        ownerResults.refresh();
    }

    private void onRowAction(String action, int modelRow) {
        Pet pet = table.getEntityModel().getRow(modelRow);
        if (pet != null && "View profile".equals(action)) {
            new PetDetailsDialog(window(), clinic, pet).setVisible(true);
        }
    }
}
