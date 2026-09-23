package com.song.ui.config;

import com.song.util.SecretMask;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 * 密钥输入：失焦显示首3尾4掩码；获焦编辑明文；提交时回调。
 */
public final class MaskedSecretField extends TextField {

    private String plain = "";
    private boolean editing;
    private Consumer<String> onCommitted;

    public MaskedSecretField() {
        focusedProperty().addListener((obs, was, now) -> {
            if (now) {
                beginEdit();
            } else {
                endEdit();
            }
        });
        setOnAction(e -> endEdit());
    }

    /**
     * 提交明文后的回调（失焦且值变化时）。
     *
     * @param onCommitted 接收最新明文
     */
    public void setOnCommitted(Consumer<String> onCommitted) {
        this.onCommitted = onCommitted;
    }

    /**
     * 载入明文并显示掩码。
     *
     * @param value 明文
     */
    public void setPlain(String value) {
        this.plain = value == null ? "" : value;
        if (!editing) {
            showMasked();
        } else {
            setText(plain);
        }
    }

    /** @return 当前明文 */
    public String getPlain() {
        return plain;
    }

    private void beginEdit() {
        editing = true;
        setText(plain);
        positionCaret(getText().length());
    }

    private void endEdit() {
        if (!editing) {
            return;
        }
        editing = false;
        String typed = getText() == null ? "" : getText().trim();
        if (!SecretMask.looksMasked(typed)) {
            String previous = plain;
            plain = typed;
            if (onCommitted != null && !previous.equals(plain)) {
                onCommitted.accept(plain);
            }
        }
        showMasked();
    }

    private void showMasked() {
        setText(SecretMask.mask(plain));
    }
}
