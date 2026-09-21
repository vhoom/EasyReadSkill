module com.song.easyreadskill {
    requires javafx.controls;
    requires org.slf4j;
    requires logback.classic;
    requires logback.core;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires com.google.gson;


    opens com.song.model to com.google.gson;
    opens com.song.config to com.google.gson;

    exports com.song;
    exports com.song.model;
    exports com.song.service;
    exports com.song.config;
    exports com.song.service.factory;
    exports com.song.skin;
    exports com.song.skin.windows;
    requires javafx.base;
}