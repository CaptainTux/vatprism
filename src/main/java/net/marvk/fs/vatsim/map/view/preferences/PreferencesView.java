package net.marvk.fs.vatsim.map.view.preferences;

import com.dlsc.formsfx.model.validators.DoubleRangeValidator;
import com.dlsc.formsfx.model.validators.IntegerRangeValidator;
import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import lombok.SneakyThrows;
import net.marvk.fs.vatsim.map.App;
import net.marvk.fs.vatsim.map.data.Preferences;
import net.marvk.fs.vatsim.map.view.Notifications;
import net.marvk.fs.vatsim.map.view.SettingsScope;
import net.marvk.fs.vatsim.map.view.painter.MetaPainter;
import net.marvk.fs.vatsim.map.view.painter.Painter;
import net.marvk.fs.vatsim.map.view.painter.PainterExecutor;
import net.marvk.fs.vatsim.map.view.painter.Parameter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.octicons.Octicons;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class PreferencesView {
    private final Preferences preferences;
    private final SettingsScope settingsScope;
    private PreferencesFx preferencesFx;

    @Inject
    public PreferencesView(final Preferences preferences, final SettingsScope settingsScope) {
        this.preferences = preferences;
        this.settingsScope = settingsScope;
        this.settingsScope.getPainters()
                          .addListener((ListChangeListener<PainterExecutor<?>>) c -> getPreferencesDialog());
    }

    public void show() {
        getPreferencesDialog().show(true);
    }

    private PreferencesFx getPreferencesDialog() {
        if (preferencesFx == null) {
            preferencesFx = createPreferencesDialog();
        }

        return preferencesFx;
    }

    @SneakyThrows
    private PreferencesFx createPreferencesDialog() {
        return PreferencesFx.of(App.class, general(), style(), painters())
                            .saveSettings(false);
    }

    private Category style() {
        return Category.of("Style");
    }

    private Category general() {
        final BooleanProperty debug = preferences.booleanProperty("general.debug");
        final IntegerProperty uiFontSize = preferences.integerProperty("general.font_size");
        final IntegerProperty property = preferences.integerProperty("general.map_font_size");
        final DoubleProperty scrollSpeed = preferences.doubleProperty("general.scroll_speed");

        debug.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                preferences.booleanProperty("metrics.enabled").set(false);
            }
        });
        debug.set(true);

        final IntegerProperty uiScale = preferences.integerProperty("general.ui_scale");
        uiScale.bind(uiFontSize.divide(12.0));

        return Category.of(
                "General",
                FontIcon.of(Octicons.GEAR_16),
                Setting.of("Enable Debug Mode", debug),
                Setting.of("UI Font Size", uiFontSize, 4, 72),
                Setting.of("Map Font Size", property, 4, 72),
                Setting.of("Scroll Speed", scrollSpeed, 1.1, 16, 2)
        );
    }

    private Category painters() throws IllegalAccessException {
        final Category[] painters = paintersCategories(settingsScope.getPainters());

        return Category
                .of("Painters", FontIcon.of(Octicons.PAINTBRUSH_16))
                .subCategories(painters)
                .expand();
    }

    private Category[] paintersCategories(final ObservableList<PainterExecutor<?>> executors) throws IllegalAccessException {
        final List<Category> categories = new ArrayList<>();

        for (final PainterExecutor<?> executor : executors) {
            final Group[] groups = getSettings(executor.getPainter(), executor.getName());
            for (final Group group : groups) {
                for (final Setting<?, ?> setting : group.getSettings()) {
                    final Property<?> o = setting.valueProperty();
//                    observables.put(o.getName(), o);
                }
            }
            final Category category = Category.of(executor.getName(), groups);
            categories.add(category);
        }
        return categories.toArray(Category[]::new);
    }

    private Group[] getSettings(final Painter<?> painter, final String prefix) throws IllegalAccessException {

        final List<Field> fields = Arrays
                .stream(fields(painter))
                .filter(e -> e.isAnnotationPresent(net.marvk.fs.vatsim.map.view.painter.Group.class) || e.isAnnotationPresent(Parameter.class) || e
                        .isAnnotationPresent(MetaPainter.class))
                .peek(e -> e.setAccessible(true))
                .collect(Collectors.toList());

        final ArrayList<Group> result = new ArrayList<>();

        String currentGroup = prefix;
        final Collection<Setting<?, ?>> settings = new ArrayList<>();

        for (final Field field : fields) {
            if (field.isAnnotationPresent(net.marvk.fs.vatsim.map.view.painter.Group.class)) {
                if (!settings.isEmpty()) {
                    settings.removeIf(Objects::isNull);

                    if (!settings.isEmpty()) {
                        result.add(Group.of(currentGroup, settings.toArray(Setting[]::new)));
                    }
                    settings.clear();
                }

                final var groupAnnotation = field.getAnnotation(net.marvk.fs.vatsim.map.view.painter.Group.class);
                currentGroup = groupAnnotation.value();
            }

            if (field.isAnnotationPresent(Parameter.class)) {
                settings.add(extracted(painter, field, prefix));
            }
        }

        if (!settings.isEmpty()) {
            settings.removeIf(Objects::isNull);

            if (!settings.isEmpty()) {
                result.add(Group.of(currentGroup, settings.toArray(Setting[]::new)));
            }
        }

        for (final Field field : fields) {
            if (field.isAnnotationPresent(MetaPainter.class)) {
                final MetaPainter metaPainter = field.getAnnotation(MetaPainter.class);

                final Painter<?> thePainter = (Painter<?>) field.get(painter);
                result.addAll(Arrays.asList(getSettings(thePainter, prefix + "." + metaPainter.value())));
            }
        }

        return result.toArray(Group[]::new);
    }

    private static Field[] fields(final Painter<?> painter) {
        Class<?> clazz = painter.getClass();

        final ArrayList<Field> result = new ArrayList<>();

        while (clazz != Object.class) {
            final Field[] declaredFields = clazz.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                result.add(i, declaredFields[i]);
            }
            clazz = clazz.getSuperclass();
        }

        return result.toArray(Field[]::new);
    }

    private Setting<?, ?> extracted(final Painter<?> painter, final Field field, final String prefix) throws IllegalAccessException {
        final Parameter parameter = field.getAnnotation(Parameter.class);

        final String name = parameter.value();

        final double min = parameter.min();
        final double max = parameter.max();

        final String bindToKey = parameter.bind();
        final boolean bind = !bindToKey.isBlank();
        final boolean visible = parameter.visible();
        final String key = key(prefix, name);
        if (Color.class.isAssignableFrom(field.getType())) {
            final ObjectProperty<Color> property = preferences.colorProperty(key, (Color) field.get(painter));
            property.addListener((observable, oldValue, newValue) -> setField(field, painter, newValue));
            setField(field, painter, property.getValue());
            if (bind) {
                property.bind(preferences.colorProperty(bindToKey));
            }
            if (visible) {
                return Setting.of(name, property).customKey(key);
            }
        } else if (int.class.isAssignableFrom(field.getType())) {
            final IntegerProperty property = preferences.integerProperty(key, (int) field.get(painter));
            property.addListener((observable, oldValue, newValue) -> setField(field, painter, newValue));
            setField(field, painter, property.getValue());
            if (bind) {
                property.bind(preferences.integerProperty(bindToKey));
            }
            if (visible) {
                return Setting.of(name, property)
                              .customKey(key)
                              .validate(IntegerRangeValidator.between((int) min, (int) max, "Not in range"));
            }
        } else if (double.class.isAssignableFrom(field.getType())) {
            final DoubleProperty property = preferences.doubleProperty(key, (double) field.get(painter));
            property.addListener((observable, oldValue, newValue) -> setField(field, painter, newValue));
            setField(field, painter, property.getValue());
            if (bind) {
                property.bind(preferences.doubleProperty(bindToKey));
            }
            if (visible) {
                return Setting.of(name, property)
                              .customKey(key)
                              .validate(DoubleRangeValidator.between(min, max, "Not in range"));
            }
        } else if (boolean.class.isAssignableFrom(field.getType())) {
            final BooleanProperty property = preferences.booleanProperty(key, (boolean) field.get(painter));
            property.addListener((observable, oldValue, newValue) -> setField(field, painter, newValue));
            setField(field, painter, property.getValue());
            if (bind) {
                property.bind(preferences.booleanProperty(bindToKey));
            }
            if (visible) {
                return Setting.of(name, property)
                              .customKey(key);
            }
        }
        return null;
    }

    @SneakyThrows
    private static void setField(final Field field, final Painter<?> painter, final Object newValue) {
        field.set(painter, newValue);
        Notifications.REPAINT.publish();
    }

    private static String key(final String... keys) {
        return Arrays
                .stream(keys)
                .map(e -> e.toLowerCase(Locale.ROOT))
                .map(e -> e.replaceAll("\\s", "_"))
                .map(e -> e.replaceAll("[^A-Za-z0-9._]", ""))
                .collect(Collectors.joining("."));
    }
}