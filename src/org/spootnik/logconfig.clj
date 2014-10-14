(ns org.spootnik.logconfig
  "Small veneer on top of log4j and commons logging.
   Originally based on the logging initialization in riemann"
  (:import org.apache.log4j.Logger
           org.apache.log4j.BasicConfigurator
           org.apache.log4j.EnhancedPatternLayout
           org.apache.log4j.Level
           org.apache.log4j.ConsoleAppender
           org.apache.log4j.FileAppender
           org.apache.log4j.spi.RootLogger
           org.apache.log4j.rolling.TimeBasedRollingPolicy
           org.apache.log4j.rolling.RollingFileAppender
           org.apache.commons.logging.LogFactory
           net.logstash.log4j.JSONEventLayoutV1))

(def levels
  "Logging levels"
  {"debug" Level/DEBUG
   "info"  Level/INFO
   "warn"  Level/WARN
   "error" Level/ERROR
   "all"   Level/ALL
   "fatal" Level/FATAL
   "trace" Level/TRACE
   "off"   Level/OFF})

(defn start-logging!
  "Initialize log4j logging"
  [{:keys [external console files pattern level overrides json]}]
  (let [j-layout    (JSONEventLayoutV1.)
        p-layout    (EnhancedPatternLayout. pattern)
        layout      (fn [json?] (if json? j-layout p-layout))
        root-logger (Logger/getRootLogger)]

    (when-not external

      (.removeAllAppenders root-logger)

      (when console
        (.addAppender root-logger (ConsoleAppender. (layout json))))

      (doseq [file files
              :let [path (if (string? file) file (:file file))
                    json (if (string? file) false (:json file))]]
        (let [rolling-policy (doto (TimeBasedRollingPolicy.)
                               (.setActiveFileName file)
                               (.setFileNamePattern
                                (str file ".%d{yyyy-MM-dd}.gz"))
                               (.activateOptions))
              log-appender   (doto (RollingFileAppender.)
                               (.setRollingPolicy rolling-policy)
                               (.setLayout (layout json))
                               (.activateOptions))]
          (.addAppender root-logger log-appender)))

      (.setLevel root-logger (get levels level Level/INFO))

      (doseq [[logger level] overrides
              :let [logger (Logger/getLogger (name logger))
                    level  (get levels level Level/DEBUG)]]
        (.setLevel logger level)))))