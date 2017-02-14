(ns com.sdhs.etc.analysis.db
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]
            [honeysql.helpers :as helpers :refer [select from where order-by]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource
           java.sql.SQLException))


(def pg-db {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :subname "//10.180.29.35:5432/etc"
            :dbtype "postgresql"
            :dbname "etc"
            :host "10.180.29.35"
            :user "analysis"
            :password "data"})


(defn db_pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(def pooled-db (delay (db_pool pg-db)))

(defn db-conn [] @pooled-db)

(defn query-road-path
  [userid start_intime end_intime cars page pagesize]
  (let [sql "select a.carno as car_number,b.inname as origin_name,a.instation as origin_name_etc,b.in_poi[0] as origin_longitude,b.in_poi[1] as origin_latitude,
             to_char(a.intime,'YYYY-MM-DD HH24:MI:SS') as in_time,b.outname as dest_name,a.outstation as dest_name_etc,b.out_poi[0] as dest_longitude,b.out_poi[1] as dest_latitude,
             to_char(a.outtime,'YYYY-MM-DD HH24:MI:SS') as out_time
             from etc_consumewaste_record a
             left join in_out_station_poi b on a.enprovid||'0000' =b.pcode and a.instation = b.instation and a.outstation = b.outstation
             where a.userid = ? and a.intime >= to_date(?,'YYYY-MM-DD') and a.intime <= to_date(?, 'YYYY-MM-DD') "]
    (log/debug "查询参数：" (string/join " " [userid start_intime end_intime cars page pagesize]))
    (if (not-empty cars)
      (let [s (str sql "and carno in (" (string/join ", " (repeat (count cars) "?")) ") limit ? offset ?")
            param (concat [s userid start_intime end_intime] cars [pagesize (* (dec page) pagesize)])]
        (j/query (db-conn) param))
      (j/query (db-conn) [(str sql "limit ? offset ?") userid start_intime end_intime pagesize (* (dec page) pagesize)]))))

(defn query-station-poi
  [pcode instation outstation]
  (j/query (db-conn) ["select pcode,instation,outstation,inname,inattr,in_poi[0] as in_lng, in_poi[1] as in_lat,
outname, outattr, out_poi[0] as out_lng, out_poi[1] as out_lat,distance,inscore,outscore,speed from in_out_station_poi where pcode = ? and instation = ? and outstation = ?" pcode instation outstation]))

(defn query-gaode-station
  [pcode station attr]
  (let [condition [(when (not-empty station) [:like :station (str "%" station "%")])
                   (when (not-empty attr) [:like :attr (str "%" attr "%")])]
        wheres (keep identity condition)
        sql (-> (select :id :pcode :station :attr (sql/raw "poi[0] as lng") (sql/raw "poi[1] as lat"))
                (from :gaode_station))]
    (when (not-empty wheres)
      (when (not-empty pcode) (conj wheres [:= :pcode pcode]))
      (j/query (db-conn) (sql/format (merge sql (apply where wheres) (order-by (sql/raw "poi[0],poi[1]"))))))))

(defn query-car-path
  [cardno start_intime end_intime]
  (let [sql "select a.carno as car_number,b.inname as origin_name,a.instation as origin_name_etc,b.in_poi[0] as origin_longitude,b.in_poi[1] as origin_latitude,
             to_char(a.intime,'YYYY-MM-DD HH24:MI:SS') as in_time,b.outname as dest_name,a.outstation as dest_name_etc,b.out_poi[0] as dest_longitude,b.out_poi[1] as dest_latitude,
             to_char(a.outtime,'YYYY-MM-DD HH24:MI:SS') as out_time
             from etc_consumewaste_record a
             left join in_out_station_poi b on a.enprovid||'0000' =b.pcode and a.instation = b.instation and a.outstation = b.outstation
             where a.cardno = ? and a.intime >= to_date(?,'YYYY-MM-DD') and a.intime <= to_date(?, 'YYYY-MM-DD')
             order by intime"]
    (j/query (db-conn) [sql cardno start_intime end_intime])))
