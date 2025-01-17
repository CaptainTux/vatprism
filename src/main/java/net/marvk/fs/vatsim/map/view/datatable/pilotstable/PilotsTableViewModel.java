package net.marvk.fs.vatsim.map.view.datatable.pilotstable;

import com.google.inject.Inject;
import javafx.collections.ObservableList;
import net.marvk.fs.vatsim.map.data.Pilot;
import net.marvk.fs.vatsim.map.data.Preferences;
import net.marvk.fs.vatsim.map.view.datatable.SimpleTableViewModel;

public class PilotsTableViewModel extends SimpleTableViewModel<Pilot> {
    @Inject
    public PilotsTableViewModel(final Preferences preferences) {
        super(preferences);
    }

    @Override
    public ObservableList<Pilot> items() {
        return toolbarScope.filteredPilots();
    }
}
