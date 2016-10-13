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

(defn matches [pattern]
  (fn [obj] [pattern obj]))

; (defn map [ & pattern]
;   (fn [obj]
;     (when (and (map? obj))
;       (let [forms (for [p (partition 2 pattern)
;                         o obj
;                         :let [match (utils/match-and-unify o (matches p))]
;                         :when match]
;                     [p o])]
;         (when (not-empty forms)
;           (reduce (fn [[lss rss] [ls rs]] [(conj lss ls) (conj rss rs)]) [] forms))))))

(let [s (set {:a 10 :b 20})
      m (some #{[:a 10]} s)]
  (disj s m))
(flatten (seq{:a 10 :b 20}))

(defn- match-kv [elem [pattern-k pattern-v]]
  (let [[k v] elem
        k-match (utils/match-and-unify k pattern-k)
        v-match (utils/match-and-unify v pattern-v)]
    (when (and k-match v-match)
      (let [ls-k (-> k-match keys vec)
            rs-k (-> k-match vals vec)
            ls-v (-> v-match keys vec)
            rs-v (-> v-match vals vec)]
        [elem [ls-k ls-v] [rs-k rs-v]]))))

(defn map [ & pattern]
  (fn [obj]
    (when (and (map? obj) (even? (count pattern)))
      (loop [[pattern & forms] (partition 2 pattern)
             map (set obj)
             [ls rs] [[] []]]
        (if pattern
          (let [[m l r] (some #(match-kv % pattern) map)
                submap (disj map m)
                acc [(conj ls l) (conj rs r)]]
            (and m (recur forms submap acc)))
          [ls rs])))))


(defn instance? [class]
  (fn [obj]
    (if (and (clojure.core/instance? java.lang.Class class)
             (clojure.core/instance? class obj))
      [])))

(defmacro match [obj & matches]
  (apply utils/match* obj matches))
