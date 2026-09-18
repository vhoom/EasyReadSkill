package com.song.skin;

/**
 * 将设计令牌构建为 JavaFX CSS。
 */
public class CssBuilder {

    private final SkinTokens t;

    public CssBuilder(SkinTokens tokens) {
        this.t = tokens;
    }

    public String build() {
        StringBuilder css = new StringBuilder(4096);
        css.append("/* EasyReadSkill auto-generated skin */\n");
        css.append(".root {\n")
                .append("    -fx-font-family: \"").append(t.fontFamily()).append("\";\n")
                .append("    -fx-font-size: ").append(t.fontSize()).append("px;\n")
                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                .append("    -fx-base: ").append(t.surface()).append(";\n")
                .append("    -fx-accent: ").append(t.primary()).append(";\n")
                .append("    -fx-focus-color: ").append(t.primary()).append(";\n")
                .append("    -fx-faint-focus-color: ").append(t.primary()).append("22;\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("}\n");

        css.append(".top-bar {\n")
                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("    -fx-border-width: 0 0 1 0;\n")
                .append("}\n");
        css.append(".right-panel {\n")
                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                .append("}\n");
        css.append(".left-panel {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("    -fx-border-width: 0 1 0 0;\n")
                .append("}\n");

        css.append(".split-pane {\n")
                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                .append("    -fx-padding: 0;\n")
                .append("}\n");
        css.append(".split-pane > .split-pane-divider {\n")
                .append("    -fx-background-color: ").append(t.border()).append(";\n")
                .append("    -fx-padding: 0 1 0 1;\n")
                .append("}\n");

        css.append(".label, .radio-button, .check-box, .titled-pane > .title > .text {\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("}\n");
        css.append(".label.secondary {\n")
                .append("    -fx-text-fill: ").append(t.textSecondary()).append(";\n")
                .append("}\n");

        css.append(".button {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("    -fx-border-radius: ").append(t.radius()).append(";\n")
                .append("    -fx-background-radius: ").append(t.radius()).append(";\n")
                .append("    -fx-padding: 6 12 6 12;\n")
                .append("}\n");
        css.append(".button:hover {\n")
                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                .append("}\n");
        css.append(".button:default, .button.primary {\n")
                .append("    -fx-background-color: ").append(t.primary()).append(";\n")
                .append("    -fx-text-fill: ").append(t.primaryText()).append(";\n")
                .append("    -fx-border-color: ").append(t.primary()).append(";\n")
                .append("}\n");
        css.append(".button:disabled {\n")
                .append("    -fx-opacity: 0.55;\n")
                .append("}\n");

        css.append(".text-field, .text-area, .combo-box, .spinner {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("    -fx-prompt-text-fill: ").append(t.textSecondary()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("    -fx-border-radius: ").append(t.radius()).append(";\n")
                .append("    -fx-background-radius: ").append(t.radius()).append(";\n")
                .append("}\n");
        css.append(".text-area .content {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("}\n");
        css.append(".combo-box .list-cell {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("}\n");
        css.append(".combo-box-popup .list-view {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("}\n");
        css.append(".combo-box-popup .list-cell:filled:hover {\n")
                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                .append("}\n");

        css.append(".list-view, .table-view, .tree-view {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-control-inner-background: ").append(t.surface()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("}\n");
        css.append(".list-cell {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("}\n");
        css.append(".list-cell:filled:selected, .list-cell:filled:selected:hover {\n")
                .append("    -fx-background-color: ").append(t.selection()).append(";\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("}\n");
        css.append(".list-cell:filled:hover {\n")
                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                .append("}\n");

        css.append(".scroll-pane, .scroll-pane > .viewport {\n")
                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                .append("}\n");
        css.append(".scroll-bar:vertical, .scroll-bar:horizontal {\n")
                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                .append("}\n");
        css.append(".scroll-bar .thumb {\n")
                .append("    -fx-background-color: ").append(t.border()).append(";\n")
                .append("    -fx-background-radius: 4;\n")
                .append("}\n");

        css.append(".separator .line {\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("}\n");
        css.append(".progress-bar .track {\n")
                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                .append("}\n");
        css.append(".progress-bar .bar {\n")
                .append("    -fx-background-color: ").append(t.primary()).append(";\n")
                .append("}\n");
        css.append(".tooltip {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                .append("}\n");
        css.append(".dialog-pane {\n")
                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                .append("}\n");
        css.append(".dialog-pane .header-panel {\n")
                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                .append("}\n");
        css.append(".dialog-pane .content.label {\n")
                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                .append("}\n");

        return css.toString();
    }
}