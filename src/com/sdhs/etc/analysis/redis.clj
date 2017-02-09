(ns com.sdhs.etc.analysis.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as str :refer [join]]))

(def server2-conn {:pool {:max-total 8} :spec {:host "10.180.29.35" :port 6379}}) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server2-conn ~@body))

(defn get-available-stations
  [pcode instation outstation]
  (wcar* (car/get (join ":" [pcode instation outstation]))))
