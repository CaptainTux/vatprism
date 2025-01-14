package net.marvk.fs.vatsim.map.view.painter;

import com.sun.javafx.geom.Line2D;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import net.marvk.fs.vatsim.map.GeomUtil;
import net.marvk.fs.vatsim.map.data.Polygon;
import net.marvk.fs.vatsim.map.view.map.MapVariables;
import net.marvk.fs.vatsim.map.view.map.PainterMetric;

import java.util.List;

public class PainterHelper {
    private static final double MIN_DISTANCE = 2;
    private static final double MIN_SQUARE_DISTANCE = squareThreshold(MIN_DISTANCE);

    private PainterMetric metric = new PainterMetric();

    private static double squareThreshold(final double value) {
        return value <= 0 ? Integer.MIN_VALUE : value * value;
    }

    private final MapVariables mapVariables;

    public PainterHelper(final MapVariables mapVariables) {
        this.mapVariables = mapVariables;
    }

    public PainterMetric metricSnapshot() {
        final PainterMetric result = this.metric;
        this.metric = new PainterMetric();
        return result;
    }

    public void strokePolygons(final GraphicsContext c, final Polygon polygon) {
        drawPolygons(c, polygon, false, false, true);
    }

    public void strokePolylines(final GraphicsContext c, final Polygon polygon) {
        drawPolygons(c, polygon, true, false, true);
    }

    public void fillPolygons(final GraphicsContext c, final Polygon polygon) {
        drawPolygons(c, polygon, false, true, true);
    }

    public void strokePolyline(final GraphicsContext c, final Point2D[] polyline) {
        if (polyline.length == 0) {
            return;
        }

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;

        strokePolyline(c, polyline, 0);

        // TODO I don't think this is right for wrapping paths
        for (final Point2D p : polyline) {
            minX = Math.min(p.getX(), minX);
            maxX = Math.max(p.getX(), maxX);
        }

        if (mapVariables.toCanvasX(minX) > 0) {
            strokePolyline(c, polyline, -360);
        }

        if (mapVariables.toCanvasX(maxX) < mapVariables.getViewWidth()) {
            strokePolyline(c, polyline, 360);
        }
    }

    private void strokePolyline(final GraphicsContext c, final Point2D[] polyline, final double offsetX) {
        final int n = writePolylineToBuffer(polyline, offsetX);
        strokePolyline(c, n);
    }

    private void drawPolygons(final GraphicsContext c, final Polygon polygon, final boolean polyline, final boolean fill, final boolean simplify) {
        if (mapVariables.toCanvasX(polygon.boundary().getMinX()) < 0) {
            drawPolygon(c, polygon, 360, polyline, fill, simplify);
        }

        if (mapVariables.toCanvasX(polygon.boundary().getMaxX()) > mapVariables.getViewWidth()) {
            drawPolygon(c, polygon, -360, polyline, fill, simplify);
        }

        drawPolygon(c, polygon, 0, polyline, fill, simplify);
    }

    private void drawPolygon(final GraphicsContext c, final Polygon polygon, final double offsetX, final boolean polyline, final boolean fill, final boolean simplify) {
        if (!mapVariables.isRectIntersectingWorldView(shiftedBounds(polygon, offsetX))) {
            return;
        }

        final int numPoints = writePolygonToBuffer(polygon, offsetX);

        final boolean twoDimensional = numPoints >= 3;
        if (!twoDimensional) {
            return;
        }

        if (polyline) {
            if (!fill) {
                strokePolyline(c, numPoints);
            }
        } else {
            if (fill) {
                fillPolygon(c, numPoints);
            } else {
                strokePolygon(c, numPoints);
            }
        }
    }

    private void strokePolygon(final GraphicsContext c, final int numPoints) {
        metric.getStrokePolygon().increment();
        c.strokePolygon(mapVariables.getXBuf(), mapVariables.getYBuf(), numPoints);
    }

    private void fillPolygon(final GraphicsContext c, final int numPoints) {
        metric.getFillPolygon().increment();
        c.fillPolygon(mapVariables.getXBuf(), mapVariables.getYBuf(), numPoints);
    }

    private void strokePolyline(final GraphicsContext c, final int numPoints) {
        metric.getStrokePolyline().increment();
        c.strokePolyline(mapVariables.getXBuf(), mapVariables.getYBuf(), numPoints);
    }

    private int writePolylineToBuffer(final Point2D[] polyline, final double offsetX) {
        double lastX = polyline[0].getX();

        double offset = 0;

        for (int i = 0; i < polyline.length; i++) {
            final Point2D p = polyline[i];
            final double curX = p.getX();
            if (Math.abs(curX - lastX) >= 180) {
                if (curX < 0) {
                    offset += 360;
                } else {
                    offset -= 360;
                }
            }

            final double x = mapVariables.toCanvasX(p.getX() + offsetX + offset);
            final double y = mapVariables.toCanvasY(p.getY());
            mapVariables.setBuf(i, x, y);
            lastX = curX;
        }
        return polyline.length;
    }

    private int writePolygonToBuffer(final Polygon polygon, final double offsetX) {
        int numPoints = writeRingToBuffer(polygon.getExteriorRing(), offsetX, 0);

        final List<Polygon.Ring> holeRings = polygon.getHoleRings();

        for (final Polygon.Ring hole : holeRings) {
            numPoints += writeRingToBuffer(hole, offsetX, numPoints);
        }

        for (int i = 0; i < holeRings.size() - 1; i++) {
            final Polygon.Ring hole = holeRings.get(holeRings.size() - 2 - i);
            mapVariables.setBuf(numPoints, mapVariables.toCanvasX(hole.getPointsX()[0] + offsetX), mapVariables.toCanvasY(hole
                    .getPointsY()[0]));
            numPoints += 1;
        }

        return numPoints;
    }

