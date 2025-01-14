package net.marvk.fs.vatsim.map.data;

import lombok.Value;
import net.marvk.fs.vatsim.api.data.VatsimPilotRating;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class PilotRating implements Comparable<PilotRating> {
    private static final Map<Integer, PilotRating> RATINGS = new LinkedHashMap<>();

    int id;
    String shortName;
    String longName;

    public static PilotRating of(final int id, final String shortName, final String longName) {
        return RATINGS.computeIfAbsent(id, key -> new PilotRating(id, shortName, longName));
    }

    public static PilotRating of(final VatsimPilotRating rating) {
        return of(Integer.parseInt(rating.getId()), rating.getShortName(), rating.getLongName());
    }

    public static PilotRating[] values() {
        return RATINGS.values().toArray(PilotRating[]::new);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return id == ((PilotRating) o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(final PilotRating o) {
        return Integer.compare(id, o.id);
    }
}
