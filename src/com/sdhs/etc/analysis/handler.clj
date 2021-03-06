(ns com.sdhs.etc.analysis.handler
  (:require [com.sdhs.etc.analysis.db :refer [query-road-path query-station-poi query-gaode-station query-car-path]]
            [clojure.tools.logging :as log]
            [com.sdhs.etc.analysis.redis :refer [get-available-stations]]))

(defn road-path
  [m]
  (let [{:keys [client_id client_name cars start_time end_time coord_type page count] :or {cars [] coord_type 1 page 1 count 20}} m]
    (log/debug (str "search userid: " client_id))
    (if (and client_id start_time end_time)
      (let [routes (query-road-path client_id start_time end_time (if (coll? cars) cars (vector cars)) (min page 30) (min count 500))]
        (log/debug "search result:" (clojure.core/count routes) "," (first routes))
        {:return_code "000"
         :return_msg "OK"
         :total_number (clojure.core/count routes)
         :routes routes})
      {:return_code "001"
       :return_msg (format "参数不全:%s" (str m))
       :total_number 0
       :routes []})))

(defn in-out-station
  [m]
  (let [{:keys [pcode instation outstation]} m]
    (if (and pcode instation outstation)
      {:status "OK"
       :result (query-station-poi pcode instation outstation)}

      {:status "ERROR"})))

(defn available-stations
  [m]
  (let [{:keys [pcode instation outstation]} m]
    (if (and pcode instation outstation)
      {:status "OK"
       :result (get-available-stations pcode instation outstation)}
      {:status "ERROR"})))

(defn gaode-stations
  [{:keys [pcode station attr]}]
  (if (and (empty? station) (empty? attr))
    {:status "ERROR"}
    {:status "OK" :result (query-gaode-station pcode station attr)}))

(defn car-path
  [{:keys [cardno start end]}]
  (if (and cardno start end)
    {:status "OK" :result (query-car-path cardno start end)}
    {:status "ERROR"}))
