module com.translator {
    requires javafx.controls;
    requires com.google.gson;
    requires java.net.http;

    exports com.translator.api;
    exports com.translator.facade;
    exports com.translator.bootstrap;
    exports com.translator.config;
    exports com.translator.http;
    exports com.translator.youdao;
    exports com.translator.baidu;
    exports com.translator.demo;

    exports com.llm.api;
    exports com.llm.facade;

    opens com.translator.api to com.google.gson;
    opens com.translator.config to com.google.gson;
    opens com.translator.youdao.internal to com.google.gson;
    opens com.translator.baidu.internal to com.google.gson;

    opens com.translator.demo to javafx.graphics;
}
