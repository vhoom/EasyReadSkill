module com.song.easyreadskill {
    requires javafx.controls;
    requires org.slf4j;
    requires logback.classic;
    requires logback.core;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires com.google.gson;

    // 让 JavaFX 能反射访问
    opens com.song to javafx.fxml;
    // 让 Gson 能反射读写这些类
    opens com.song.model to com.google.gson;
    opens com.song.config to com.google.gson;

    exports com.song;
    exports com.song.model;
    exports com.song.service;
    exports com.song.config;
    exports com.song.service.factory;
    exports com.song.skin;
    exports com.song.skin.animation;
    exports com.song.skin.control;
    exports com.song.skin.windows;
    requires javafx.base;
}