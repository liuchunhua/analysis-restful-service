(ns com.sdhs.etc.analysis.core
  (:require [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.java.jdbc :as j]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.logging :as log])
  (:use
   com.sdhs.etc.analysis.handler
   ring.middleware.params
   compojure.core
   ring.adapter.jetty)
  (:gen-class))

(defroutes rest-route
  (GET "/" request "It's working!")
  (POST "/query/roadpath" {p :params} (response (road-path p)))
  (POST "/query/station" {p :params} (response (in-out-station p)))
  (POST "/available-stations" {p :params} (response (available-stations p)))
  (POST "/gaode-stations" {p :params} (response (gaode-stations p)))
  (POST "/car-path" {p :params} (response (car-path p)))
  (route/files "/" {:root "target"})
  (route/resources "/" {:root "target"})
  (route/not-found "Not Found"))


(def app
  (-> rest-route
      (wrap-defaults (-> site-defaults
                         (assoc-in [:security :anti-forgery] false)
                         (assoc-in [:responses :content-types] false)))
      (wrap-json-params)
      (wrap-json-response)
      (wrap-gzip)))

(def reloadable-app
  (wrap-reload #'app))

(defn -main
  [& args]
  (let [mode (or (System/getenv "DEV") "PRODUCT")
        port (or (System/getenv "PORT") 9000)]
    (log/info "running mode : " mode "port:" port)
    (run-jetty (if (= mode "DEV") reloadable-app app) {:port (Integer/valueOf port)})))
