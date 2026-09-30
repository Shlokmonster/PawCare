package com.pawcare.gui.pages;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.Page;
import com.pawcare.gui.components.ActionColumn;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.Column;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernTable;
import com.pawcare.gui.components.SearchField;
import com.pawcare.gui.components.StatCard;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.gui.dialogs.OwnerDetailsDialog;
import com.pawcare.gui.dialogs.OwnerDialog;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The client book: the people who own the animals the clinic treats.
 *
 * <p>An owner can only be deleted once no pet is registered to them. The rule is enforced
 * by the service, and the failure it raises is shown here with the list of pets that are
 * still attached, so the user knows exactly what to do next.</p>
 */
public class OwnersPage extends Page {

    /** View index of the column holding the row actions. */
    private static final int ACTION_COLUMN = 6;
    /** Height of the table card. The page scrolls rather than squashing the table. */
    private static final int TABLE_HEIGHT = 410;

    private final SearchField search = new SearchField("Search by name, id, phone or email");
    private final JLabel summary = new JLabel();
    private final ModernTable<Owner> table;

    private final StatCard totalCard =
            new StatCard("Registered owners", "0", "in the client book", "owners", Theme.c().primary);
    private final StatCard withPetsCard =
            new StatCard("Owners with pets", "0", "have at least one patient", "paw", Theme.c().accent);
    private final StatCard averageCard =
            new StatCard("Average pets per owner", "0.0", "across the whole register",
                    "reports", Theme.c().info);

    public OwnersPage(ClinicService clinic) {
        super(clinic, "Owners",
                "The client book. Every pet on the register belongs to one of these owners.");

        ModernButton add = new ModernButton("Add owner",
                ModernButton.Variant.PRIMARY, "plus");
        add.addActionListener(e -> openForm(null));
        header().addAction(add);

        table = new ModernTable<>(buildColumns());
        table.setEmptyIcon("owners");
        table.setEmptyMessage("No owners match the current search");
        ActionColumn.install(table, ACTION_COLUMN, this::onRowAction, "View", "Edit", "Delete");
        installDoubleClick();

        search.onChange(this::applyFilters);

        summary.setFont(Theme.FONT_TINY);
        summary.setForeground(Theme.c().textSecondary);

        stack(row(totalCard, withPetsCard, averageCard), Theme.SPACE_XL);
        stack(buildToolbar(), Theme.SPACE_MD);
        stack(fixed(buildTableCard(), TABLE_HEIGHT), 0);
    }

    // ------------------------------------------------------------------
    // Static layout
    // ------------------------------------------------------------------

    private List<Column<Owner>> buildColumns() {
        List<Column<Owner>> columns = new ArrayList<>();
        columns.add(new Column<>("ID", Owner::getOwnerId, 70));
        columns.add(new Column<>("Name", Owner::getName, 170));
        columns.add(new Column<>("Mobile", Owner::getPhone, 130));
        columns.add(new Column<>("Email", Owner::getEmail, 210));
        columns.add(new Column<>("Address", Owner::getAddress, 240));
        columns.add(new Column<>("Pets", this::petCount, 70));
        columns.add(new Column<>("Actions", owner -> "", 180));
        return columns;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        bar.setOpaque(false);
        bar.add(search, BorderLayout.CENTER);

        ModernButton clear = new ModernButton("Clear", ModernButton.Variant.SECONDARY, "refresh");
        clear.addActionListener(e -> {
            search.clear();
            applyFilters();
        });

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        right.setOpaque(false);
        right.add(clear);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private Card buildTableCard() {
        Card card = new Card("Client book",
                "Click a heading to sort. Use the row actions to view, edit or delete.",
                summary);
        card.content().add(scroll(table, 400), BorderLayout.CENTER);
        return card;
    }

    private void installDoubleClick() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || table.columnAtPoint(e.getPoint()) == ACTION_COLUMN) {
                    return;
                }
                Owner owner = table.getObjectAt(table.rowAtPoint(e.getPoint()));
                if (owner != null) {
                    openDetails(owner);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshHeader();
        updateSummaryCards();
        applyFilters();
    }

    /** Every figure here is counted from the live collections, never stored. */
    private void updateSummaryCards() {
        int owners = clinic.owners().count();
        int pets = clinic.pets().count();

        int withPets = 0;
        for (Owner owner : clinic.owners().getAllOwners()) {
            if (!clinic.pets().findByOwner(owner.getOwnerId()).isEmpty()) {
                withPets++;
            }
        }

        totalCard.setValue(String.valueOf(owners));
        totalCard.setCaption(owners == 1 ? "client on file" : "clients on file");

        withPetsCard.setValue(String.valueOf(withPets));
        withPetsCard.setCaption(owners - withPets + " without a pet");

        averageCard.setValue(owners == 0 ? "0.0"
                : String.format(java.util.Locale.ENGLISH, "%.1f", pets / (double) owners));
        averageCard.setCaption(pets + (pets == 1 ? " patient in total" : " patients in total"));
    }

    private void applyFilters() {
        List<Owner> rows = clinic.search().searchOwners(search.getText());
        int total = clinic.owners().count();
        summary.setText(rows.size() + " of " + total + (total == 1 ? " owner" : " owners"));
        table.setRows(rows);
    }

    private int petCount(Owner owner) {
        return clinic.pets().findByOwner(owner.getOwnerId()).size();
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void onRowAction(String action, int modelRow) {
        Owner owner = table.getEntityModel().getRow(modelRow);
        if (owner == null) {
            return;
        }
        switch (action) {
            case "View":
                openDetails(owner);
                break;
            case "Edit":
                openForm(owner);
                break;
            case "Delete":
                delete(owner);
                break;
            default:
                break;
        }
    }

    private void openDetails(Owner owner) {
        new OwnerDetailsDialog(window(), clinic, owner).setVisible(true);
    }

    private void openForm(Owner existing) {
        OwnerDialog dialog = new OwnerDialog(window(), clinic, existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
            toast(existing == null ? "Owner added to the client book" : "Owner details updated");
        }
    }

    private void delete(Owner owner) {
        List<Pet> pets = clinic.pets().findByOwner(owner.getOwnerId());
        if (!pets.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (Pet pet : pets) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(pet.getName()).append(" (").append(pet.getAnimalId()).append(')');
            }
            Dialogs.warning(this, "Owner still has pets",
                    owner.getName() + " still has " + pets.size() + " registered pet(s): "
                            + names + ".\nReassign or delete those patients first.");
            return;
        }

        if (!Dialogs.confirmDelete(this, owner.getName() + " (" + owner.getOwnerId() + ")")) {
            return;
        }
        try {
            clinic.deleteOwner(owner.getOwnerId());
            refresh();
            toast(owner.getName() + " was removed from the client book");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to delete owner", e);
        }
    }
}
