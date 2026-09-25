package com.song.skin;

/**
 * 将设计令牌构建为 JavaFX CSS。
 * 深色必须覆盖 Modena 的 looked-up color，否则标签/滚动条仍走默认浅色。
 */
public class CssBuilder {

        private final SkinTokens t;

        public CssBuilder(SkinTokens tokens) {
                this.t = tokens;
        }

        public String build() {
                StringBuilder css = new StringBuilder(8192);
                css.append("/* EasyReadSkill auto-generated skin */\n");

                // 覆盖 Modena 派生色：标签实际吃的是 -fx-text-background-color
                css.append(".root {\n")
                                .append("    -fx-font-family: ").append(quoteFontStack(t.fontFamily())).append(";\n")
                                .append("    -fx-font-size: ").append(t.fontSize()).append("px;\n")
                                .append("    -fx-background: ").append(t.background()).append(";\n")
                                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                                .append("    -fx-base: ").append(t.surface()).append(";\n")
                                .append("    -fx-control-inner-background: ").append(t.surface()).append(";\n")
                                .append("    -fx-control-inner-background-alt: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-text-base-color: ").append(t.text()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.text()).append(";\n")
                                .append("    -fx-text-inner-color: ").append(t.text()).append(";\n")
                                .append("    -fx-prompt-text-fill: ").append(t.textSecondary()).append(";\n")
                                .append("    -fx-accent: ").append(t.primary()).append(";\n")
                                .append("    -fx-default-button: ").append(t.primary()).append(";\n")
                                .append("    -fx-focus-color: ").append(t.primary()).append(";\n")
                                .append("    -fx-faint-focus-color: ").append(t.focusRing()).append(";\n")
                                .append("    -fx-hover-base: ").append(t.hover()).append(";\n")
                                .append("    -fx-pressed-base: ").append(t.selection()).append(";\n")
                                .append("    -fx-box-border: ").append(t.border()).append(";\n")
                                .append("    -fx-text-box-border: ").append(t.border()).append(";\n")
                                .append("    -fx-mark-color: ").append(t.mark()).append(";\n")
                                .append("    -fx-mark-highlight-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("}\n");

                css.append(".top-bar {\n")
                                .append("    -fx-background-color: ").append(t.background()).append(";\n")
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
                                .append("    -fx-padding: 0 0.5 0 0.5;\n")
                                .append("}\n");

                // 标签 / 单选 / 复选：同时写 text-fill 与 text-background-color
                css.append(".label, .radio-button, .check-box, .titled-pane > .title,\n")
                                .append(".titled-pane > .title > .text, .hyperlink, .menu-button > .label {\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".label.section-title {\n")
                                .append("    -fx-font-family: \"Georgia\", \"Times New Roman\", serif;\n")
                                .append("    -fx-font-size: 18px;\n")
                                .append("    -fx-font-weight: 400;\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".label.item-name {\n")
                                .append("    -fx-font-size: 13px;\n")
                                .append("    -fx-font-weight: 500;\n")
                                .append("}\n");
                css.append(".label.eyebrow {\n")
                                .append("    -fx-font-size: 12px;\n")
                                .append("    -fx-font-weight: 500;\n")
                                .append("    -fx-text-fill: ").append(t.textSecondary()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.textSecondary()).append(";\n")
                                .append("}\n");
                css.append(".label.secondary {\n")
                                .append("    -fx-font-size: 12px;\n")
                                .append("    -fx-text-fill: ").append(t.textSecondary()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.textSecondary()).append(";\n")
                                .append("}\n");
                css.append(".action-bar {\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("    -fx-border-width: 1 0 0 0;\n")
                                .append("}\n");
                css.append(".radio-button > .text, .check-box > .box > .mark, .check-box > .text {\n")
                                .append("    -fx-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".hyperlink {\n")
                                .append("    -fx-text-fill: ").append(t.primary()).append(";\n")
                                .append("}\n");
                css.append(".hyperlink:hover {\n")
                                .append("    -fx-text-fill: ").append(t.primaryHover()).append(";\n")
                                .append("}\n");

                css.append(".button {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.text()).append(";\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("    -fx-border-width: 1;\n")
                                .append("    -fx-border-radius: ").append(t.radius()).append(";\n")
                                .append("    -fx-background-radius: ").append(t.radius()).append(";\n")
                                .append("    -fx-padding: 8 16 8 16;\n")
                                .append("    -fx-min-height: 36;\n")
                                .append("    -fx-font-size: 14px;\n")
                                .append("    -fx-font-weight: 500;\n")
                                .append("    -fx-cursor: hand;\n")
                                .append("}\n");
                css.append(".button:hover {\n")
                                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                                .append("}\n");
                css.append(".button:armed {\n")
                                .append("    -fx-background-color: ").append(t.pressed()).append(";\n")
                                .append("}\n");
                css.append(".button:focused {\n")
                                .append("    -fx-border-color: ").append(t.focusStroke()).append(";\n")
                                .append("    -fx-border-width: 2;\n")
                                .append("}\n");
                css.append(".button:default, .button.primary {\n")
                                .append("    -fx-background-color: ").append(t.primary()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.primaryText()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.primaryText()).append(";\n")
                                .append("    -fx-border-color: ").append(t.primary()).append(";\n")
                                .append("}\n");
                css.append(".button:default:hover, .button.primary:hover {\n")
                                .append("    -fx-background-color: ").append(t.primaryHover()).append(";\n")
                                .append("    -fx-border-color: ").append(t.primaryHover()).append(";\n")
                                .append("}\n");
                css.append(".button:default:armed, .button.primary:armed {\n")
                                .append("    -fx-background-color: ").append(t.primaryPressed()).append(";\n")
                                .append("    -fx-border-color: ").append(t.primaryPressed()).append(";\n")
                                .append("}\n");
                css.append(".button:disabled {\n")
                                .append("    -fx-opacity: 0.5;\n")
                                .append("}\n");

                String inputCommon = "    -fx-background-color: " + t.background() + ";\n"
                                + "    -fx-control-inner-background: " + t.background() + ";\n"
                                + "    -fx-text-fill: " + t.text() + ";\n"
                                + "    -fx-text-inner-color: " + t.text() + ";\n"
                                + "    -fx-prompt-text-fill: " + t.textSecondary() + ";\n"
                                + "    -fx-border-color: " + t.border() + ";\n"
                                + "    -fx-border-width: 1;\n"
                                + "    -fx-border-radius: " + t.radius() + ";\n"
                                + "    -fx-background-radius: " + t.radius() + ";\n"
                                + "    -fx-min-height: 36;\n"
                                + "    -fx-padding: 8 12 8 12;\n"
                                + "    -fx-highlight-fill: " + t.selection() + ";\n"
                                + "    -fx-highlight-text-fill: " + t.text() + ";\n";
                css.append(".text-field, .text-area, .password-field, .combo-box, .spinner,\n")
                                .append(".combo-box > .text-input, .spinner > .text-field {\n")
                                .append(inputCommon)
                                .append("}\n");
                css.append(".text-field:focused, .password-field:focused, .combo-box:focused,\n")
                                .append(".spinner:focused {\n")
                                .append("    -fx-border-color: ").append(t.focusStroke()).append(";\n")
                                .append("    -fx-border-width: 1;\n")
                                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                                .append("    -fx-effect: dropshadow(gaussian, ").append(t.focusRing())
                                .append(", 6, 0.35, 0, 0);\n")
                                .append("}\n");
                css.append(".text-area {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-control-inner-background: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-border-color: transparent;\n")
                                .append("    -fx-background-radius: 12;\n")
                                .append("    -fx-border-radius: 12;\n")
                                .append("}\n");
                css.append(".text-area:focused {\n")
                                .append("    -fx-border-color: ").append(t.focusStroke()).append(";\n")
                                .append("    -fx-border-width: 1;\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-effect: dropshadow(gaussian, ").append(t.focusRing())
                                .append(", 6, 0.35, 0, 0);\n")
                                .append("}\n");
                css.append(".text-area .content {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-background-radius: 12;\n")
                                .append("    -fx-padding: 16 16 16 16;\n")
                                .append("}\n");
                css.append(".combo-box .list-cell {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".combo-box > .arrow-button {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("}\n");
                css.append(".combo-box > .arrow-button > .arrow {\n")
                                .append("    -fx-background-color: ").append(t.textSecondary()).append(";\n")
                                .append("}\n");
                css.append(".combo-box-popup .list-view {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-control-inner-background: ").append(t.surface()).append(";\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("}\n");
                css.append(".combo-box-popup .list-cell:filled:hover {\n")
                                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                                .append("}\n");
                css.append(".combo-box-popup .list-cell:filled:selected {\n")
                                .append("    -fx-background-color: ").append(t.selection()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".spinner .increment-arrow-button, .spinner .decrement-arrow-button {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("}\n");
                css.append(".spinner .increment-arrow-button .increment-arrow,\n")
                                .append(".spinner .decrement-arrow-button .decrement-arrow {\n")
                                .append("    -fx-background-color: ").append(t.textSecondary()).append(";\n")
                                .append("}\n");
                css.append(".spinner .increment-arrow-button:hover, .spinner .decrement-arrow-button:hover {\n")
                                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                                .append("}\n");

                css.append(".list-view, .table-view, .tree-view {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-control-inner-background: ").append(t.surface()).append(";\n")
                                .append("    -fx-border-color: transparent;\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("    -fx-padding: 4 0 8 0;\n")
                                .append("}\n");
                css.append(".list-cell {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-padding: 2 8 2 8;\n")
                                .append("    -fx-background-insets: 1 8 1 8;\n")
                                .append("    -fx-background-radius: 8;\n")
                                .append("}\n");
                css.append(".list-cell:filled:selected, .list-cell:filled:selected:hover {\n")
                                .append("    -fx-background-color: ").append(t.selection()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-background-insets: 1 8 1 8;\n")
                                .append("    -fx-background-radius: 8;\n")
                                .append("}\n");
                css.append(".list-cell:filled:hover {\n")
                                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                                .append("    -fx-background-insets: 1 8 1 8;\n")
                                .append("    -fx-background-radius: 8;\n")
                                .append("}\n");
                css.append(".selection-divider {\n")
                                .append("    -fx-background-color: ").append(t.border()).append(";\n")
                                .append("    -fx-min-height: 1;\n")
                                .append("    -fx-pref-height: 1;\n")
                                .append("    -fx-max-height: 1;\n")
                                .append("    -fx-padding: 0;\n")
                                .append("}\n");
                css.append(".list-cell:empty {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("}\n");

                css.append(".radio-button > .radio, .check-box > .box {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-border-color: ").append(t.strokeStrong()).append(";\n")
                                .append("    -fx-min-width: 16; -fx-min-height: 16;\n")
                                .append("    -fx-pref-width: 16; -fx-pref-height: 16;\n")
                                .append("}\n");
                css.append(".check-box > .box {\n")
                                .append("    -fx-background-radius: 4;\n")
                                .append("    -fx-border-radius: 4;\n")
                                .append("}\n");
                css.append(".radio-button:selected > .radio {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-border-color: ").append(t.primary()).append(";\n")
                                .append("}\n");
                css.append(".check-box:selected > .box {\n")
                                .append("    -fx-background-color: ").append(t.primary()).append(";\n")
                                .append("    -fx-border-color: ").append(t.primary()).append(";\n")
                                .append("}\n");
                css.append(".radio-button > .radio > .dot {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("}\n");
                css.append(".radio-button:selected > .radio > .dot {\n")
                                .append("    -fx-background-color: ").append(t.mark()).append(";\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("}\n");
                css.append(".check-box > .box > .mark {\n")
                                .append("    -fx-background-color: ").append(t.primaryText()).append(";\n")
                                .append("}\n");
                css.append(".radio-button:hover > .radio, .check-box:hover > .box {\n")
                                .append("    -fx-border-color: ").append(t.primary()).append(";\n")
                                .append("}\n");

                css.append(".radio-button.filter-tab {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-background-radius: ").append(t.radius()).append(";\n")
                                .append("    -fx-border-color: transparent;\n")
                                .append("    -fx-border-radius: ").append(t.radius()).append(";\n")
                                .append("    -fx-border-width: 0;\n")
                                .append("    -fx-padding: 8 14 8 14;\n")
                                .append("    -fx-font-size: 14px;\n")
                                .append("    -fx-font-weight: 500;\n")
                                .append("    -fx-cursor: hand;\n")
                                .append("}\n");
                css.append(".radio-button.filter-tab > .radio {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-border-color: transparent;\n")
                                .append("    -fx-padding: 0;\n")
                                .append("    -fx-min-width: 0; -fx-min-height: 0;\n")
                                .append("    -fx-pref-width: 0; -fx-pref-height: 0;\n")
                                .append("    -fx-max-width: 0; -fx-max-height: 0;\n")
                                .append("}\n");
                css.append(".radio-button.filter-tab:hover {\n")
                                .append("    -fx-background-color: ").append(t.hover()).append(";\n")
                                .append("}\n");
                css.append(".radio-button.filter-tab:selected {\n")
                                .append("    -fx-background-color: ").append(t.selection()).append(";\n")
                                .append("    -fx-border-color: transparent;\n")
                                .append("}\n");
                css.append(".radio-button.filter-tab:selected:hover {\n")
                                .append("    -fx-background-color: ").append(t.selection()).append(";\n")
                                .append("}\n");

                // 滚动条：覆盖 ListView / TextArea / ScrollPane 内嵌结构
                appendScrollBar(css);

                css.append(".separator .line {\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("    -fx-border-width: 1 0 0 0;\n")
                                .append("}\n");
                css.append(".progress-bar {\n")
                                .append("    -fx-pref-height: 4;\n")
                                .append("    -fx-min-height: 4;\n")
                                .append("    -fx-max-height: 4;\n")
                                .append("}\n");
                css.append(".progress-bar > .track {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-background-radius: 999;\n")
                                .append("}\n");
                css.append(".progress-bar > .bar {\n")
                                .append("    -fx-background-color: ").append(t.primary()).append(";\n")
                                .append("    -fx-background-radius: 999;\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("    -fx-padding: 1;\n")
                                .append("}\n");

                css.append(".tag-warn {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.warning()).append(";\n")
                                .append("    -fx-border-color: ").append(t.warning()).append(";\n")
                                .append("    -fx-border-width: 1;\n")
                                .append("    -fx-border-radius: 999;\n")
                                .append("    -fx-background-radius: 999;\n")
                                .append("    -fx-padding: 0 6 0 6;\n")
                                .append("    -fx-font-size: 10px;\n")
                                .append("}\n");
                css.append(".tooltip {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("    -fx-background-radius: ").append(t.radius()).append(";\n")
                                .append("    -fx-border-radius: ").append(t.radius()).append(";\n")
                                .append("}\n");
                css.append(".dialog-pane {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".dialog-pane > .header-panel {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("}\n");
                css.append(".dialog-pane > .button-bar > .container {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("}\n");
                css.append(".dialog-pane .label, .dialog-pane .content.label {\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-text-background-color: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".dialog-pane > .button-bar .button {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("}\n");
                css.append(".dialog-pane > .button-bar .button:default {\n")
                                .append("    -fx-background-color: ").append(t.primary()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.primaryText()).append(";\n")
                                .append("    -fx-border-color: ").append(t.primary()).append(";\n")
                                .append("}\n");
                css.append(".titled-pane > .title {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("    -fx-text-fill: ").append(t.text()).append(";\n")
                                .append("}\n");
                css.append(".titled-pane > *.content {\n")
                                .append("    -fx-background-color: ").append(t.surface()).append(";\n")
                                .append("    -fx-border-color: ").append(t.border()).append(";\n")
                                .append("}\n");

                return css.toString();
        }

        private void appendScrollBar(StringBuilder css) {
                String thumb = t.scrollThumb();

                css.append(".scroll-pane, .scroll-pane > .viewport, .scroll-pane > .corner {\n")
                                .append("    -fx-background-color: ").append(t.background()).append(";\n")
                                .append("}\n");
                css.append(".text-area .scroll-pane, .text-area .scroll-pane > .viewport,\n")
                                .append(".text-area .scroll-pane > .corner {\n")
                                .append("    -fx-background-color: ").append(t.surfaceAlt()).append(";\n")
                                .append("}\n");

                css.append(".scroll-bar:vertical {\n")
                                .append("    -fx-pref-width: 8;\n")
                                .append("}\n");
                css.append(".scroll-bar:horizontal {\n")
                                .append("    -fx-pref-height: 8;\n")
                                .append("}\n");
                css.append(".scroll-bar {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("    -fx-padding: 0;\n")
                                .append("}\n");
                css.append(".scroll-bar > .track,\n")
                                .append(".scroll-bar > .increment-button,\n")
                                .append(".scroll-bar > .decrement-button {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-background-insets: 0;\n")
                                .append("    -fx-padding: 0;\n")
                                .append("}\n");
                css.append(".scroll-bar > .increment-button > .increment-arrow,\n")
                                .append(".scroll-bar > .decrement-button > .decrement-arrow {\n")
                                .append("    -fx-background-color: transparent;\n")
                                .append("    -fx-padding: 0;\n")
                                .append("    -fx-shape: \"\";\n")
                                .append("}\n");
                css.append(".scroll-bar > .thumb {\n")
                                .append("    -fx-background-color: ").append(thumb).append(";\n")
                                .append("    -fx-background-insets: 1;\n")
                                .append("    -fx-background-radius: 999;\n")
                                .append("}\n");
        }


        private static String quoteFontStack(String stack) {
                String[] parts = stack.split(",");
                StringBuilder quoted = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                        String name = parts[i].trim();
                        if (i > 0)
                                quoted.append(", ");
                        if (name.equals("sans-serif") || name.equals("serif") || name.equals("monospace")) {
                                quoted.append(name);
                        } else {
                                quoted.append('"').append(name).append('"');
                        }
                }
                return quoted.toString();
        }
}
