package com.song.skin;

/**
 * 切换皮肤命令。
 */
public class SwitchSkinCommand implements SkinCommand {

    private final SkinManager manager;
    private final SkinType type;

    public SwitchSkinCommand(SkinManager manager, SkinType type) {
        this.manager = manager;
        this.type = type;
    }

    @Override
    public void execute() {
        manager.switchTo(type);
    }
}