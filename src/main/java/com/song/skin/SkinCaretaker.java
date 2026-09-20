package com.song.skin;

/**
 * 皮肤快照管理者（Caretaker 模式）。
 */
public class SkinCaretaker {

    private SkinMemento memento;

    public void save(SkinMemento memento) {
        this.memento = memento;
    }

    public SkinMemento restore() {
        return memento;
    }
}