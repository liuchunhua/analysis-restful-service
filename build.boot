(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-devel "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [compojure "1.5.2"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [honeysql "0.8.2"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [com.taoensso/carmine "2.15.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.6.2"]
                 [org.slf4j/slf4j-log4j12 "1.6.2"]
                 [log4j "1.2.16"]
                 [commons-logging "1.1.1"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [amalloy/ring-gzip-middleware "0.1.3"]])

(task-options!
 pom {:project 'com.sdhs.etc.analysis/analysis-restful-service
      :version "0.0.1"
      :description "restful service"
      }
 uber {:exclude-scope #{"provided"}}
 aot {:namespace #{'com.sdhs.etc.analysis.core 'com.sdhs.etc.analysis.db 'com.sdhs.etc.analysis.handler}}
 jar {:main 'com.sdhs.etc.analysis.core}
 repl {:init-ns 'com.sdhs.etc.analysis.core})

(require '[com.sdhs.etc.analysis.core :as analysis])
(deftask run []
  (with-pass-thru _ (analysis/-main)))

(deftask build
  []
  (comp
   (pom)
   (jar)
   (install)))

(deftask package
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (aot)
   (uber)
   (jar)
   (sift)
   (target)))