    private int writeRingToBuffer(final Polygon.Ring ring, final double offsetX, final int indexOffset) {
        double lastDrawnX = Double.MAX_VALUE;
        double lastDrawnY = Double.MAX_VALUE;

        int numPoints = 0;

        for (int i = 0; i < ring.numPoints(); i++) {
            final double x = mapVariables.toCanvasX(ring.getPointsX()[i] + offsetX);
            final double y = mapVariables.toCanvasY(ring.getPointsY()[i]);

            final double squareDistance = GeomUtil.squareDistance(lastDrawnX, lastDrawnY, x, y);

            if (i == 0 || i == ring.numPoints() - 1 || squareDistance > MIN_SQUARE_DISTANCE) {
                mapVariables.setBuf(indexOffset + numPoints, x, y);

                numPoints += 1;

                lastDrawnX = x;
                lastDrawnY = y;
            }
        }

        return numPoints;
    }

    private static Rectangle2D shiftedBounds(final Polygon polygon, final double offsetX) {
        final Rectangle2D boundary = polygon.boundary();
        return new Rectangle2D(
                boundary.getMinX() + offsetX,
                boundary.getMinY(),
                boundary.getWidth(),
                boundary.getHeight()
        );
    }

    public void setPixel(final GraphicsContext c, final Color color, final int x, final int y) {
        final int actualX;

        if (x < 0) {
            actualX = (int) (x + mapVariables.getViewWidth() * mapVariables.getScale());
        } else if (x >= mapVariables.getViewWidth()) {
            actualX = (int) (x - mapVariables.getViewWidth() * mapVariables.getScale());
        } else {
            actualX = x;
        }

        c.getPixelWriter().setColor(
                actualX,
                y,
                color
        );
    }

    public void fillTextWithBackground(
            final GraphicsContext c,
            final double x,
            final double y,
            final String text,
            final boolean background,
            final TextAlignment align,
            final VPos baseline,
            final Color textColor,
            final Color backgroundColor
    ) {
        final int _x = (int) Math.round(x);
        final int _y = (int) Math.round(y);

        if (background) {
            c.setTextBaseline(baseline);
            final FontMetrics fm = Toolkit.getToolkit().getFontLoader().getFontMetrics(c.getFont());

            c.setFill(backgroundColor);
            final int width = (int) Math.round(text.chars().mapToDouble(e -> fm.getCharWidth((char) e)).sum());
            final int height = Math.round(fm.getLineHeight());

            final double baselineOffset = switch (baseline) {
                case TOP -> 0;
                case CENTER -> -height / 2.0 - 1;
                case BOTTOM -> -height;
                default -> throw new IllegalArgumentException("Illegal baseline " + baseline);
            };

            final double horizontalOffset = switch (align) {
                case RIGHT -> -width;
                case CENTER -> -width / 2.0;
                case LEFT -> 0;
                default -> throw new IllegalArgumentException("Illegal alignment " + align);
            };

            final double xRect = _x + horizontalOffset - 1;
            final double yRect = _y + baselineOffset;
            fillRect(c, Math.round(xRect), Math.round(yRect), Math.ceil(width + 1), Math.ceil(height));
        }

        if (align != null) {
            c.setTextAlign(align);
        }
        c.setTextBaseline(baseline);
        c.setFill(textColor);
        fillText(c, text, _x, _y);
    }

    public void fillText(final GraphicsContext c, final String text, final double x, final double y) {
        // TODO temporary fix
        if (!mapVariables.isRectIntersectingCanvasView(x - 150, y - 150, 300, 300)) {
            return;
        }

        metric.getFillText().increment();
        c.fillText(text, x, y);
    }

    public void fillOval(final GraphicsContext c, final double x, final double y, final double w, final double h) {
        metric.getFillOval().increment();
        c.fillOval(x, y, w, h);
    }

    public void strokeOval(final GraphicsContext c, final double x, final double y, final double w, final double h) {
        metric.getStrokeOval().increment();
        c.strokeOval(x, y, w, h);
    }

    public void strokeLine(final GraphicsContext c, final Line2D line) {
        strokeLine(c, line.x1, line.y1, line.x2, line.y2);
    }

    public void strokeLine(final GraphicsContext c, final Point2D p1, final Point2D p2) {
        strokeLine(c, p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void strokeLine(final GraphicsContext c, final double x1, final double y1, final double x2, final double y2) {
        if (!mapVariables.isLineIntersectingCanvasView(x1, y1, x2, y2)) {
            return;
        }

        metric.getStrokeLine().increment();
        c.strokeLine(x1, y1, x2, y2);
    }

    public void strokeRect(final GraphicsContext c, final double x, final double y, final double w, final double h) {
        if (!mapVariables.isRectIntersectingCanvasView(x, y, w, h)) {
            return;
        }

        metric.getStrokeRect().increment();
        c.strokeRect(x, y, w, h);
    }

    public void fillRect(final GraphicsContext c, final double x, final double y, final double w, final double h) {
        if (!mapVariables.isRectIntersectingCanvasView(x, y, w, h)) {
            return;
        }

        metric.getFillRect().increment();
        c.fillRect(x, y, w, h);
    }
}
