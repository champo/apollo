package io.muun.apollo.presentation.ui.view;

import io.muun.apollo.R;
import io.muun.apollo.presentation.ui.utils.UiUtils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.BindViews;

import javax.annotation.Nullable;


public class MuunNumericKeyboard extends MuunView {

    public interface OnKeyListener {
        void onNumberKey(int number);

        void onDeleteKey();
    }

    private static final String[] NUMBER_KEY_LETTERS = {
            "\u00A0", // invisible characters to occupy same space as text
            "\u00A0",
            "ABC",
            "DEF",
            "GHI",
            "JKL",
            "MNO",
            "PQRS",
            "TUV",
            "WXYZ"
    };

    public static final int MAX_WIDTH_DP = 464;

    @BindViews({
            R.id.key_0,
            R.id.key_1,
            R.id.key_2,
            R.id.key_3,
            R.id.key_4,
            R.id.key_5,
            R.id.key_6,
            R.id.key_7,
            R.id.key_8,
            R.id.key_9
    })
    TextView[] numberKeys;

    @BindView(R.id.key_del)
    ImageView deleteKey;

    @BindColor(R.color.text_secondary_color)
    int alphabetCharactersColor;

    private OnKeyListener listener;

    public MuunNumericKeyboard(Context context) {
        super(context);
    }

    public MuunNumericKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MuunNumericKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.muun_numeric_keyboard;
    }

    @Override
    protected void setUp(Context context, @Nullable AttributeSet attrs) {
        super.setUp(context, attrs);

        for (int i = 0; i < numberKeys.length; i++) {
            final int number = i;
            final TextView numberKey = numberKeys[number];

            final CharSequence label = TextUtils.concat(
                    new RichText("" + number),
                    "\n",
                    new RichText(NUMBER_KEY_LETTERS[number])
                            .setRelativeSize(0.5f)
                            .setForegroundColor(alphabetCharactersColor)
            );

            numberKey.setText(label);
            numberKey.setOnClickListener(ignored -> onNumberKeyClicked(number));
        }

        deleteKey.setOnClickListener(ignored -> onDeleteKeyClicked());
    }

    public void setListener(OnKeyListener listener) {
        this.listener = listener;
    }

    /**
     * Enable or disable the keyboard.
     */
    public void setEnabled(boolean isEnabled) {
        for (View key : numberKeys) {
            key.setEnabled(isEnabled);
        }

        setDeleteEnabled(isEnabled);
    }

    /**
     * Set whether the delete key can be pressed.
     */
    public void setDeleteEnabled(boolean enabled) {
        UiUtils.setTint(deleteKey, enabled ? R.color.icon_color : R.color.disabled_color);
        deleteKey.setEnabled(enabled);
    }

    private void onNumberKeyClicked(int number) {
        if (listener != null && numberKeys[number].isEnabled()) {
            listener.onNumberKey(number);
        }
    }

    private void onDeleteKeyClicked() {
        if (listener != null && deleteKey.isEnabled()) {
            listener.onDeleteKey();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // Do not allow MuunNumericKeyboard to take more width than MAX_WIDTH_DP.
        // This is mostly intended for landscape and tablets.

        final Resources resources = getResources();

        final int maxWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                MAX_WIDTH_DP,
                resources.getDisplayMetrics()
        );

        final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int modeWidth = MeasureSpec.getMode(widthMeasureSpec);

        final int minMeasuredWidth = MeasureSpec.makeMeasureSpec(
                Math.min(specWidth, maxWidthPx),
                modeWidth
        );

        super.onMeasure(minMeasuredWidth, heightMeasureSpec);
    }
}
