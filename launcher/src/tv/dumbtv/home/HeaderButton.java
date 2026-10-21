package tv.dumbtv.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.TextView;

/** Small monochrome icons: no emoji fonts, image assets or extra libraries. */
final class HeaderButton extends TextView {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();

    HeaderButton(Context context, boolean settings) {
        super(context);
        setContentDescription(Language.text(context, settings ? "Einstellungen" : "Gestaltung"));
        if (settings) {
            shape.setFillType(Path.FillType.EVEN_ODD);
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI / 24;
                float r = i % 6 == 0 || i % 6 == 5 ? 8f : 10.5f;
                float x = 12 + r * (float) Math.cos(a);
                float y = 12 + r * (float) Math.sin(a);
                if (i == 0) shape.moveTo(x, y); else shape.lineTo(x, y);
            }
            shape.close();
            shape.addCircle(12, 12, 3.5f, Path.Direction.CW);
        } else {
            shape.moveTo(9, 13); shape.lineTo(18, 3);
            shape.cubicTo(20, 1, 23, 4, 21, 6);
            shape.lineTo(12, 16); shape.close();
            shape.moveTo(10, 15);
            shape.cubicTo(3, 12, 6, 20, 2, 21);
            shape.cubicTo(10, 24, 15, 19, 10, 15); shape.close();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        ink.setColor(getCurrentTextColor());
        float size = Ui.dp(getContext(), 24);
        canvas.save();
        canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
        canvas.scale(size / 24, size / 24);
        canvas.drawPath(shape, ink);
        canvas.restore();
    }
}
