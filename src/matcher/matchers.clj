(ns matcher.matchers
  (:refer-clojure :exclude [list vector instance? map pmap or and])
  (:require [clojure.core :as core]
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

(defn matches [pattern]
  (fn [obj] [pattern obj]))

(defn- match-kv [elem [pattern-k pattern-v]]
  (let [[k v] elem
        k-match (utils/match-and-unify k pattern-k)
        v-match (utils/match-and-unify v pattern-v)]
    (when (core/and k-match v-match)
      (let [ls-k (-> k-match keys vec)
            rs-k (-> k-match vals vec)
            ls-v (-> v-match keys vec)
            rs-v (-> v-match vals vec)]
        [elem [ls-k ls-v] [rs-k rs-v]]))))

(defn map [ & pattern]
  (fn [obj]
    (when (core/and (map? obj) (even? (count pattern)))
      (loop [[pattern & forms] (partition 2 pattern)
             map (set obj)
             [ls rs] [[] []]]
        (if pattern
          (let [[m l r] (some #(match-kv % pattern) map)
                submap (disj map m)
                acc [(conj ls l) (conj rs r)]]
            (core/and m (recur forms submap acc)))
          [ls rs])))))

(defn instance? [class]
  (fn [obj]
    (if (core/and (core/instance? java.lang.Class class)
                  (core/instance? class obj))
      [])))

(defn or [ & matchers]
  (fn [obj]
    (some #(if-let [r (utils/match-and-unify obj %)]
             [(keys r) (vals r)]) matchers)))

(defn and [ & matchers]
  (fn [obj]
    (let [applied (core/map #(utils/match-and-unify obj %) matchers)]
      (when (every? map? applied)
        (let [ks (core/map keys applied)
              vs (core/map vals applied)]
          [ks vs])))))

(defmacro record [record & args]
  (let [arglist (when (symbol? record)
                  (some-> record name (clojure.string/replace #"^" "->")
                          symbol resolve meta :arglists first
                          (->> (core/map keyword) vec)))
        arguments (vec args)]
    `(fn [~'obj]
       (when (core/and ~arglist
                  (core/instance? ~record ~'obj)
                  (core/or (= (count ~arguments) 0)
                           (= (count ~arguments) (count ~arglist))))
         [~arguments (core/map #(get ~'obj %) ~arglist)]))))

(defmacro match [obj & matches]
  (apply utils/match* obj matches))
