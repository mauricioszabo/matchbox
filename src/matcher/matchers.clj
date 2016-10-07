(ns matcher.matchers
  (:refer-clojure :exclude [list vector instance? map pmap])
  (:require [clojure.core.unify :as u]
            [matcher.utils.match :as utils]))

(defn- check-elements [m rest]
  (let [[before after] (split-with #(not= % '&) rest)
        m-size (count m)
        b-size (count before)
        b-vec (vec before)
        m-vec (vec m)]
    (cond
      (empty? after) (when (= m-size b-size) [b-vec m-vec])
      (= 2 (count after)) (when (>= m-size b-size)
                            [(conj b-vec (last after))
                             (conj (subvec m-vec 0 b-size) (drop b-size m-vec))])
      :else nil)))

(defn list [ & rest]
  (fn [m] (when (list? m) (check-elements m rest))))

(defn vector [ & rest]
  (fn [m] (when (vector? m) (check-elements m rest))))

(defn map [pattern]
  (fn [m]
    (when (and (map? m) (map? pattern)) [pattern m])))

(defn pmap [pattern]
  (fn [m]
    (when (and (map? m) (map? pattern))
      (let [vs (-> pattern vals set)
            partial-map (->> m
                             (filter (fn [[k v]] (or (contains? pattern k)
                                                     (contains? vs v))))
                             (into {}))]
        [pattern partial-map]))))

(defn instance? [class]
  (fn [obj]
    (if (and (clojure.core/instance? java.lang.Class class)
             (clojure.core/instance? class obj))
      [])))

(defmacro match [obj & matches]
  (apply utils/match* obj matches))

(u/unify {'?a '?b} {:a 10, :b 20})
