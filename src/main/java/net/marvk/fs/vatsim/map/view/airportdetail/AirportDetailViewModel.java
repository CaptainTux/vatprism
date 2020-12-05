package net.marvk.fs.vatsim.map.view.airportdetail;

import net.marvk.fs.vatsim.map.data.Airport;
import net.marvk.fs.vatsim.map.view.datadetail.DataDetailSubViewModel;

public class AirportDetailViewModel extends DataDetailSubViewModel<Airport> {
    public void setToFir() {
        if (getData() != null) {
            setDataDetail(getData().getFlightInformationRegionBoundary());
        }
    }
}
